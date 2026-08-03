package com.example.barracloud.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentDao {
    @Query("SELECT * FROM recents ORDER BY lastOpenedAt DESC LIMIT 100")
    fun getAllRecents(): Flow<List<RecentEntity>>

    @Query("SELECT * FROM recents WHERE path = :path LIMIT 1")
    suspend fun getRecentByPath(path: String): RecentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateRecent(recent: RecentEntity)

    @Query("UPDATE recents SET playbackPositionMs = :positionMs, lastOpenedAt = :timestamp WHERE path = :path")
    suspend fun updatePlaybackPosition(path: String, positionMs: Long, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM recents WHERE path = :path")
    suspend fun deleteRecent(path: String)

    @Query("DELETE FROM recents")
    suspend fun clearAllRecents()
}
