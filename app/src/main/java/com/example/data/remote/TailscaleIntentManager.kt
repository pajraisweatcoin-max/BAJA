package com.example.data.remote

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.RemoteException
import com.example.util.AppLogger
import com.example.util.ConnectionLogger
import com.example.util.ToastManager

class TailscaleIntentManager(private val context: Context) {

    companion object {
        const val TAILSCALE_PACKAGE = "com.tailscale.ipn"
        const val TAILSCALE_RECEIVER = "com.tailscale.ipn.IPNReceiver"

        const val ACTION_CONNECT = "com.tailscale.ipn.CONNECT_VPN"
        const val ACTION_DISCONNECT = "com.tailscale.ipn.DISCONNECT_VPN"
        const val ACTION_USE_EXIT_NODE = "com.tailscale.ipn.USE_EXIT_NODE"
    }

    private val toastManager = ToastManager(context)
    private val logger = ConnectionLogger(context)

    fun isTailscaleInstalled(): Boolean {
        // Try multiple lookup techniques for package visibility across Android OS versions
        return try {
            context.packageManager.getPackageInfo(TAILSCALE_PACKAGE, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(TAILSCALE_PACKAGE)
            if (launchIntent != null) {
                true
            } else {
                val broadcastIntent = Intent(ACTION_CONNECT).apply { setPackage(TAILSCALE_PACKAGE) }
                val receivers = context.packageManager.queryBroadcastReceivers(broadcastIntent, 0)
                if (receivers.isNotEmpty()) {
                    true
                } else {
                    logger.log("Package Missing", "Tailscale ($TAILSCALE_PACKAGE) tidak terdeteksi via PackageManager")
                    false
                }
            }
        } catch (e: Exception) {
            AppLogger.e("TAILSCALE", "Gagal memeriksa status aplikasi Tailscale: ${e.message}")
            false
        }
    }

    fun openTailscaleApp(): Boolean {
        return try {
            var launchIntent = context.packageManager.getLaunchIntentForPackage(TAILSCALE_PACKAGE)
            if (launchIntent == null) {
                launchIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    setPackage(TAILSCALE_PACKAGE)
                }
            }
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            toastManager.showToast("Membuka Tailscale...")
            logger.log("Open Tailscale", "Membuka aplikasi Tailscale")
            AppLogger.i("TAILSCALE", "Membuka aplikasi Tailscale")
            true
        } catch (e: Exception) {
            toastManager.showToast("Gagal membuka Tailscale")
            logger.log("Package Missing", "Aplikasi Tailscale tidak dapat dibuka: ${e.message}")
            AppLogger.e("TAILSCALE", "Gagal membuka Tailscale: ${e.message}")
            false
        }
    }

    fun sendConnectVpn(): Boolean {
        if (!isTailscaleInstalled()) {
            toastManager.showToast("Tailscale belum terpasang.")
            return false
        }

        return try {
            toastManager.showDirectToast("VPN Tailscale Terhubung")
            logger.log("Connecting VPN", "Mengirim broadcast $ACTION_CONNECT")

            val intent = Intent(ACTION_CONNECT).apply {
                component = ComponentName(TAILSCALE_PACKAGE, TAILSCALE_RECEIVER)
                setPackage(TAILSCALE_PACKAGE)
                addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
            }
            context.sendBroadcast(intent)
            AppLogger.i("TAILSCALE", "Broadcast CONNECT_VPN telah dikirim ke $TAILSCALE_RECEIVER")
            true
        } catch (e: Exception) {
            handleBroadcastException("CONNECT_VPN", e)
            false
        }
    }

    fun sendDisconnectVpn(): Boolean {
        if (!isTailscaleInstalled()) {
            return false
        }

        return try {
            toastManager.showToast("Disconnecting VPN")
            logger.log("Disconnecting VPN", "Mengirim broadcast $ACTION_DISCONNECT")

            val intent = Intent(ACTION_DISCONNECT).apply {
                component = ComponentName(TAILSCALE_PACKAGE, TAILSCALE_RECEIVER)
                setPackage(TAILSCALE_PACKAGE)
                addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
            }
            context.sendBroadcast(intent)
            AppLogger.i("TAILSCALE", "Broadcast DISCONNECT_VPN telah dikirim ke $TAILSCALE_RECEIVER")
            true
        } catch (e: Exception) {
            handleBroadcastException("DISCONNECT_VPN", e)
            false
        }
    }

    fun sendUseExitNode(exitNodeName: String): Boolean {
        if (!isTailscaleInstalled()) {
            return false
        }

        return try {
            logger.log("USE_EXIT_NODE", "Mengirim broadcast USE_EXIT_NODE dengan exit node: $exitNodeName")

            val intent = Intent(ACTION_USE_EXIT_NODE).apply {
                component = ComponentName(TAILSCALE_PACKAGE, TAILSCALE_RECEIVER)
                setPackage(TAILSCALE_PACKAGE)
                putExtra("name", exitNodeName)
                putExtra("exit_node", exitNodeName)
                addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
            }
            context.sendBroadcast(intent)
            AppLogger.i("TAILSCALE", "Broadcast USE_EXIT_NODE dikirim ($exitNodeName)")
            true
        } catch (e: Exception) {
            handleBroadcastException("USE_EXIT_NODE", e)
            false
        }
    }

    private fun handleBroadcastException(action: String, e: Exception) {
        when (e) {
            is SecurityException -> AppLogger.e("TAILSCALE", "SecurityException saat $action: ${e.message}")
            is IllegalStateException -> AppLogger.e("TAILSCALE", "IllegalStateException saat $action: ${e.message}")
            is RemoteException -> AppLogger.e("TAILSCALE", "RemoteException saat $action: ${e.message}")
            else -> AppLogger.e("TAILSCALE", "Gagal mengirim $action: ${e.message}")
        }
    }
}
