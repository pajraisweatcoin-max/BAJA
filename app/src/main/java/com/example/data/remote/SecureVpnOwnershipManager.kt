package com.example.data.remote

import android.content.Context
import com.example.data.local.SecureStorage
import com.example.util.AppLogger
import com.example.util.ConnectionLogger
import com.example.util.ToastManager

class SecureVpnOwnershipManager(private val context: Context) {

    private val appContext = context.applicationContext
    private val secureStorage = SecureStorage(appContext)
    private val leaseManager = VpnLeaseManager.getInstance(appContext)
    private val intentManager = TailscaleIntentManager(appContext)
    private val logger = ConnectionLogger(appContext)
    private val toastManager = ToastManager(appContext)

    fun isOwnershipActive(): Boolean {
        val config = secureStorage.getConfig()
        return config.enableTailscale && config.secureVpnOwnership
    }

    fun handleVpnConnectedExternally() {
        val config = secureStorage.getConfig()
        if (!config.enableTailscale) return

        if (config.secureVpnOwnership && !leaseManager.isLeaseActive()) {
            logger.log("External VPN Connect", "VPN diaktifkan di luar penggunaan BarraCloud")
            if (!config.allowManualVpnControl) {
                logger.log("Forced Disconnect", "Memutus VPN secara otomatis karena Secure VPN Ownership Mode ON")
                toastManager.showToast("Forced Disconnect VPN")
                intentManager.sendDisconnectVpn()
            }
        } else {
            logger.log("External VPN Connect", "Sesi VPN terdeteksi aktif")
        }
    }

    fun handleVpnDisconnectedExternally() {
        val config = secureStorage.getConfig()
        if (!config.enableTailscale) return

        if (leaseManager.isLeaseActive() && config.keepVpnAlive) {
            logger.log("External VPN Disconnect", "VPN diputus pengguna dari aplikasi Tailscale")
            if (config.autoReconnectVpn) {
                logger.log("Forced Reconnect", "Menyambungkan kembali VPN sesuai kebijakan Keep Alive")
                intentManager.sendConnectVpn()
            }
        } else {
            logger.log("External VPN Disconnect", "Sesi VPN terputus")
        }
    }

    fun enforceOwnershipOnExit() {
        val config = secureStorage.getConfig()
        if (config.enableTailscale && config.secureVpnOwnership && config.disconnectOnExit) {
            logger.log("Forced Disconnect", "Aplikasi ditutup -> Memutus VPN secara otomatis")
            intentManager.sendDisconnectVpn()
            leaseManager.releaseLease()
        }
    }
}
