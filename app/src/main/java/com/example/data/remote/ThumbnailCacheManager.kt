package com.example.data.remote

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class ThumbnailCacheManager(private val context: Context) {

    private val cacheDir: File
        get() = File(context.cacheDir, "image_cache")

    suspend fun getCacheSizeBytes(): Long = withContext(Dispatchers.IO) {
        calculateFolderSize(context.cacheDir)
    }

    suspend fun getFormattedCacheSize(): String = withContext(Dispatchers.IO) {
        val bytes = getCacheSizeBytes()
        when {
            bytes >= 1024 * 1024 * 1024 -> String.format("%.2f GB", bytes.toDouble() / (1024 * 1024 * 1024))
            bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes.toDouble() / (1024 * 1024))
            bytes >= 1024 -> String.format("%.0f KB", bytes.toDouble() / 1024)
            else -> "$bytes Bytes"
        }
    }

    suspend fun clearCache(): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            deleteFolderContents(context.cacheDir)
            true
        }.getOrDefault(false)
    }

    private fun calculateFolderSize(folder: File): Long {
        var size = 0L
        val files = folder.listFiles() ?: return 0L
        for (file in files) {
            size += if (file.isDirectory) {
                calculateFolderSize(file)
            } else {
                file.length()
            }
        }
        return size
    }

    private fun deleteFolderContents(folder: File) {
        val files = folder.listFiles() ?: return
        for (file in files) {
            if (file.isDirectory) {
                deleteFolderContents(file)
            }
            file.delete()
        }
    }
}
