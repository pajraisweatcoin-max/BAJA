package com.example.data.remote

import android.content.Context
import com.example.data.model.LeaseStatus
import com.example.data.model.VpnLease
import com.example.util.AppLogger
import com.example.util.ConnectionLogger
import com.example.util.ToastManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class VpnLeaseManager private constructor(context: Context) {

    private val toastManager = ToastManager(context.applicationContext)
    private val logger = ConnectionLogger(context.applicationContext)

    private val _currentLease = MutableStateFlow<VpnLease?>(null)
    val currentLease: StateFlow<VpnLease?> = _currentLease.asStateFlow()

    companion object {
        @Volatile
        private var instance: VpnLeaseManager? = null

        fun getInstance(context: Context): VpnLeaseManager {
            return instance ?: synchronized(this) {
                instance ?: VpnLeaseManager(context.applicationContext).also { instance = it }
            }
        }
    }

    fun acquireLease(): VpnLease {
        val newSessionId = UUID.randomUUID().toString()
        val newUuid = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        val lease = VpnLease(
            sessionId = newSessionId,
            uuid = newUuid,
            connectionTime = now,
            disconnectTime = 0L,
            owner = "BarraCloud",
            leaseStatus = LeaseStatus.ACQUIRED,
            lastActivity = now
        )

        _currentLease.value = lease
        logger.log("Lease Acquired", "Session: ${newSessionId.take(8)} | Owner: BarraCloud")
        toastManager.showToast("Lease Acquired")
        AppLogger.s("LEASE", "VPN Lease berhasil diambil: ${lease.sessionId}")
        return lease
    }

    fun releaseLease() {
        val active = _currentLease.value
        if (active != null && active.leaseStatus == LeaseStatus.ACQUIRED) {
            val releasedLease = active.copy(
                disconnectTime = System.currentTimeMillis(),
                leaseStatus = LeaseStatus.RELEASED,
                lastActivity = System.currentTimeMillis()
            )
            _currentLease.value = releasedLease
            logger.log("Lease Released", "Session: ${active.sessionId.take(8)}")
            toastManager.showToast("Lease Released")
            AppLogger.i("LEASE", "VPN Lease telah dilepaskan: ${active.sessionId}")
        }
    }

    fun updateActivity() {
        _currentLease.value?.let { active ->
            if (active.leaseStatus == LeaseStatus.ACQUIRED) {
                _currentLease.value = active.copy(lastActivity = System.currentTimeMillis())
            }
        }
    }

    fun isLeaseActive(): Boolean {
        return _currentLease.value?.leaseStatus == LeaseStatus.ACQUIRED
    }
}
