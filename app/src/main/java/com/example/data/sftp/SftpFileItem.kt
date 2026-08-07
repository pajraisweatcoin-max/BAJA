package com.example.data.sftp

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SftpFileItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long,
    val permissions: String,
    val mtime: Long,
    val isSymlink: Boolean = false,
    val linkTarget: String? = null
) {
    val formattedSize: String
        get() {
            if (isDirectory) return "Folder"
            val kb = size / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format(Locale.US, "%.2f GB", gb)
                mb >= 1.0 -> String.format(Locale.US, "%.2f MB", mb)
                kb >= 1.0 -> String.format(Locale.US, "%.1f KB", kb)
                else -> "$size B"
            }
        }

    val formattedDate: String
        get() {
            if (mtime <= 0) return "-"
            val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            return sdf.format(Date(mtime * 1000L))
        }

    val fileExtension: String
        get() = name.substringAfterLast('.', "").lowercase()

    val isTextEditable: Boolean
        get() {
            if (isDirectory) return false
            val ext = fileExtension
            val textExtensions = setOf(
                "txt", "json", "xml", "yml", "yaml", "md", "log", "sh", "bash",
                "py", "js", "ts", "kt", "java", "c", "cpp", "h", "css", "html",
                "php", "conf", "ini", "env", "properties", "sql", "csv", "htaccess"
            )
            return textExtensions.contains(ext) || ext.isEmpty()
        }
}

data class ClipboardItem(
    val sourcePath: String,
    val fileName: String,
    val isDirectory: Boolean,
    val isCutMode: Boolean // true for Move (Cut), false for Copy
)

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

data class TransferTask(
    val id: String,
    val fileName: String,
    val isUpload: Boolean,
    val totalBytes: Long,
    var transferredBytes: Long = 0,
    var isCompleted: Boolean = false,
    var error: String? = null
)
