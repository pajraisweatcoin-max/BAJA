package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.local.SecureStorage
import com.example.data.remote.VPNMonitor
import com.example.util.AppLogger
import com.example.util.ConnectionLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class BackgroundMonitorService : Service() {

    companion object {
        const val CHANNEL_ID = "barracloud_vpn_monitor_channel"
        const val NOTIFICATION_ID = 8801

        fun startService(context: Context) {
            val intent = Intent(context, BackgroundMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, BackgroundMonitorService::class.java)
            context.stopService(intent)
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private lateinit var vpnMonitor: VPNMonitor
    private lateinit var secureStorage: SecureStorage
    private lateinit var logger: ConnectionLogger

    override fun onCreate() {
        super.onCreate()
        vpnMonitor = VPNMonitor.getInstance(this)
        secureStorage = SecureStorage(this)
        logger = ConnectionLogger(this)

        createNotificationChannel()
        startForegroundWithNotification("BarraCloud", "VPN Connected", "Secure VPN Ownership Active")

        serviceScope.launch {
            vpnMonitor.isVpnConnected.collectLatest { isConnected ->
                val config = secureStorage.getConfig()
                if (config.connectionNotification) {
                    val text = if (isConnected) "VPN Connected" else "VPN Disconnected"
                    updateNotification("BarraCloud", text, "Secure VPN Ownership Active")
                }
            }
        }
        AppLogger.i("SERVICE", "BackgroundMonitorService diciptakan")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        AppLogger.i("SERVICE", "BackgroundMonitorService diaktifkan")
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(true)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
        AppLogger.i("SERVICE", "BackgroundMonitorService dihentikan & Notifikasi dihapus")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "BarraCloud VPN Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifikasi status koneksi VPN Tailscale"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun startForegroundWithNotification(title: String, text: String, subtext: String) {
        val notification = buildNotification(title, text, subtext)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
                } else {
                    startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
                }
            } catch (e: Exception) {
                AppLogger.w("SERVICE", "Gagal startForeground dengan spesifik type: ${e.message}. Menggunakan default.")
                startForeground(NOTIFICATION_ID, notification)
            }
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(title: String, text: String, subtext: String) {
        val config = secureStorage.getConfig()
        if (!config.connectionNotification) {
            stopForeground(true)
            return
        }
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, buildNotification(title, text, subtext))
    }

    private fun buildNotification(title: String, text: String, subtext: String) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSubText(subtext)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()
}
