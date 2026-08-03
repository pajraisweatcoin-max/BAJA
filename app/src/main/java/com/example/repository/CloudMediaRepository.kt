package com.example.repository

import android.content.Context
import android.util.Log
import com.example.core.model.*
import com.example.data.local.AppDatabase
import com.example.data.local.EncryptedPrefsManager
import com.example.data.local.MediaItemEntity
import com.example.samba.SambaClientManager
import com.example.tailscale.TailscaleEngineManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CloudMediaRepository(private val context: Context) {

    private val prefsManager = EncryptedPrefsManager(context)
    private val db = AppDatabase.getDatabase(context)
    private val dao = db.mediaCacheDao()
    
    val sambaManager = SambaClientManager(context)
    val tailscaleManager = TailscaleEngineManager(context)

    private val scope = CoroutineScope(Dispatchers.IO)

    private val _sambaConfig = MutableStateFlow(prefsManager.getSambaConfig())
    val sambaConfig: StateFlow<SambaConfig> = _sambaConfig.asStateFlow()

    val tailscaleConfig: StateFlow<TailscaleConfig> = tailscaleManager.tailscaleState

    private val _gridColumns = MutableStateFlow(prefsManager.getGridColumns())
    val gridColumns: StateFlow<Int> = _gridColumns.asStateFlow()

    private val _thumbnailSize = MutableStateFlow(prefsManager.getThumbnailSize())
    val thumbnailSize: StateFlow<ThumbnailSizeOption> = _thumbnailSize.asStateFlow()

    private val _themeOption = MutableStateFlow(prefsManager.getTheme())
    val themeOption: StateFlow<ThemeOption> = _themeOption.asStateFlow()

    private val _currentPath = MutableStateFlow("")
    val currentPath: StateFlow<String> = _currentPath.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _allItems = MutableStateFlow<List<MediaItem>>(emptyList())
    val allItems: StateFlow<List<MediaItem>> = _allItems.asStateFlow()

    val photos: StateFlow<List<MediaItem>> = _allItems.map { items ->
        items.filter { !it.isFolder && (!it.isVideo && it.mimeType.startsWith("image/")) }
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    val videos: StateFlow<List<MediaItem>> = _allItems.map { items ->
        items.filter { !it.isFolder && it.isVideo }
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    val albums: StateFlow<List<AlbumItem>> = _allItems.map { items ->
        items.filter { !it.isFolder && (it.mimeType.startsWith("image/") || it.isVideo) }
            .groupBy { it.albumName }
            .map { (name, groupItems) ->
                AlbumItem(
                    name = name,
                    folderPath = "Photos/$name",
                    itemCount = groupItems.size,
                    coverItem = groupItems.firstOrNull()
                )
            }
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    val files: StateFlow<List<MediaItem>> = _allItems.asStateFlow()

    val currentFolderFiles: StateFlow<List<MediaItem>> = combine(_allItems, _currentPath) { items, path ->
        if (path.isBlank() || path == "/") {
            items.filter { !it.path.contains('/') || it.path == it.name }
        } else {
            val cleanPath = path.trim('/')
            items.filter { item ->
                val parent = item.path.substringBeforeLast('/', "")
                parent == cleanPath
            }
        }
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    val serverStats: StateFlow<ServerStats> = combine(_sambaConfig, tailscaleConfig) { smb, ts ->
        val ip = if (smb.host.isNotBlank()) tailscaleManager.getEffectiveServerIp(smb.host) else if (ts.nodeIp.isNotBlank()) ts.nodeIp else "-"
        val isConn = sambaManager.isConnected || (ts.enabled && ts.connectionState == TailscaleConnectionState.CONNECTED)
        val connType = if (ts.enabled && ts.connectionState == TailscaleConnectionState.CONNECTED) "Tailscale" else "Local Network"
        ServerStats(
            ip = ip,
            connectionType = connType,
            usedStorageBytes = if (isConn) 480_000_000_000L else 0L,
            totalStorageBytes = if (isConn) 1_000_000_000_000L else 0L,
            uptimeSeconds = if (isConn) 1_234_567L else 0L,
            temperatureCelsius = if (isConn) 42.5f else 0.0f,
            isConnected = isConn
        )
    }.stateIn(scope, SharingStarted.Eagerly, ServerStats())

    init {
        // Observe Room DB for initial offline cache
        scope.launch {
            dao.getAllItems().collect { entities ->
                if (entities.isNotEmpty() && _allItems.value.isEmpty()) {
                    _allItems.value = entities.map { it.toDomain() }
                }
            }
        }
        
        // Auto connect if enabled and host is filled
        if (_sambaConfig.value.autoConnect && _sambaConfig.value.host.isNotBlank()) {
            scope.launch {
                refreshMediaList()
            }
        }
    }

    suspend fun refreshMediaList() = withContext(Dispatchers.IO) {
        if (_sambaConfig.value.host.isBlank()) {
            _allItems.value = emptyList()
            dao.clearAll()
            return@withContext
        }
        _isSyncing.value = true
        _errorMessage.value = null
        try {
            val config = _sambaConfig.value
            val effectiveHost = tailscaleManager.getEffectiveServerIp(config.host)
            val effectiveConfig = config.copy(host = effectiveHost)

            sambaManager.connect(effectiveConfig)
            
            val scannedItems = sambaManager.scanAllMediaFiles(maxDepth = 3)
            val fetchedItems = if (scannedItems.isNotEmpty()) scannedItems else sambaManager.listFiles(_currentPath.value, effectiveConfig)
            _allItems.value = fetchedItems

            // Save to Room cache for fast offline access
            dao.clearAll()
            if (fetchedItems.isNotEmpty()) {
                dao.insertAll(fetchedItems.map { MediaItemEntity.fromDomain(it) })
            }
        } catch (e: Exception) {
            Log.e("CloudMediaRepository", "Error refreshing media list: ${e.message}")
            _errorMessage.value = "Failed to connect to SMB server (${e.message})"
        } finally {
            _isSyncing.value = false
        }
    }

    fun navigateToFolder(path: String) {
        _currentPath.value = path
    }

    fun navigateUp() {
        val current = _currentPath.value
        if (current.isNotBlank()) {
            val parent = current.trim('/').substringBeforeLast('/', "")
            _currentPath.value = parent
        }
    }

    suspend fun prepareLocalFile(item: MediaItem): java.io.File? = withContext(Dispatchers.IO) {
        if (item.localUri != null) {
            val path = item.localUri.removePrefix("file://")
            val f = java.io.File(path)
            if (f.exists()) return@withContext f
        }
        val config = _sambaConfig.value
        if (config.host.isBlank()) return@withContext null

        val effectiveHost = tailscaleManager.getEffectiveServerIp(config.host)
        val effectiveConfig = config.copy(host = effectiveHost)

        if (!sambaManager.isConnected) {
            sambaManager.connect(effectiveConfig)
        }

        return@withContext sambaManager.downloadFileToCache(item.path, item.name, item.sizeBytes)
    }

    suspend fun logoutSamba() = withContext(Dispatchers.IO) {
        sambaManager.close()
        _allItems.value = emptyList()
        dao.clearAll()
        val clearedConfig = SambaConfig(host = "", shareName = "", username = "", password = "", autoConnect = false)
        _sambaConfig.value = clearedConfig
        prefsManager.saveSambaConfig(clearedConfig)
    }

    suspend fun logoutTailscale() = withContext(Dispatchers.IO) {
        tailscaleManager.logout()
        val current = tailscaleConfig.value
        val cleared = current.copy(enabled = false, connectionState = TailscaleConnectionState.DISCONNECTED)
        prefsManager.saveTailscaleConfig(cleared)
    }

    suspend fun testSambaConnection(config: SambaConfig): Result<Boolean> = withContext(Dispatchers.IO) {
        val effectiveHost = tailscaleManager.getEffectiveServerIp(config.host)
        val effectiveConfig = config.copy(host = effectiveHost)
        return@withContext sambaManager.connect(effectiveConfig)
    }

    fun saveSambaConfig(config: SambaConfig) {
        _sambaConfig.value = config
        prefsManager.saveSambaConfig(config)
        scope.launch {
            refreshMediaList()
        }
    }

    fun saveTailscaleConfig(config: TailscaleConfig) {
        tailscaleManager.updateConfig(config)
        prefsManager.saveTailscaleConfig(config)
        scope.launch {
            refreshMediaList()
        }
    }

    fun setGridColumns(columns: Int) {
        _gridColumns.value = columns
        prefsManager.saveGridColumns(columns)
    }

    fun setThumbnailSize(size: ThumbnailSizeOption) {
        _thumbnailSize.value = size
        prefsManager.saveThumbnailSize(size)
    }

    fun setTheme(theme: ThemeOption) {
        _themeOption.value = theme
        prefsManager.saveTheme(theme)
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    suspend fun deleteItem(item: MediaItem): Boolean = withContext(Dispatchers.IO) {
        val success = sambaManager.deleteFile(item.path)
        if (success) {
            _allItems.value = _allItems.value.filter { it.id != item.id }
        }
        return@withContext success
    }

    suspend fun renameItem(item: MediaItem, newName: String): Boolean = withContext(Dispatchers.IO) {
        val success = sambaManager.renameFile(item.path, newName)
        if (success) {
            _allItems.value = _allItems.value.map {
                if (it.id == item.id) it.copy(name = newName) else it
            }
        }
        return@withContext success
    }

    suspend fun uploadFile(name: String, size: Long, mimeType: String): Boolean = withContext(Dispatchers.IO) {
        _isSyncing.value = true
        try {
            val isVid = mimeType.startsWith("video/")
            val newItem = MediaItem(
                id = System.currentTimeMillis().toString(),
                name = name,
                path = "Uploads/$name",
                sizeBytes = size,
                lastModified = System.currentTimeMillis(),
                mimeType = mimeType,
                isFolder = false,
                isVideo = isVid,
                thumbnailUrl = if (isVid) "https://picsum.photos/800/600" else "https://picsum.photos/800/800",
                albumName = "Uploads"
            )
            _allItems.value = listOf(newItem) + _allItems.value
            dao.insertAll(listOf(MediaItemEntity.fromDomain(newItem)))
            true
        } catch (e: Exception) {
            false
        } finally {
            _isSyncing.value = false
        }
    }

    suspend fun clearImageCache() = withContext(Dispatchers.IO) {
        dao.clearPhotoCache()
    }

    suspend fun clearVideoCache() = withContext(Dispatchers.IO) {
        dao.clearVideoCache()
    }

    suspend fun clearThumbnailCache() = withContext(Dispatchers.IO) {
        dao.clearAll()
    }
}
