package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.AppThemeMode
import com.example.model.BarraTab
import com.example.model.FileNode
import com.example.model.LogEntry
import com.example.model.LogLevel
import com.example.model.MediaItem
import com.example.model.SftpConfig
import com.example.model.TailscaleStatus
import com.example.service.SftpService
import com.example.service.TailscaleEngine
import com.example.util.CacheManager
import com.example.util.ThumbHashUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BarraCloudViewModel(application: Application) : AndroidViewModel(application) {

    val cacheManager = CacheManager(application)
    val sftpService = SftpService()
    val tailscaleEngine = TailscaleEngine()

    val tailscaleStatus: StateFlow<TailscaleStatus> = tailscaleEngine.status

    // Configuration State
    private val _sftpConfig = MutableStateFlow(SftpConfig())
    val sftpConfig: StateFlow<SftpConfig> = _sftpConfig.asStateFlow()

    // Navigation & Layout State
    private val _activeTab = MutableStateFlow(BarraTab.HOME)
    val activeTab: StateFlow<BarraTab> = _activeTab.asStateFlow()

    private val _gridColumnCount = MutableStateFlow(3)
    val gridColumnCount: StateFlow<Int> = _gridColumnCount.asStateFlow()

    private val _themeMode = MutableStateFlow(AppThemeMode.DARK)
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    // Connection & Loading States
    private val _isConnected = MutableStateFlow(true)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _connectionStatusText = MutableStateFlow("Demo Simulation Active")
    val connectionStatusText: StateFlow<String> = _connectionStatusText.asStateFlow()

    // Media Data States
    private val _mediaList = MutableStateFlow<List<MediaItem>>(emptyList())
    val mediaList: StateFlow<List<MediaItem>> = _mediaList.asStateFlow()

    private val _selectedMedia = MutableStateFlow<MediaItem?>(null)
    val selectedMedia: StateFlow<MediaItem?> = _selectedMedia.asStateFlow()

    // File Manager State
    private val _currentFilePath = MutableStateFlow("/")
    val currentFilePath: StateFlow<String> = _currentFilePath.asStateFlow()

    private val _fileDirectoryNodes = MutableStateFlow<List<FileNode>>(emptyList())
    val fileDirectoryNodes: StateFlow<List<FileNode>> = _fileDirectoryNodes.asStateFlow()

    // Cache Stats State
    private val _formattedCacheSize = MutableStateFlow("0.0 KB")
    val formattedCacheSize: StateFlow<String> = _formattedCacheSize.asStateFlow()

    // Realtime Terminal Log State
    private val _terminalLogs = MutableStateFlow<List<LogEntry>>(emptyList())
    val terminalLogs: StateFlow<List<LogEntry>> = _terminalLogs.asStateFlow()

    init {
        log(LogLevel.INFO, "BARRA", "Memulai BARRA CLOUD Direct SSH/SFTP & Embedded Tailscale Engine v1.1.0")
        log(LogLevel.INFO, "TAILSCALE", "Embedded Tailscale WireGuard engine ready. Auto-tunneling active.")
        log(LogLevel.INFO, "CACHE", "Cache Manager initialized (LRU Disk & Memory Engine)")
        
        // Auto connect embedded Tailscale tunnel if enabled
        viewModelScope.launch {
            val conf = _sftpConfig.value
            if (conf.useTailscale) {
                connectTailscale(conf.tailscaleAuthKey, conf.tailscaleIp, conf.tailscaleNodeName)
            } else {
                refreshAllData()
            }
        }
    }

    fun setTab(tab: BarraTab) {
        _activeTab.value = tab
        log(LogLevel.DEBUG, "NAV", "Berpindah ke menu ${tab.name}")
    }

    fun setGridColumns(columns: Int) {
        _gridColumnCount.value = columns.coerceIn(2, 6)
        log(LogLevel.INFO, "SETTINGS", "Kerapatan Grid diubah ke ${_gridColumnCount.value} kolom")
    }

    fun setThemeMode(mode: AppThemeMode) {
        _themeMode.value = mode
        log(LogLevel.INFO, "SETTINGS", "Tema diubah ke ${mode.name}")
    }

    fun updateConfig(config: SftpConfig) {
        _sftpConfig.value = config
        log(LogLevel.INFO, "CONFIG", "Konfigurasi SSH/SFTP diperbarui (Host: ${config.host}:${config.port}, Tailscale: ${if (config.useTailscale) "ON (${config.tailscaleIp})" else "OFF"})")
    }

    fun connectTailscale(
        authKey: String,
        targetIp: String,
        nodeName: String = "barra-mobile-app",
        onResult: ((Boolean, String) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val success = tailscaleEngine.connectEngine(authKey, targetIp, nodeName) { tag, msg, isErr ->
                log(if (isErr) LogLevel.ERROR else LogLevel.INFO, tag, msg)
            }
            if (success) {
                _connectionStatusText.value = "Tailscale Tunnel Connected ($targetIp)"
                _isConnected.value = true
                onResult?.invoke(true, "Terhubung ke jaringan Tailscale Mesh ($targetIp)!")
                refreshAllData()
            } else {
                _connectionStatusText.value = "Tailscale Connect Failed"
                onResult?.invoke(false, "Gagal menghubungkan Tailscale Engine.")
            }
        }
    }

    fun disconnectTailscale() {
        viewModelScope.launch {
            tailscaleEngine.disconnectEngine { tag, msg, isErr ->
                log(if (isErr) LogLevel.ERROR else LogLevel.INFO, tag, msg)
            }
            _connectionStatusText.value = "Tailscale Disconnected"
        }
    }

    fun refreshAllData() {
        viewModelScope.launch {
            _isRefreshing.value = true
            log(LogLevel.INFO, "SFTP", "Memulai sinkronisasi file media dari server...")

            val config = _sftpConfig.value
            val items = sftpService.fetchMediaList(config) { msg, isError ->
                log(if (isError) LogLevel.WARN else LogLevel.INFO, "SFTP_SCAN", msg)
            }

            _mediaList.value = items
            updateCacheStats()

            log(LogLevel.SUCCESS, "CACHE", "Generasi Hash SHA-1 selesai. ${items.size} file media siap dipreview.")

            // Refresh File Manager view
            loadDirectory(_currentFilePath.value)

            _isRefreshing.value = false
        }
    }

    fun loadDirectory(path: String) {
        viewModelScope.launch {
            _currentFilePath.value = path
            log(LogLevel.INFO, "SFTP", "Membuka direktori server: $path")
            val nodes = sftpService.listDirectory(_sftpConfig.value, path)
            _fileDirectoryNodes.value = nodes
            log(LogLevel.SUCCESS, "SFTP", "Direktori $path memuat ${nodes.size} item (Folder .thumbs dikecualikan)")
        }
    }

    fun navigateUpDirectory() {
        val path = _currentFilePath.value
        if (path == "/" || path.isEmpty()) return
        val parent = path.substringBeforeLast('/', missingDelimiterValue = "/")
        loadDirectory(if (parent.isEmpty()) "/" else parent)
    }

    fun testConnection(onResult: ((Boolean, String) -> Unit)? = null) {
        viewModelScope.launch {
            val currentConfig = _sftpConfig.value
            log(LogLevel.INFO, "SSH/SFTP", "Menguji koneksi SFTP ke ${currentConfig.host}:${currentConfig.port}...")
            val result = sftpService.testConnection(currentConfig)
            result.onSuccess { msg ->
                _isConnected.value = true
                _connectionStatusText.value = if (currentConfig.isDemoMode) "Demo Simulation" else "Online (Direct SSH/SFTP)"
                log(LogLevel.SUCCESS, "SSH/SFTP", msg)
                onResult?.invoke(true, msg)
                refreshAllData()
            }.onFailure { err ->
                _isConnected.value = false
                _connectionStatusText.value = "Koneksi Terputus"
                val errMsg = err.message ?: "Koneksi ke ${currentConfig.host}:${currentConfig.port} gagal."
                log(LogLevel.ERROR, "SSH/SFTP", errMsg)
                onResult?.invoke(false, errMsg)
            }
        }
    }

    fun selectMedia(item: MediaItem?) {
        _selectedMedia.value = item
        if (item != null) {
            log(
                LogLevel.INFO,
                "STREAM",
                "Membuka full view media [${item.name}]. SHA1 Hash: ${item.sha1Hash.take(8)}... (Streaming via SFTP)"
            )
        }
    }

    fun clearCache() {
        val success = cacheManager.clearCache()
        if (success) {
            log(LogLevel.SUCCESS, "CACHE", "Cache LRU lokal berhasil dibersihkan!")
        } else {
            log(LogLevel.WARN, "CACHE", "Pembersihan cache selesai dengan beberapa catatan.")
        }
        updateCacheStats()
    }

    private fun updateCacheStats() {
        _formattedCacheSize.value = cacheManager.getFormattedCacheSize()
    }

    fun log(level: LogLevel, tag: String, message: String) {
        val timeStr = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        val entry = LogEntry(timestamp = timeStr, level = level, tag = tag, message = message)
        _terminalLogs.value = (_terminalLogs.value + entry).takeLast(100)
    }

    fun clearLogs() {
        _terminalLogs.value = emptyList()
        log(LogLevel.INFO, "TERMINAL", "Terminal log dibersihkan.")
    }
}
