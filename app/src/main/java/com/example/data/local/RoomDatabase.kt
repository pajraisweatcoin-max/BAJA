package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.core.model.MediaItem
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "cached_media_items")
data class MediaItemEntity(
    @PrimaryKey val id: String,
    val name: String,
    val path: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val mimeType: String,
    val isFolder: Boolean,
    val isVideo: Boolean,
    val durationSeconds: Long?,
    val thumbnailUrl: String?,
    val localUri: String?,
    val albumName: String
) {
    fun toDomain(): MediaItem {
        return MediaItem(
            id = id,
            name = name,
            path = path,
            sizeBytes = sizeBytes,
            lastModified = lastModified,
            mimeType = mimeType,
            isFolder = isFolder,
            isVideo = isVideo,
            durationSeconds = durationSeconds,
            thumbnailUrl = thumbnailUrl,
            localUri = localUri,
            albumName = albumName
        )
    }

    companion object {
        fun fromDomain(item: MediaItem): MediaItemEntity {
            return MediaItemEntity(
                id = item.id,
                name = item.name,
                path = item.path,
                sizeBytes = item.sizeBytes,
                lastModified = item.lastModified,
                mimeType = item.mimeType,
                isFolder = item.isFolder,
                isVideo = item.isVideo,
                durationSeconds = item.durationSeconds,
                thumbnailUrl = item.thumbnailUrl,
                localUri = item.localUri,
                albumName = item.albumName
            )
        }
    }
}

@Dao
interface MediaCacheDao {
    @Query("SELECT * FROM cached_media_items ORDER BY lastModified DESC")
    fun getAllItems(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM cached_media_items WHERE isVideo = 0 AND isFolder = 0 ORDER BY lastModified DESC")
    fun getPhotos(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM cached_media_items WHERE isVideo = 1 ORDER BY lastModified DESC")
    fun getVideos(): Flow<List<MediaItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<MediaItemEntity>)

    @Query("DELETE FROM cached_media_items")
    suspend fun clearAll()

    @Query("DELETE FROM cached_media_items WHERE isVideo = 0 AND isFolder = 0")
    suspend fun clearPhotoCache()

    @Query("DELETE FROM cached_media_items WHERE isVideo = 1")
    suspend fun clearVideoCache()
}

@Database(entities = [MediaItemEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mediaCacheDao(): MediaCacheDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "barra_cloud_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
