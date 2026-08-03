package com.example.core.model

import androidx.annotation.DrawableRes

enum class FileType {
    IMAGE,
    VIDEO,
    AUDIO,
    DOCUMENT,
    ARCHIVE,
    CODE,
    FOLDER,
    OTHER
}

enum class SortType {
    NAME,
    DATE,
    SIZE
}

enum class GridColumnOption(val count: Int) {
    TWO(2),
    THREE(3),
    FOUR(4),
    FIVE(5)
}

enum class ThumbnailSizeOption(val dpSize: Int, val label: String) {
    SMALL(80, "Small"),
    MEDIUM(120, "Medium"),
    LARGE(160, "Large"),
    XL(200, "XL")
}

enum class ThemeOption(val label: String) {
    LIGHT("Light"),
    DARK("Dark"),
    SYSTEM("Follow System")
}

enum class TailscaleConnectionState(val label: String) {
    DISCONNECTED("Disconnected"),
    CONNECTING("Connecting..."),
    CONNECTED("Connected"),
    ERROR("Connection Error")
}

data class MediaItem(
    val id: String,
    val name: String,
    val path: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val mimeType: String,
    val isFolder: Boolean = false,
    val isVideo: Boolean = false,
    val durationSeconds: Long? = null,
    val thumbnailUrl: String? = null,
    val localUri: String? = null,
    val albumName: String = "Root"
) {
    val fileType: FileType
        get() = when {
            isFolder -> FileType.FOLDER
            isVideo -> FileType.VIDEO
            mimeType.startsWith("image/") -> FileType.IMAGE
            mimeType.startsWith("audio/") -> FileType.AUDIO
            mimeType.contains("pdf") || mimeType.contains("word") || mimeType.contains("text") -> FileType.DOCUMENT
            mimeType.contains("zip") || mimeType.contains("rar") || mimeType.contains("tar") -> FileType.ARCHIVE
            mimeType.contains("json") || mimeType.contains("xml") || mimeType.contains("kt") -> FileType.CODE
            else -> FileType.OTHER
        }

    val formattedSize: String
        get() {
            if (isFolder) return ""
            val kb = sizeBytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format("%.2f GB", gb)
                mb >= 1.0 -> String.format("%.1f MB", mb)
                kb >= 1.0 -> String.format("%.0f KB", kb)
                else -> "$sizeBytes B"
            }
        }

    val formattedDuration: String
        get() {
            val totalSeconds = durationSeconds ?: return ""
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format("%02d:%02d", minutes, seconds)
        }
}

data class AlbumItem(
    val name: String,
    val folderPath: String,
    val itemCount: Int,
    val coverItem: MediaItem?
)

data class SambaConfig(
    val host: String = "100.64.0.1",
    val port: Int = 445,
    val shareName: String = "Media",
    val username: String = "admin",
    val password: String = "",
    val autoConnect: Boolean = true
)

data class TailscaleConfig(
    val enabled: Boolean = true,
    val autoStart: Boolean = true,
    val autoReconnect: Boolean = true,
    val authKey: String = "",
    val nodeIp: String = "100.64.1.42",
    val deviceName: String = "barra-nas",
    val exitNode: String = "None (Direct)",
    val connectionState: TailscaleConnectionState = TailscaleConnectionState.CONNECTED
)

data class ServerStats(
    val ip: String = "100.64.1.42",
    val connectionType: String = "Tailscale",
    val usedStorageBytes: Long = 480_000_000_000L,
    val totalStorageBytes: Long = 1_000_000_000_000L,
    val uptimeSeconds: Long = 1_234_567L,
    val temperatureCelsius: Float = 42.5f,
    val isConnected: Boolean = true
) {
    val formattedStoragePercent: Float
        get() = if (totalStorageBytes > 0) (usedStorageBytes.toFloat() / totalStorageBytes.toFloat()) else 0f

    val formattedStorageText: String
        get() {
            val usedGb = usedStorageBytes / (1024 * 1024 * 1024)
            val totalGb = totalStorageBytes / (1024 * 1024 * 1024)
            return "$usedGb GB / $totalGb GB"
        }

    val formattedUptime: String
        get() {
            val days = uptimeSeconds / (24 * 3600)
            val hours = (uptimeSeconds % (24 * 3600)) / 3600
            return "${days}d ${hours}h"
        }
}
