package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import java.io.File
import java.io.FileOutputStream

class CacheManager(context: Context) {

    private val cacheDir: File = File(context.cacheDirectory, "barra_thumbs").apply {
        if (!exists()) mkdirs()
    }

    // 32 MB Memory LRU Cache for decoded Bitmaps
    private val maxMemorySizeKb = (Runtime.getRuntime().maxMemory() / 1024 / 4).toInt()
    private val memoryCache = object : LruCache<String, Bitmap>(maxMemorySizeKb) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    /**
     * Get bitmap from memory LRU or Disk cache
     */
    fun getThumbnail(sha1Hash: String): Bitmap? {
        // 1. Memory cache check
        val memBitmap = memoryCache.get(sha1Hash)
        if (memBitmap != null && !memBitmap.isRecycled) {
            return memBitmap
        }

        // 2. Disk cache check
        val file = File(cacheDir, "$sha1Hash.jpg")
        if (file.exists()) {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            if (bitmap != null) {
                memoryCache.put(sha1Hash, bitmap)
                return bitmap
            }
        }
        return null
    }

    /**
     * Save thumbnail bytes to memory & disk cache
     */
    fun saveThumbnail(sha1Hash: String, bytes: ByteArray): Bitmap? {
        val file = File(cacheDir, "$sha1Hash.jpg")
        try {
            FileOutputStream(file).use { out ->
                out.write(bytes)
            }
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            if (bitmap != null) {
                memoryCache.put(sha1Hash, bitmap)
            }
            return bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * Checks if thumbnail exists in disk cache
     */
    fun hasThumbnail(sha1Hash: String): Boolean {
        return memoryCache.get(sha1Hash) != null || File(cacheDir, "$sha1Hash.jpg").exists()
    }

    /**
     * Total cache size in bytes
     */
    fun getCacheSizeBytes(): Long {
        var size: Long = 0
        cacheDir.listFiles()?.forEach { file ->
            if (file.isFile) {
                size += file.length()
            }
        }
        return size
    }

    /**
     * Formatted string of current cache size (e.g. "14.2 MB")
     */
    fun getFormattedCacheSize(): String {
        val bytes = getCacheSizeBytes()
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
            else -> "%.2f MB".format(bytes / (1024.0 * 1024.0))
        }
    }

    /**
     * Clear all cached files and reset memory cache
     */
    fun clearCache(): Boolean {
        memoryCache.evictAll()
        var success = true
        cacheDir.listFiles()?.forEach { file ->
            if (file.isFile) {
                if (!file.delete()) success = false
            }
        }
        return success
    }

    private val Context.cacheDirectory: File
        get() = this.cacheDir
}
