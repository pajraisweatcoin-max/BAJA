package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.MediaEntity
import com.example.data.local.SecureStorage
import com.example.data.model.MediaItem
import com.example.data.model.ServerConfig
import com.example.data.remote.BarraApiService
import com.example.data.remote.SambaManager
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
    private val sambaManager = SambaManager(application)

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

    private val _isSambaConnected = MutableStateFlow(false)
    val isSambaConnected: StateFlow<Boolean> = _isSambaConnected.asStateFlow()

    // Fullscreen viewer state
    private val _selectedMedia = MutableStateFlow<MediaItem?>(null)
    val selectedMedia: StateFlow<MediaItem?> = _selectedMedia.asStateFlow()

    // Clipboard for copy/paste operations in Samba mode
    private val _clipboardItem = MutableStateFlow<MediaItem?>(null)
    val clipboardItem: StateFlow<MediaItem?> = _clipboardItem.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    init {
        refreshAllMedia()
        loadDirectory("/")
        checkSambaConnection()
    }

    fun checkSambaConnection() {
        viewModelScope.launch {
            val config = secureStorage.getConfig()
            if (!config.enableSamba) {
                _isSambaConnected.value = false
                return@launch
            }
            val res = sambaManager.testConnection(config)
            _isSambaConnected.value = res.isSuccess
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

            // Fetch HTTP directory items first
            val httpRes = apiService.listDirectory(config.httpUrl, path)
            val baseItems = httpRes.getOrDefault(emptyList())

            // If Samba is enabled/connected, enrich directory listing with Samba (including .thumbs)
            if (config.enableSamba) {
                val smbRes = sambaManager.listSmbDirectory(config, path, baseItems)
                _fileManagerItems.value = smbRes.getOrDefault(baseItems)
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
            val res = sambaManager.uploadFile(config, _currentPath.value, fileUri, fileName)
            if (res.isSuccess) {
                _snackbarMessage.value = "File $fileName berhasil diunggah via Samba"
                loadDirectory(_currentPath.value)
            } else {
                _snackbarMessage.value = res.exceptionOrNull()?.message ?: "Gagal mengunggah file"
            }
            _isLoading.value = false
        }
    }

    fun deleteMediaItem(item: MediaItem) {
        viewModelScope.launch {
            val config = secureStorage.getConfig()
            val res = sambaManager.deleteItem(config, item.path)
            if (res.isSuccess) {
                _snackbarMessage.value = "File ${item.name} berhasil dihapus"
                loadDirectory(_currentPath.value)
                refreshAllMedia()
            } else {
                _snackbarMessage.value = "Gagal menghapus file ${item.name}"
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
            val res = sambaManager.copyItem(config, item.path, _currentPath.value)
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
            val res = sambaManager.moveItem(config, item.path, _currentPath.value, newName)
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
