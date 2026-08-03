package com.example.barracloud.smb

import android.util.Log
import com.example.barracloud.data.models.MediaItem
import com.example.barracloud.data.models.MediaType
import com.example.barracloud.data.models.SmbCredentials
import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.msfscc.FileAttributes
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.SmbConfig
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.connection.Connection
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.DiskShare
import com.hierynomus.smbj.share.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

sealed class SmbConnectionState {
    object Disconnected : SmbConnectionState()
    object Connecting : SmbConnectionState()
    data class Connected(val server: String, val share: String) : SmbConnectionState()
    data class Error(val message: String) : SmbConnectionState()
}

class SmbConnectionManager {

    private var smbClient: SMBClient? = null
    private var currentConnection: Connection? = null
    private var currentSession: Session? = null
    private var currentDiskShare: DiskShare? = null
    private var activeCredentials: SmbCredentials? = null

    private val _connectionState = MutableStateFlow<SmbConnectionState>(SmbConnectionState.Disconnected)
    val connectionState: StateFlow<SmbConnectionState> = _connectionState.asStateFlow()

    // Cache of open files for fast random-access reading
    private val openFileCache = ConcurrentHashMap<String, File>()

    @Synchronized
    private fun getClient(): SMBClient {
        if (smbClient == null) {
            val config = SmbConfig.builder()
                .withTimeout(15, TimeUnit.SECONDS)
                .withSoTimeout(15, TimeUnit.SECONDS)
                .build()
            smbClient = SMBClient(config)
        }
        return smbClient!!
    }

