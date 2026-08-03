package com.example.barracloud.data.models

import java.util.Locale

enum class MediaType {
    PHOTO,
    VIDEO,
    RAW
}

data class MediaItem(
    val id: String,
    val name: String,
    val path: String,
    val parentFolder: String,
    val type: MediaType,
    val size: Long = 0L,
    val lastModified: Long = System.currentTimeMillis(),
    val mimeType: String = ""
) {
    val extension: String
        get() = name.substringAfterLast('.', "").lowercase(Locale.ROOT)

    val isGif: Boolean
        get() = extension == "gif"

    val formattedSize: String
        get() {
            if (size <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
            return String.format(
                Locale.US,
                "%.1f %s",
                size / Math.pow(1024.0, digitGroups.toDouble()),
                units[digitGroups.coerceAtMost(units.size - 1)]
            )
        }

    companion object {
        val PHOTO_EXTENSIONS = setOf(
            "jpg", "jpeg", "png", "webp", "heic", "gif", "bmp"
        )
        val RAW_EXTENSIONS = setOf(
            "cr2", "nef", "arw", "dng", "orf", "rw2", "pef", "raf"
        )
        val VIDEO_EXTENSIONS = setOf(
            "mp4", "mkv", "avi", "mov", "m4v", "3gp", "webm", "ts", "mpeg", "mpg", "flv"
        )

        fun determineType(filename: String): MediaType? {
            val ext = filename.substringAfterLast('.', "").lowercase(Locale.ROOT)
            return when {
                PHOTO_EXTENSIONS.contains(ext) -> MediaType.PHOTO
                RAW_EXTENSIONS.contains(ext) -> MediaType.RAW
                VIDEO_EXTENSIONS.contains(ext) -> MediaType.VIDEO
                else -> null
            }
        }
    }
}
