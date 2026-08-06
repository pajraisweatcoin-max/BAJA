package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.MediaEntity
import com.example.data.local.SecureStorage
import com.example.data.model.MediaItem
import com.example.data.model.ServerConfig
import com.example.data.remote.BarraApiService
import com.example.data.remote.SftpManager
import com.example.util.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class MediaGroup(
    val dateHeader: String,
    val items: List<MediaItem>
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val mediaDao = db.mediaDao()
    private val secureStorage = SecureStorage(application)
    val apiService = BarraApiService(secureStorage)
    private val sftpManager = SftpManager(application)

    val serverConfig: StateFlow<ServerConfig> = MutableStateFlow(secureStorage.getConfig()).asStateFlow()

    // Cached media flows from Room
    val allMedia: StateFlow<List<MediaItem>> = mediaDao.getAllMedia()
        .map { list -> list.map { it.toMediaItem() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val photoMedia: StateFlow<List<MediaItem>> = mediaDao.getAllPhotos()
        .map { list -> list.map { it.toMediaItem() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val videoMedia: StateFlow<List<MediaItem>> = mediaDao.getAllVideos()
        .map { list -> list.map { it.toMediaItem() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Grouped media timeline for Tab 1 (Home)
    val timelineGroups: StateFlow<List<MediaGroup>> = allMedia
        .map { list -> groupMediaByDate(list) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // File Manager Tab state
    private val _currentPath = MutableStateFlow("/")
    val currentPath: StateFlow<String> = _currentPath.asStateFlow()

    private val _fileManagerItems = MutableStateFlow<List<MediaItem>>(emptyList())
    val fileManagerItems: StateFlow<List<MediaItem>> = _fileManagerItems.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSftpConnected = MutableStateFlow(false)
    val isSftpConnected: StateFlow<Boolean> = _isSftpConnected.asStateFlow()

    // Fullscreen viewer state
    private val _selectedMedia = MutableStateFlow<MediaItem?>(null)
    val selectedMedia: StateFlow<MediaItem?> = _selectedMedia.asStateFlow()

    // Clipboard for copy/paste operations in SFTP mode
    private val _clipboardItem = MutableStateFlow<MediaItem?>(null)
    val clipboardItem: StateFlow<MediaItem?> = _clipboardItem.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val intentManager = com.example.data.remote.TailscaleIntentManager(application)

    init {
        startInitialConnectionSequence()
    }

    fun startInitialConnectionSequence() {
        viewModelScope.launch {
            _isLoading.value = true
            val config = secureStorage.getConfig()

            // 1. Dahulukan proses mengaktifkan VPN
            if (config.enableTailscale && config.autoConnectVpn) {
                AppLogger.i("CONNECTIVITY", "Proses 1: Mengaktifkan VPN Tailscale")
                if (intentManager.isTailscaleInstalled()) {
                    intentManager.sendConnectVpn()
                    kotlinx.coroutines.delay(600)
                }
            }

            // 2. Lalu proses konektifitas SSH / SFTP
            if (config.enableSftp && config.autoConnectSftp) {
                AppLogger.i("CONNECTIVITY", "Proses 2: Menghubungkan SSH / SFTP")
                val connRes = sftpManager.testConnection(config)
                _isSftpConnected.value = connRes.isSuccess
                kotlinx.coroutines.delay(300)
            }

            // 3. Lalu proses konektifitas HTTP
            AppLogger.i("CONNECTIVITY", "Proses 3: Menghubungkan HTTP")
            val res = apiService.listAllMediaRecursively(config.httpUrl, "/")
            if (res.isSuccess) {
                val items = res.getOrDefault(emptyList())
                val entities = items.map { MediaEntity.fromMediaItem(it) }
                mediaDao.clearAll()
                mediaDao.insertAll(entities)
            } else {
                _snackbarMessage.value = res.exceptionOrNull()?.message ?: "Gagal memuat media dari server"
            }

            val currentDir = _currentPath.value
            val httpRes = apiService.listDirectory(config.httpUrl, currentDir)
            val baseItems = httpRes.getOrDefault(emptyList())

            if (config.enableSftp) {
                val sftpRes = sftpManager.listDirectory(config, currentDir, baseItems)
                if (sftpRes.isSuccess) {
                    _isSftpConnected.value = true
                    _fileManagerItems.value = sftpRes.getOrDefault(baseItems)
                } else {
                    _fileManagerItems.value = baseItems
                }
            } else {
                _fileManagerItems.value = baseItems
            }

            _isLoading.value = false
        }
    }

    fun checkSftpConnection() {
        viewModelScope.launch {
            val config = secureStorage.getConfig()
            if (!config.enableSftp) {
                _isSftpConnected.value = false
                return@launch
            }
            val res = sftpManager.testConnection(config)
            _isSftpConnected.value = res.isSuccess
        }
    }

    fun refreshAllMedia() {
        viewModelScope.launch {
            val config = secureStorage.getConfig()
            _isLoading.value = true
            val res = apiService.listAllMediaRecursively(config.httpUrl, "/")
            if (res.isSuccess) {
                val items = res.getOrDefault(emptyList())
                val entities = items.map { MediaEntity.fromMediaItem(it) }
                mediaDao.clearAll()
                mediaDao.insertAll(entities)
            } else {
                _snackbarMessage.value = res.exceptionOrNull()?.message ?: "Gagal memuat media dari server"
            }
            _isLoading.value = false
        }
    }

    fun loadDirectory(path: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _currentPath.value = path
            val config = secureStorage.getConfig()

            if (config.enableSftp && config.autoConnectSftp && !_isSftpConnected.value) {
                val connRes = sftpManager.testConnection(config)
                _isSftpConnected.value = connRes.isSuccess
            }

            // Fetch HTTP directory items first
            val httpRes = apiService.listDirectory(config.httpUrl, path)
            val baseItems = httpRes.getOrDefault(emptyList())

            if (config.enableSftp) {
                val sftpRes = sftpManager.listDirectory(config, path, baseItems)
                if (sftpRes.isSuccess) {
                    _isSftpConnected.value = true
                    _fileManagerItems.value = sftpRes.getOrDefault(baseItems)
                } else {
                    _fileManagerItems.value = baseItems
                }
            } else {
                _fileManagerItems.value = baseItems
            }

            _isLoading.value = false
        }
    }

    fun navigateUpDirectory() {
        val current = _currentPath.value
        if (current == "/") return
        val parent = current.substringBeforeLast("/").ifEmpty { "/" }
        loadDirectory(parent)
    }

    fun uploadFileToCurrentDirectory(fileUri: Uri, fileName: String) {
        viewModelScope.launch {
            val config = secureStorage.getConfig()
            _isLoading.value = true
            com.example.util.AppLogger.i("MAIN_VM", "Proses unggah $fileName dimulai...")

            val res = if (config.enableSftp) {
                sftpManager.uploadFile(config, _currentPath.value, fileUri, fileName)
            } else {
                apiService.uploadFile(config.httpUrl, _currentPath.value, fileUri, fileName, getApplication())
            }

            if (res.isSuccess) {
                val dirPath = _currentPath.value
                val fullPath = if (dirPath == "/") "/$fileName" else "$dirPath/$fileName"
                val mimeType = getApplication<Application>().contentResolver.getType(fileUri) ?: when {
                    fileName.endsWith(".jpg", true) || fileName.endsWith(".jpeg", true) || fileName.endsWith(".png", true) -> "image/jpeg"
                    fileName.endsWith(".mp4", true) || fileName.endsWith(".mkv", true) -> "video/mp4"
                    else -> "application/octet-stream"
                }

                val uploadedItem = MediaItem(
                    name = fileName,
                    path = fullPath,
                    isDir = false,
                    size = 102400L,
                    mtime = System.currentTimeMillis(),
                    mime = mimeType
                )

                mediaDao.insertAll(listOf(MediaEntity.fromMediaItem(uploadedItem)))

                val currentList = _fileManagerItems.value.toMutableList()
                if (!currentList.any { it.path == fullPath }) {
                    currentList.add(0, uploadedItem)
                    _fileManagerItems.value = currentList
                }

                _snackbarMessage.value = "File $fileName berhasil diunggah!"
                loadDirectory(_currentPath.value)
                refreshAllMedia()
            } else {
                _snackbarMessage.value = res.exceptionOrNull()?.message ?: "Gagal mengunggah file"
            }
            _isLoading.value = false
        }
    }

    fun deleteMediaItem(item: MediaItem) {
        viewModelScope.launch {
            val config = secureStorage.getConfig()
            val res = sftpManager.deleteItem(config, item.path)
            if (res.isSuccess) {
                _snackbarMessage.value = "File ${item.name} berhasil dihapus"
                loadDirectory(_currentPath.value)
                refreshAllMedia()
            } else {
                _snackbarMessage.value = res.exceptionOrNull()?.message ?: "Gagal menghapus file ${item.name}"
            }
        }
    }

    fun copyItemToClipboard(item: MediaItem) {
        _clipboardItem.value = item
        _snackbarMessage.value = "${item.name} disalin ke clipboard"
    }

    fun pasteItemFromClipboard() {
        val item = _clipboardItem.value ?: return
        viewModelScope.launch {
            val config = secureStorage.getConfig()
            val res = sftpManager.copyItem(config, item.path, _currentPath.value)
            if (res.isSuccess) {
                _snackbarMessage.value = "${item.name} berhasil ditempel ke ${_currentPath.value}"
                _clipboardItem.value = null
                loadDirectory(_currentPath.value)
            } else {
                _snackbarMessage.value = "Gagal menempel ${item.name}"
            }
        }
    }

    fun moveItem(item: MediaItem, newName: String) {
        viewModelScope.launch {
            val config = secureStorage.getConfig()
            val res = sftpManager.moveItem(config, item.path, _currentPath.value, newName)
            if (res.isSuccess) {
                _snackbarMessage.value = "File diubah menjadi $newName"
                loadDirectory(_currentPath.value)
            } else {
                _snackbarMessage.value = "Gagal mengubah nama file"
            }
        }
    }

    fun selectMedia(item: MediaItem?) {
        _selectedMedia.value = item
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    private fun groupMediaByDate(items: List<MediaItem>): List<MediaGroup> {
        val groups = LinkedHashMap<String, MutableList<MediaItem>>()
        val todayCal = Calendar.getInstance()
        val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

        val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))

        for (item in items) {
            val itemCal = Calendar.getInstance().apply { timeInMillis = item.mtime }
            val header = when {
                isSameDay(itemCal, todayCal) -> "Hari Ini"
                isSameDay(itemCal, yesterdayCal) -> "Kemarin"
                else -> dateFormat.format(Date(item.mtime))
            }

            val list = groups.getOrPut(header) { mutableListOf() }
            list.add(item)
        }

        return groups.map { (header, list) -> MediaGroup(header, list) }
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}