    suspend fun connect(credentials: SmbCredentials): Result<Boolean> = withContext(Dispatchers.IO) {
        _connectionState.value = SmbConnectionState.Connecting
        try {
            disconnectInternal()
            activeCredentials = credentials

            val client = getClient()
            val conn = client.connect(credentials.server, credentials.port)
            currentConnection = conn

            val authCtx = if (credentials.username.isBlank()) {
                AuthenticationContext.anonymous()
            } else {
                AuthenticationContext(
                    credentials.username,
                    credentials.password.toCharArray(),
                    credentials.domain
                )
            }

            val sess = conn.authenticate(authCtx)
            currentSession = sess

            val diskShare = sess.connectShare(credentials.shareName) as DiskShare
            currentDiskShare = diskShare

            _connectionState.value = SmbConnectionState.Connected(
                server = credentials.server,
                share = credentials.shareName
            )
            Log.d(TAG, "Successfully connected to SMB share: ${credentials.server}/${credentials.shareName}")
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "SMB connection failed", e)
            val errorMsg = e.localizedMessage ?: "Gagal terhubung ke server SMB"
            _connectionState.value = SmbConnectionState.Error(errorMsg)
            Result.failure(e)
        }
    }

    suspend fun listMediaFiles(folderPath: String = ""): Result<List<MediaItem>> = withContext(Dispatchers.IO) {
        val share = currentDiskShare ?: return@withContext Result.failure(IllegalStateException("Belum terhubung ke SMB"))
        val resultList = mutableListOf<MediaItem>()

        try {
            val cleanFolderPath = folderPath.trim('/').replace('/', '\\')
            val fileInfos = share.list(cleanFolderPath)

            for (info in fileInfos) {
                val fileName = info.fileName
                if (fileName == "." || fileName == "..") continue

                val isDirectory = (info.fileAttributes and FileAttributes.FILE_ATTRIBUTE_DIRECTORY.value) != 0L

                val fullPath = if (folderPath.isBlank()) fileName else "$folderPath/$fileName"

                if (isDirectory) {
                    // Recursively or scan subfolders
                    try {
                        val subItems = scanDirectoryRecursively(share, fullPath, maxDepth = 2, currentDepth = 1)
                        resultList.addAll(subItems)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed scanning subfolder $fullPath", e)
                    }
                } else {
                    val mediaType = MediaItem.determineType(fileName)
                    if (mediaType != null) {
                        resultList.add(
                            MediaItem(
                                id = fullPath,
                                name = fileName,
                                path = fullPath,
                                parentFolder = folderPath.ifBlank { "Root" },
                                type = mediaType,
                                size = info.endOfFile,
                                lastModified = runCatching { info.changeTime?.toEpochMillis() }.getOrNull() ?: System.currentTimeMillis()
                            )
                        )
                    }
                }
            }
            Result.success(resultList)
        } catch (e: Exception) {
            Log.e(TAG, "Error listing files in $folderPath", e)
            // Attempt auto-reconnect if connection was dropped
            activeCredentials?.let { creds ->
                val reconnectRes = connect(creds)
                if (reconnectRes.isSuccess) {
                    return@withContext listMediaFiles(folderPath)
                }
            }
            Result.failure(e)
        }
    }

    private fun scanDirectoryRecursively(
        share: DiskShare,
        folderPath: String,
        maxDepth: Int,
        currentDepth: Int
    ): List<MediaItem> {
        val list = mutableListOf<MediaItem>()
        val cleanPath = folderPath.trim('/').replace('/', '\\')
        val fileInfos = share.list(cleanPath)

        for (info in fileInfos) {
            val name = info.fileName
            if (name == "." || name == "..") continue

            val isDir = (info.fileAttributes and FileAttributes.FILE_ATTRIBUTE_DIRECTORY.value) != 0L

            val fullPath = "$folderPath/$name"

            if (isDir && currentDepth < maxDepth) {
                list.addAll(scanDirectoryRecursively(share, fullPath, maxDepth, currentDepth + 1))
            } else if (!isDir) {
                val type = MediaItem.determineType(name)
                if (type != null) {
                    list.add(
                        MediaItem(
                            id = fullPath,
                            name = name,
                            path = fullPath,
                            parentFolder = folderPath,
                            type = type,
                            size = info.endOfFile,
                            lastModified = runCatching { info.changeTime?.toEpochMillis() }.getOrNull() ?: System.currentTimeMillis()
                        )
                    )
                }
            }
        }
        return list
    }

    fun getFileLength(path: String): Long {
        return try {
            val share = currentDiskShare ?: return 0L
            val cleanPath = path.trim('/').replace('/', '\\')
            if (share.fileExists(cleanPath)) {
                val fileInfo = share.getFileInformation(cleanPath)
                fileInfo.standardInformation.endOfFile
            } else 0L
        } catch (e: Exception) {
            Log.e(TAG, "Failed getting file length for $path", e)
            0L
        }
    }

    fun openInputStreamAtOffset(path: String, offset: Long = 0L): InputStream? {
        val share = currentDiskShare ?: return null
        val cleanPath = path.trim('/').replace('/', '\\')

        return try {
            val file = share.openFile(
                cleanPath,
                setOf(AccessMask.GENERIC_READ),
                null,
                SMB2ShareAccess.ALL,
                SMB2CreateDisposition.FILE_OPEN,
                null
            )
            val stream = file.inputStream
            if (offset > 0) {
                stream.skip(offset)
            }
            stream
        } catch (e: Exception) {
            Log.e(TAG, "Error opening input stream for $path at offset $offset", e)
            null
        }
    }

    suspend fun disconnect() = withContext(Dispatchers.IO) {
        disconnectInternal()
        _connectionState.value = SmbConnectionState.Disconnected
    }

    private fun disconnectInternal() {
        try {
            openFileCache.values.forEach { runCatching { it.close() } }
            openFileCache.clear()
            runCatching { currentDiskShare?.close() }
            runCatching { currentSession?.close() }
            runCatching { currentConnection?.close() }
        } catch (e: Exception) {
            Log.w(TAG, "Error closing SMB connection components", e)
        } finally {
            currentDiskShare = null
            currentSession = null
            currentConnection = null
        }
    }

    companion object {
        private const val TAG = "SmbManager"
    }
}
