package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.SpeedTestRecord
import com.example.data.repository.SpeedTestRepository
import com.example.engine.SpeedTestPhase
import com.example.engine.SpeedTestProgress
import com.example.engine.TailscaleSpeedEngine
import com.example.model.TailnetNode
import com.example.model.TailscaleEngineState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SpeedTestViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SpeedTestRepository
    val historyRecords: StateFlow<List<SpeedTestRecord>>

    private val engine = TailscaleSpeedEngine()

    private val _engineState = MutableStateFlow(TailscaleEngineState())
    val engineState: StateFlow<TailscaleEngineState> = _engineState.asStateFlow()

    private val _peers = MutableStateFlow<List<TailnetNode>>(TailscaleSpeedEngine.DEFAULT_PEERS)
    val peers: StateFlow<List<TailnetNode>> = _peers.asStateFlow()

    private val _selectedNode = MutableStateFlow(_peers.value.first())
    val selectedNode: StateFlow<TailnetNode> = _selectedNode.asStateFlow()

    private val _progress = MutableStateFlow(SpeedTestProgress(targetNode = _selectedNode.value))
    val progress: StateFlow<SpeedTestProgress> = _progress.asStateFlow()

    private var testJob: Job? = null

    init {
        val db = AppDatabase.getDatabase(application)
        repository = SpeedTestRepository(db.speedTestDao())
        historyRecords = repository.allRecords.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Ping initial peer list
        refreshPeerLatencies()
    }

    fun selectTargetNode(node: TailnetNode) {
        _selectedNode.value = node
        if (_progress.value.phase == SpeedTestPhase.IDLE || _progress.value.phase == SpeedTestPhase.COMPLETED) {
            _progress.value = SpeedTestProgress(targetNode = node)
        }
    }

    fun startSpeedTest() {
        if (testJob?.isActive == true) return

        val target = _selectedNode.value
        testJob = viewModelScope.launch {
            engine.executeSpeedTest(target).collect { currentProgress ->
                _progress.value = currentProgress

                if (currentProgress.phase == SpeedTestPhase.COMPLETED) {
                    saveRecordToHistory(currentProgress)
                }
            }
        }
    }

    fun cancelSpeedTest() {
        testJob?.cancel()
        testJob = null
        _progress.value = SpeedTestProgress(
            phase = SpeedTestPhase.CANCELLED,
            targetNode = _selectedNode.value
        )
    }

    private suspend fun saveRecordToHistory(progress: SpeedTestProgress) {
        val node = progress.targetNode ?: _selectedNode.value
        val rating = engine.getNetworkRating(
            pingMs = progress.pingMs,
            downloadMbps = progress.downloadMbps,
            isDirect = node.isDirect
        )

        val record = SpeedTestRecord(
            nodeName = node.name,
            ipAddress = node.ip,
            pingMs = progress.pingMs,
            jitterMs = progress.jitterMs,
            downloadMbps = progress.downloadMbps,
            uploadMbps = progress.uploadMbps,
            connectionType = if (node.isDerpRelay) "DERP Relay (${node.location})" else "Tailnet Direct (WireGuard)",
            networkRating = rating,
            bytesTransferredMb = (progress.downloadMbps + progress.uploadMbps) * 5.0 / 8.0
        )

        repository.insertRecord(record)
    }

    fun refreshPeerLatencies() {
        viewModelScope.launch {
            val updated = _peers.value.map { node ->
                val ping = engine.pingSingleNode(node)
                node.copy(latencyMs = ping)
            }
            _peers.value = updated
        }
    }

    fun addCustomPeer(name: String, ip: String, location: String) {
        val newNode = TailnetNode(
            id = "custom-${System.currentTimeMillis()}",
            name = name,
            ip = ip,
            location = location,
            isDerpRelay = false,
            latencyMs = 15,
            isDirect = true,
            tags = listOf("Custom Peer")
        )
        _peers.value = _peers.value + newNode
        selectTargetNode(newNode)
    }

    fun updateAuthKey(newKey: String, tailnetName: String) {
        _engineState.value = _engineState.value.copy(
            authKey = newKey,
            tailnetName = tailnetName,
            isConnected = true
        )
    }

    fun toggleEmbeddedServer() {
        _engineState.value = _engineState.value.copy(
            isServerRunning = !_engineState.value.isServerRunning
        )
    }

    fun deleteRecord(id: Int) {
        viewModelScope.launch {
            repository.deleteRecord(id)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }
}
