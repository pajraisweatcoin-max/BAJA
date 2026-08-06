package com.example.data.remote

import android.content.Context
import com.example.data.local.SecureStorage
import com.example.util.AppLogger
import com.example.util.ConnectionLogger
import com.example.util.ToastManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ReconnectManager(private val context: Context) {

    private val appContext = context.applicationContext
    private val tailscaleIntentManager = TailscaleIntentManager(appContext)
    private val secureStorage = SecureStorage(appContext)
    private val toastManager = ToastManager(appContext)
    private val logger = ConnectionLogger(appContext)
    private val vpnMonitor = VPNMonitor.getInstance(appContext)

    private var reconnectJob: Job? = null

    fun triggerAutoReconnect(scope: CoroutineScope, onFinished: (Boolean) -> Unit = {}) {
        val config = secureStorage.getConfig()
        if (!config.autoReconnectVpn) {
            AppLogger.i("RECONNECT", "Auto Reconnect dimatikan dalam preferensi")
            return
        }

        reconnectJob?.cancel()
        reconnectJob = scope.launch(Dispatchers.IO) {
            toastManager.showToast("VPN terputus")
            logger.log("VPN Disconnected", "Memulai proses auto-reconnect")

            val maxRetries = config.reconnectRetry.coerceAtLeast(1)
            val retryDelay = config.reconnectDelay.coerceAtLeast(500L)
            var attempts = 0
            var reconnected = false

            while (attempts < maxRetries && !reconnected) {
                attempts++
                logger.log("Reconnect Attempt", "Percobaan $attempts dari $maxRetries")
                toastManager.showToast("Menyambungkan ulang... ($attempts/$maxRetries)")

                tailscaleIntentManager.sendConnectVpn()

                // Wait for retryDelay to verify if VPN is connected
                delay(retryDelay)

                if (vpnMonitor.checkInitialVpnState()) {
                    reconnected = true
                    logger.log("Reconnect Success", "VPN terhubung kembali pada percobaan $attempts")
                    toastManager.showToast("VPN tersambung kembali")
                    AppLogger.s("RECONNECT", "Berhasil menyambung ulang VPN pada percobaan $attempts")
                    break
                }
            }

            if (!reconnected) {
                logger.log("Reconnect Failed", "Gagal menyambungkan VPN setelah $maxRetries percobaan")
                toastManager.showToast("Gagal menyambungkan VPN")
                AppLogger.e("RECONNECT", "Gagal menyambung ulang VPN")
            }

            onFinished(reconnected)
        }
    }

    fun cancelReconnect() {
        reconnectJob?.cancel()
        reconnectJob = null
    }
}
