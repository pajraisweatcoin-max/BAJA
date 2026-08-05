package com.example.model

enum class TailscaleStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED
}

data class SftpConfig(
    val host: String = "100.112.84.50",
    val port: Int = 22,
    val username: String = "barra",
    val password: String = "secret123",
    val mediaRoot: String = "/mnt/exthdd",
    val isDemoMode: Boolean = true,
    val useTailscale: Boolean = true,
    val tailscaleAuthKey: String = "tskey-auth-k1234567890abcdef-1234567890",
    val tailscaleIp: String = "100.112.84.50",
    val tailscaleNodeName: String = "barra-mobile-app"
)

data class MediaItem(
    val id: String,
    val name: String,
    val relativePath: String,
    val isVideo: Boolean,
    val sizeBytes: Long,
    val mtimeMs: Long,
    val sha1Hash: String,
    val formattedDate: String,
    val formattedSize: String
)

data class FileNode(
    val name: String,
    val relativePath: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val mtimeMs: Long,
    val permissions: String = "rw-r--r--"
)

enum class LogLevel {
    INFO, SUCCESS, WARN, ERROR, DEBUG
}

data class LogEntry(
    val id: Long = System.currentTimeMillis() + (0..999).random(),
    val timestamp: String,
    val level: LogLevel,
    val tag: String,
    val message: String
)

enum class AppThemeMode {
    DARK, OLED
}

enum class BarraTab {
    HOME, PHOTO, VIDEO, FILE, SETTINGS
}
