package com.example.data.model

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
                name.endsWith(".gif", true) || name.endsWith(".heic", true)

    val isVideo: Boolean
        get() = mime.startsWith("video/", ignoreCase = true) ||
                name.endsWith(".mp4", true) || name.endsWith(".mkv", true) ||
                name.endsWith(".mov", true) || name.endsWith(".avi", true) ||
                name.endsWith(".webm", true)

    val isMedia: Boolean
        get() = isImage || isVideo
}

data class ServerConfig(
    val httpUrl: String = "http://homeserver.local",
    val adminPassword: String = "",
    val useTailscale: Boolean = false,
    val tailscaleAuthKey: String = "",
    val tailnetHost: String = "",
    val enableSamba: Boolean = true,
    val sambaHost: String = "192.168.1.100",
    val sambaPort: Int = 445,
    val sambaShare: String = "exthdd",
    val sambaUsername: String = "admin",
    val sambaPassword: String = "",
    val gridColumns: Int = 3,
    val appTheme: String = "SYSTEM", // "DARK", "LIGHT", "SYSTEM"
    val cacheLimitMb: Long = 1000L, // 500, 1000, -1
    val syncWifiOnly: Boolean = false
)

enum class ConnectionState {
    IDLE,
    CONNECTING,
    CONNECTED,
    ERROR
}

data class ConnectionStatus(
    val httpState: ConnectionState = ConnectionState.IDLE,
    val sambaState: ConnectionState = ConnectionState.IDLE,
    val httpMessage: String = "",
    val sambaMessage: String = ""
)
