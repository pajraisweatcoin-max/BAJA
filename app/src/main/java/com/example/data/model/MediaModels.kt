package com.example.data.model

import java.util.UUID

data class MediaItem(
    val name: String,
    val path: String,
    val isDir: Boolean = false,
    val size: Long = 0L,
    val mtime: Long = System.currentTimeMillis(),
    val mime: String = if (isDir) "directory" else "application/octet-stream",
    val sha1: String? = null
) {
    val isImage: Boolean
        get() = mime.startsWith("image/", ignoreCase = true) ||
                name.endsWith(".jpg", true) || name.endsWith(".jpeg", true) ||
                name.endsWith(".png", true) || name.endsWith(".webp", true) ||
                name.endsWith(".gif", true) || name.endsWith(".heic", true) ||
                name.endsWith(".heif", true) || name.endsWith(".bmp", true) ||
                name.endsWith(".tiff", true) || name.endsWith(".tif", true) ||
                name.endsWith(".avif", true) || name.endsWith(".raw", true) ||
                name.endsWith(".dng", true) || name.endsWith(".cr2", true) ||
                name.endsWith(".nef", true)

    val isVideo: Boolean
        get() = mime.startsWith("video/", ignoreCase = true) ||
                name.endsWith(".mp4", true) || name.endsWith(".mkv", true) ||
                name.endsWith(".mov", true) || name.endsWith(".avi", true) ||
                name.endsWith(".webm", true) || name.endsWith(".flv", true) ||
                name.endsWith(".wmv", true) || name.endsWith(".m4v", true) ||
                name.endsWith(".3gp", true) || name.endsWith(".ts", true) ||
                name.endsWith(".m2ts", true) || name.endsWith(".mpg", true) ||
                name.endsWith(".mpeg", true)

    val isMedia: Boolean
        get() = isImage || isVideo
}

enum class LeaseStatus {
    ACQUIRED,
    RELEASED,
    EXPIRED
}

data class VpnLease(
    val sessionId: String = UUID.randomUUID().toString(),
    val uuid: String = UUID.randomUUID().toString(),
    val connectionTime: Long = System.currentTimeMillis(),
    val disconnectTime: Long = 0L,
    val owner: String = "BarraCloud",
    val leaseStatus: LeaseStatus = LeaseStatus.RELEASED,
    val lastActivity: Long = System.currentTimeMillis()
)

data class ServerConfig(
    val httpUrl: String = "http://homeserver.local",
    val adminPassword: String = "",
    val enableSftp: Boolean = true,
    val autoConnectSftp: Boolean = true,
    val sftpHost: String = "192.168.1.100",
    val sftpPort: Int = 22,
    val sftpUsername: String = "root",
    val sftpPassword: String = "",
    val adminModeSftp: Boolean = false,
    val gridColumns: Int = 3,
    val appTheme: String = "SYSTEM", // "DARK", "LIGHT", "SYSTEM"
    val cacheLimitMb: Long = 1000L, // 500, 1000, -1
    val syncWifiOnly: Boolean = false,

    // Tailscale Integration Settings
    val enableTailscale: Boolean = true,
    val secureVpnOwnership: Boolean = true,
    val enableVpnLease: Boolean = true,
    val autoConnectVpn: Boolean = true,
    val autoDisconnectVpn: Boolean = true,
    val autoReconnectVpn: Boolean = true,
    val monitorVpn: Boolean = true,
    val monitorTailscale: Boolean = true,
    val disconnectOnBackground: Boolean = true,
    val disconnectOnExit: Boolean = true,
    val keepVpnAlive: Boolean = true,
    val autoOpenTailscaleIfLoginRequired: Boolean = true,
    val enableExitNode: Boolean = false,
    val showConnectionToast: Boolean = true,
    val connectionNotification: Boolean = true,
    val saveConnectionHistory: Boolean = true,
    val allowManualVpnControl: Boolean = false,
    val exitNodeName: String = "",
    val reconnectDelay: Long = 1000L,
    val reconnectRetry: Int = 5,
    val backgroundDisconnectDelay: Long = 30L
)

enum class ConnectionState {
    IDLE,
    CONNECTING,
    CONNECTED,
    ERROR
}

data class ConnectionStatus(
    val httpState: ConnectionState = ConnectionState.IDLE,
    val sftpState: ConnectionState = ConnectionState.IDLE,
    val tailscaleState: ConnectionState = ConnectionState.IDLE,
    val httpMessage: String = "",
    val sftpMessage: String = "",
    val tailscaleMessage: String = ""
)
