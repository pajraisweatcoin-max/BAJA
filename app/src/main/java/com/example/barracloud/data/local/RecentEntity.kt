package com.example.barracloud.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recents")
data class RecentEntity(
    @PrimaryKey val path: String,
    val name: String,
    val type: String,
    val size: Long,
    val playbackPositionMs: Long = 0L,
    val lastOpenedAt: Long = System.currentTimeMillis()
)
