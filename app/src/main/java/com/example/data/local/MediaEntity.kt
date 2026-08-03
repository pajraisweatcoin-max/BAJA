package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.MediaItem

@Entity(tableName = "media_cache")
data class MediaEntity(
    @PrimaryKey val path: String,
    val name: String,
    val isDir: Boolean,
    val size: Long,
    val mtime: Long,
    val mime: String,
    val sha1: String? = null
) {
    fun toMediaItem(): MediaItem = MediaItem(
        name = name,
        path = path,
        isDir = isDir,
        size = size,
        mtime = mtime,
        mime = mime,
        sha1 = sha1
    )

    companion object {
        fun fromMediaItem(item: MediaItem): MediaEntity = MediaEntity(
            path = item.path,
            name = item.name,
            isDir = item.isDir,
            size = item.size,
            mtime = item.mtime,
            mime = item.mime,
            sha1 = item.sha1
        )
    }
}
