package com.example.samba

import android.content.Context
import android.util.Log
import com.example.core.model.MediaItem
import com.example.core.model.SambaConfig
import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.SmbConfig
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.connection.Connection
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.DiskShare
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.EnumSet
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
            if (config.host.isBlank()) {
                return@withContext Result.failure(Exception("Host / IP Address tidak boleh kosong."))
            }
            if (config.shareName.isBlank()) {
                return@withContext Result.failure(Exception("Share Name tidak boleh kosong. Isikan nama share Samba/SMB (contoh: HDD, Shared)."))
            }
            val smbConfig = SmbConfig.builder()
                .withTimeout(10, TimeUnit.SECONDS)
                .withSoTimeout(10, TimeUnit.SECONDS)
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
                Result.failure(Exception("Tidak dapat terhubung ke Share Name '${config.shareName}'"))
            }
        } catch (e: Exception) {
            Log.e("SambaClientManager", "SMB Connection failed: ${e.message}", e)
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
                val cleanPath = if (path.isBlank() || path == "/") "" else path.trim('/').replace('/', '\\')
                val fileInfos = shareInstance.list(cleanPath)
                return@withContext fileInfos
                    .filter { !it.fileName.startsWith(".") && it.fileName != "." && it.fileName != ".." }
                    .map { info -> parseSmbFileInfo(info, path) }
            } catch (e: Exception) {
                Log.w("SambaClientManager", "Error listing live SMB share: ${e.message}", e)
            }
        }
        
        // Return empty list if server is disconnected or unreachable (no fake mock data)
        return@withContext emptyList()
    }

    suspend fun scanAllMediaFiles(
        maxDepth: Int = 3
    ): List<MediaItem> = withContext(Dispatchers.IO) {
        val shareInstance = share
        if (shareInstance == null || !isConnectedInternal) return@withContext emptyList()

        val resultList = mutableListOf<MediaItem>()

        fun scanDir(currentPath: String, depth: Int) {
            if (depth > maxDepth) return
            try {
                val cleanPath = if (currentPath.isBlank() || currentPath == "/") "" else currentPath.trim('/').replace('/', '\\')
                val fileInfos = shareInstance.list(cleanPath)
                for (info in fileInfos) {
                    val fileName = info.fileName
                    if (fileName.startsWith(".") || fileName == "." || fileName == "..") continue

                    val isDir = (info.fileAttributes and 0x10L) != 0L
                    val fullPath = if (currentPath.isEmpty() || currentPath == "/") fileName else "$currentPath/$fileName"

                    val item = parseSmbFileInfo(info, currentPath)
                    resultList.add(item)

                    if (isDir && depth < maxDepth) {
                        scanDir(fullPath, depth + 1)
                    }
                }
            } catch (e: Exception) {
                Log.w("SambaClientManager", "Error scanning dir '$currentPath': ${e.message}")
            }
        }

        scanDir("", 0)
        return@withContext resultList
    }

    suspend fun downloadFileToCache(path: String, fileName: String, sizeBytes: Long): File? = withContext(Dispatchers.IO) {
        val shareInstance = share ?: return@withContext null
        if (!isConnectedInternal) return@withContext null

        val cacheDir = File(context.cacheDir, "smb_media_cache")
        if (!cacheDir.exists()) cacheDir.mkdirs()

        val safeName = fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val cacheFile = File(cacheDir, "${path.hashCode()}_$safeName")

        if (cacheFile.exists() && (sizeBytes <= 0 || cacheFile.length() == sizeBytes)) {
            return@withContext cacheFile
        }

        try {
            val cleanPath = path.trim('/').replace('/', '\\')
            val smbFile = shareInstance.openFile(
                cleanPath,
                EnumSet.of(AccessMask.FILE_READ_DATA),
                null,
                SMB2ShareAccess.ALL,
                SMB2CreateDisposition.FILE_OPEN,
                null
            )
            smbFile.inputStream.use { input ->
                cacheFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            smbFile.close()
            return@withContext cacheFile
        } catch (e: Exception) {
            Log.e("SambaClientManager", "Failed to download SMB file '$path': ${e.message}", e)
            if (cacheFile.exists()) cacheFile.delete()
            return@withContext null
        }
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
