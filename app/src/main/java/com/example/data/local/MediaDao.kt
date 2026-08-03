package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    @Query("SELECT * FROM media_cache WHERE isDir = 0 ORDER BY mtime DESC")
    fun getAllMedia(): Flow<List<MediaEntity>>

    @Query("SELECT * FROM media_cache WHERE isDir = 0 AND (mime LIKE 'image/%' OR name LIKE '%.jpg' OR name LIKE '%.jpeg' OR name LIKE '%.png' OR name LIKE '%.webp' OR name LIKE '%.heic') ORDER BY mtime DESC")
    fun getAllPhotos(): Flow<List<MediaEntity>>

    @Query("SELECT * FROM media_cache WHERE isDir = 0 AND (mime LIKE 'video/%' OR name LIKE '%.mp4' OR name LIKE '%.mkv' OR name LIKE '%.mov' OR name LIKE '%.avi' OR name LIKE '%.webm') ORDER BY mtime DESC")
    fun getAllVideos(): Flow<List<MediaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<MediaEntity>)

    @Query("DELETE FROM media_cache")
    suspend fun clearAll()
}
