package com.example.util

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.data.local.SecureStorage
import com.example.data.remote.ReconnectManager
import com.example.data.remote.SecureVpnOwnershipManager
import com.example.data.remote.TailscaleIntentManager
import com.example.data.remote.VPNMonitor
import com.example.data.remote.VpnLeaseManager
import com.example.service.BackgroundMonitorService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AppLifecycleManager(private val context: Context) : DefaultLifecycleObserver {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private val secureStorage = SecureStorage(appContext)
    private val intentManager = TailscaleIntentManager(appContext)
    private val leaseManager = VpnLeaseManager.getInstance(appContext)
    private val ownershipManager = SecureVpnOwnershipManager(appContext)
    private val vpnMonitor = VPNMonitor.getInstance(appContext)
    private val reconnectManager = ReconnectManager(appContext)
    private val toastManager = ToastManager(appContext)
    private val logger = ConnectionLogger(appContext)

    private var backgroundTimerJob: Job? = null

    private val _isTailscaleInstalled = MutableStateFlow(true)
    val isTailscaleInstalled: StateFlow<Boolean> = _isTailscaleInstalled.asStateFlow()

    private val _showTailscaleMissingDialog = MutableStateFlow(false)
    val showTailscaleMissingDialog: StateFlow<Boolean> = _showTailscaleMissingDialog.asStateFlow()

    companion object {
        @Volatile
        private var instance: AppLifecycleManager? = null

        fun getInstance(context: Context): AppLifecycleManager {
            return instance ?: synchronized(this) {
                instance ?: AppLifecycleManager(context.applicationContext).also { instance = it }
            }
        }
    }

    fun initObserver() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        onAppStarted()
    }

    fun dismissMissingDialog() {
        _showTailscaleMissingDialog.value = false
    }

    fun onAppStarted() {
        logger.log("Application Started", "Memulai inisialisasi integrasi Tailscale")
        toastManager.showToast("Checking Tailscale")

        val installed = intentManager.isTailscaleInstalled()
        _isTailscaleInstalled.value = installed

        if (!installed) {
            _showTailscaleMissingDialog.value = true
            toastManager.showToast("Tailscale belum terpasang.")
            return
        }

        val config = secureStorage.getConfig()
        if (!config.enableTailscale) return

        toastManager.showToast("Checking VPN")

        // Start VPN Monitoring
        vpnMonitor.startMonitoring(
            onVpnConnected = {
                if (!leaseManager.isLeaseActive()) {
                    ownershipManager.handleVpnConnectedExternally()
                }
            },
            onVpnDisconnected = {
                if (leaseManager.isLeaseActive()) {
                    if (config.autoReconnectVpn) {
                        reconnectManager.triggerAutoReconnect(scope)
                    } else {
                        ownershipManager.handleVpnDisconnectedExternally()
                    }
                }
            }
        )

        // Acquire Lease
        if (config.enableVpnLease) {
            leaseManager.acquireLease()
        }

        // Connect VPN if needed
        if (config.autoConnectVpn) {
            intentManager.sendConnectVpn()
        }

        // Apply Exit Node if configured
        if (config.enableExitNode && config.exitNodeName.isNotBlank()) {
            intentManager.sendUseExitNode(config.exitNodeName)
        }

        // Start Foreground Service
        if (config.connectionNotification) {
            BackgroundMonitorService.startService(appContext)
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        // App enters Foreground
        logger.log("Application Foreground", "Aplikasi kembali ke latar depan")

        // Cancel background timer if user returned
        backgroundTimerJob?.cancel()
        backgroundTimerJob = null

        val config = secureStorage.getConfig()
        if (!config.enableTailscale) return

        val installed = intentManager.isTailscaleInstalled()
        _isTailscaleInstalled.value = installed

        if (!installed) {
            _showTailscaleMissingDialog.value = true
            return
        }

        // Ensure VPN is active in foreground
        if (config.autoConnectVpn) {
            val isVpnOn = vpnMonitor.checkInitialVpnState()
            if (!isVpnOn) {
                intentManager.sendConnectVpn()
            }
        }

        if (config.enableVpnLease && !leaseManager.isLeaseActive()) {
            leaseManager.acquireLease()
        }

        vpnMonitor.startMonitoring()

        if (config.connectionNotification) {
            BackgroundMonitorService.startService(appContext)
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        // App enters Background
        logger.log("Application Background", "Aplikasi masuk ke latar belakang")

        val config = secureStorage.getConfig()
        if (!config.enableTailscale) return

        if (config.disconnectOnBackground) {
            val delaySeconds = config.backgroundDisconnectDelay.coerceAtLeast(1L)
            backgroundTimerJob?.cancel()
            backgroundTimerJob = scope.launch {
                AppLogger.i("LIFECYCLE", "Timer pemutusan background berjalan ($delaySeconds detik)...")
                delay(delaySeconds * 1000L)

                logger.log("Auto Disconnect", "Timer background selesai ($delaySeconds detik) -> Memutus VPN")
                intentManager.sendDisconnectVpn()
                leaseManager.releaseLease()
                BackgroundMonitorService.stopService(appContext)
            }
        }
    }

    fun onAppExit() {
        logger.log("Application Exit", "Aplikasi benar-benar keluar")
        backgroundTimerJob?.cancel()

        val config = secureStorage.getConfig()
        if (config.enableTailscale) {
            if (config.disconnectOnExit || config.secureVpnOwnership) {
                ownershipManager.enforceOwnershipOnExit()
            } else {
                intentManager.sendDisconnectVpn()
                leaseManager.releaseLease()
            }
        }

        vpnMonitor.stopMonitoring()
        BackgroundMonitorService.stopService(appContext)
    }
}
