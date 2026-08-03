package com.example.barracloud.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val path: String,
    val name: String,
    val type: String,
    val size: Long,
    val mimeType: String = "",
    val addedAt: Long = System.currentTimeMillis()
)
