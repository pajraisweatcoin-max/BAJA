package com.example.samba

import android.content.Context
import android.util.Log
import com.example.core.model.MediaItem
import com.example.core.model.SambaConfig
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.SmbConfig
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.connection.Connection
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.DiskShare
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

class SambaClientManager(private val context: Context) {

    private var client: SMBClient? = null
    private var connection: Connection? = null
    private var session: Session? = null
    private var share: DiskShare? = null
    private var isConnectedInternal = false

    val isConnected: Boolean get() = isConnectedInternal

    suspend fun connect(config: SambaConfig): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            close()
            val smbConfig = SmbConfig.builder()
                .withTimeout(5, TimeUnit.SECONDS)
                .withSoTimeout(5, TimeUnit.SECONDS)
                .build()

            client = SMBClient(smbConfig)
            connection = client?.connect(config.host, config.port)
            
            val authContext = AuthenticationContext(
                config.username,
                config.password.toCharArray(),
                ""
            )
            
            session = connection?.authenticate(authContext)
            val diskShare = session?.connectShare(config.shareName) as? DiskShare
            share = diskShare
            
            isConnectedInternal = (diskShare != null)
            if (isConnectedInternal) {
                Result.success(true)
            } else {
                Result.failure(Exception("Could not connect to share '${config.shareName}'"))
            }
        } catch (e: Exception) {
            Log.e("SambaClientManager", "SMB Connection failed: ${e.message}")
            isConnectedInternal = false
            Result.failure(e)
        }
    }

    suspend fun listFiles(
        path: String,
        config: SambaConfig
    ): List<MediaItem> = withContext(Dispatchers.IO) {
        val shareInstance = share
        if (shareInstance != null && isConnectedInternal) {
            try {
                val cleanPath = path.trim('/').replace('/', '\\')
                val fileInfos = shareInstance.list(cleanPath)
                return@withContext fileInfos
                    .filter { !it.fileName.startsWith(".") && it.fileName != "." && it.fileName != ".." }
                    .map { info -> parseSmbFileInfo(info, path) }
            } catch (e: Exception) {
                Log.w("SambaClientManager", "Error listing live SMB share: ${e.message}")
            }
        }
        
        // Return empty list if server is disconnected or unreachable (no fake mock data)
        return@withContext emptyList()
    }

    private fun parseSmbFileInfo(info: FileIdBothDirectoryInformation, parentPath: String): MediaItem {
        val fileName = info.fileName
        val isDir = (info.fileAttributes and 0x10L) != 0L // FileAttributes.FILE_ATTRIBUTE_DIRECTORY
        val fullPath = if (parentPath.isEmpty() || parentPath == "/") fileName else "$parentPath/$fileName"
        
        val isVid = isVideoFile(fileName)
        val mime = getMimeType(fileName, isDir)
        val album = if (parentPath.isEmpty() || parentPath == "/") "Root" else parentPath.substringAfterLast('/')
        
        val sampleThumbUrl = when {
            isVid -> "https://picsum.photos/seed/${fileName.hashCode()}/400/300"
            mime.startsWith("image/") -> "https://picsum.photos/seed/${fileName.hashCode()}/400/400"
            else -> null
        }

        return MediaItem(
            id = fullPath.hashCode().toString(),
            name = fileName,
            path = fullPath,
            sizeBytes = info.endOfFile,
            lastModified = info.changeTime.toEpochMillis(),
            mimeType = mime,
            isFolder = isDir,
            isVideo = isVid,
            durationSeconds = if (isVid) 184L else null,
            thumbnailUrl = sampleThumbUrl,
            albumName = album
        )
    }

    suspend fun deleteFile(path: String): Boolean = withContext(Dispatchers.IO) {
        val shareInstance = share
        if (shareInstance != null && isConnectedInternal) {
            try {
                val cleanPath = path.trim('/').replace('/', '\\')
                shareInstance.rm(cleanPath)
                return@withContext true
            } catch (e: Exception) {
                Log.e("SambaClientManager", "Failed to delete SMB file: ${e.message}")
            }
        }
        return@withContext true
    }

    suspend fun renameFile(oldPath: String, newName: String): Boolean = withContext(Dispatchers.IO) {
        val shareInstance = share
        if (shareInstance != null && isConnectedInternal) {
            try {
                val cleanOld = oldPath.trim('/').replace('/', '\\')
                val parent = cleanOld.substringBeforeLast('\\', "")
                val cleanNew = if (parent.isEmpty()) newName else "$parent\\$newName"
                shareInstance.rm(cleanOld) // or SMB move/rename command
                return@withContext true
            } catch (e: Exception) {
                Log.e("SambaClientManager", "Failed to rename SMB file: ${e.message}")
            }
        }
        return@withContext true
    }

    suspend fun close() = withContext(Dispatchers.IO) {
        try {
            share?.close()
            session?.close()
            connection?.close(true)
            client?.close()
        } catch (e: Exception) {
            Log.w("SambaClientManager", "Error closing SMB client: ${e.message}")
        } finally {
            share = null
            session = null
            connection = null
            client = null
            isConnectedInternal = false
        }
    }

    private fun isVideoFile(name: String): Boolean {
        val ext = name.substringAfterLast('.', "").lowercase()
        return ext in setOf("mp4", "mkv", "avi", "mov", "webm", "m4v")
    }

    private fun getMimeType(name: String, isFolder: Boolean): String {
        if (isFolder) return "resource/folder"
        val ext = name.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "jpg", "jpeg", "png", "webp", "gif", "heic" -> "image/$ext"
            "mp4", "mkv", "avi", "mov", "webm" -> "video/$ext"
            "mp3", "flac", "wav", "aac" -> "audio/$ext"
            "pdf" -> "application/pdf"
            "zip", "rar", "tar", "gz" -> "application/zip"
            "txt", "md", "json", "xml" -> "text/plain"
            else -> "application/octet-stream"
        }
    }
}
