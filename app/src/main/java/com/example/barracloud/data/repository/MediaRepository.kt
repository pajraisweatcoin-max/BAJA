package com.example.barracloud.data.repository

import android.content.Context
import android.util.Log
import com.example.barracloud.data.local.AppDatabase
import com.example.barracloud.data.local.EncryptedCredentialsManager
import com.example.barracloud.data.local.FavoriteEntity
import com.example.barracloud.data.local.RecentEntity
import com.example.barracloud.data.local.SettingsRepository
import com.example.barracloud.data.models.MediaItem
import com.example.barracloud.data.models.MediaType
import com.example.barracloud.data.models.SmbCredentials
import com.example.barracloud.smb.SmbConnectionManager
import com.example.barracloud.smb.SmbConnectionState
import com.example.barracloud.smb.SmbProxyServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MediaRepository(private val context: Context) {

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val database = AppDatabase.getInstance(context)
    private val favoriteDao = database.favoriteDao()
    private val recentDao = database.recentDao()

    val credentialsManager = EncryptedCredentialsManager(context)
    val settingsRepository = SettingsRepository(context)
    val smbManager = SmbConnectionManager()
    private var proxyServer: SmbProxyServer? = null

    val connectionState: StateFlow<SmbConnectionState> = smbManager.connectionState

    private val _allMediaItems = MutableStateFlow<List<MediaItem>>(emptyList())
    val allMediaItems: StateFlow<List<MediaItem>> = _allMediaItems.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val favoriteItems: Flow<List<MediaItem>> = favoriteDao.getAllFavorites().map { entities ->
        entities.map { entity ->
            MediaItem(
                id = entity.path,
                name = entity.name,
                path = entity.path,
                parentFolder = entity.path.substringBeforeLast('/', "Root"),
                type = runCatching { MediaType.valueOf(entity.type) }.getOrDefault(MediaType.PHOTO),
                size = entity.size
            )
        }
    }

    val recentItems: Flow<List<RecentEntity>> = recentDao.getAllRecents()

    init {
        runCatching { startProxyServer() }
        // Auto connect if configured
        runCatching {
            val creds = credentialsManager.loadCredentials()
            if (creds.isValid && creds.autoConnect) {
                repositoryScope.launch {
                    runCatching { connectAndFetchMedia(creds) }
                }
            }
        }
    }

    private fun startProxyServer() {
        if (proxyServer != null) return
        val candidatePorts = listOf(8080, 8085, 8888, 9090, 0)
        for (port in candidatePorts) {
            try {
                val server = SmbProxyServer(port = port, smbManager = smbManager)
                server.start()
                proxyServer = server
                Log.d("MediaRepository", "SMB Proxy Server started on port ${server.listeningPort}")
                break
            } catch (t: Throwable) {
                Log.w("MediaRepository", "Failed to start SMB proxy server on port $port, trying next...", t)
            }
        }
    }

    fun getStreamUrl(mediaPath: String): String {
        val server = proxyServer
        return if (server != null) {
            server.getStreamUrl(mediaPath)
        } else {
            "http://127.0.0.1:8080/stream?path=$mediaPath"
        }
    }

    suspend fun connectAndFetchMedia(credentials: SmbCredentials): Result<Boolean> = withContext(Dispatchers.IO) {
        _isLoading.value = true
        _errorMessage.value = null

        val connectResult = smbManager.connect(credentials)
        if (connectResult.isSuccess) {
            if (credentials.rememberLogin) {
                credentialsManager.saveCredentials(credentials)
            }
            refreshMediaFiles()
            _isLoading.value = false
            Result.success(true)
        } else {
            val err = connectResult.exceptionOrNull()?.localizedMessage ?: "Koneksi SMB Gagal"
            _errorMessage.value = err
            _isLoading.value = false
            Result.failure(connectResult.exceptionOrNull() ?: Exception(err))
        }
    }

    suspend fun refreshMediaFiles(folderPath: String = "") = withContext(Dispatchers.IO) {
        _isLoading.value = true
        _errorMessage.value = null
        val result = smbManager.listMediaFiles(folderPath)
        if (result.isSuccess) {
            _allMediaItems.value = result.getOrDefault(emptyList())
        } else {
            _errorMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Gagal membaca berkas media"
        }
        _isLoading.value = false
    }

    fun isFavorite(path: String): Flow<Boolean> = favoriteDao.isFavorite(path)

    suspend fun toggleFavorite(mediaItem: MediaItem) = withContext(Dispatchers.IO) {
        val isFav = favoriteDao.isFavoriteSync(mediaItem.path)
        if (isFav) {
            favoriteDao.deleteFavorite(mediaItem.path)
        } else {
            favoriteDao.insertFavorite(
                FavoriteEntity(
                    path = mediaItem.path,
                    name = mediaItem.name,
                    type = mediaItem.type.name,
                    size = mediaItem.size,
                    mimeType = mediaItem.mimeType
                )
            )
        }
    }

    suspend fun addRecent(mediaItem: MediaItem, playbackPositionMs: Long = 0L) = withContext(Dispatchers.IO) {
        recentDao.insertOrUpdateRecent(
            RecentEntity(
                path = mediaItem.path,
                name = mediaItem.name,
                type = mediaItem.type.name,
                size = mediaItem.size,
                playbackPositionMs = playbackPositionMs,
                lastOpenedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun updatePlaybackPosition(path: String, positionMs: Long) = withContext(Dispatchers.IO) {
        recentDao.updatePlaybackPosition(path, positionMs)
    }

    suspend fun getRecentPosition(path: String): Long = withContext(Dispatchers.IO) {
        recentDao.getRecentByPath(path)?.playbackPositionMs ?: 0L
    }

    suspend fun disconnect() = withContext(Dispatchers.IO) {
        smbManager.disconnect()
        _allMediaItems.value = emptyList()
    }
}
