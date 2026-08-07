package com.example.viewmodel

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.db.SshServerEntity
import com.example.data.sftp.ClipboardItem
import com.example.data.sftp.ConnectionState
import com.example.data.sftp.SftpFileItem
import com.example.data.sftp.SftpSessionManager
import com.example.data.sftp.TransferTask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

enum class SortType {
    NAME_ASC,
    NAME_DESC,
    SIZE_ASC,
    SIZE_DESC,
    DATE_NEWEST,
    DATE_OLDEST
}

class SftpViewModel(private val db: AppDatabase) : ViewModel() {

    val sessionManager = SftpSessionManager.getInstance()

    val servers: StateFlow<List<SshServerEntity>> = db.sshServerDao().getAllServers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val connectionState: StateFlow<ConnectionState> = sessionManager.connectionState
    val activeServer: StateFlow<SshServerEntity?> = sessionManager.activeServer
    val currentPath: StateFlow<String> = sessionManager.currentPath

    private val _fileList = MutableStateFlow<List<SftpFileItem>>(emptyList())
    val fileList: StateFlow<List<SftpFileItem>> = _fileList.asStateFlow()

    private val _isLoadingFiles = MutableStateFlow(false)
    val isLoadingFiles: StateFlow<Boolean> = _isLoadingFiles.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortType = MutableStateFlow(SortType.NAME_ASC)
    val sortType: StateFlow<SortType> = _sortType.asStateFlow()

    val filteredFileList: StateFlow<List<SftpFileItem>> = combine(_fileList, _searchQuery, _sortType) { files, query, sort ->
        val list = if (query.isNotBlank()) {
            files.filter { it.name.contains(query, ignoreCase = true) }
        } else {
            files
        }

        val (dirs, nonDirs) = list.partition { it.isDirectory }

        val sortedDirs = when (sort) {
            SortType.NAME_ASC -> dirs.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
            SortType.NAME_DESC -> dirs.sortedWith(compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.name })
            SortType.DATE_NEWEST -> dirs.sortedByDescending { it.mtime }
            SortType.DATE_OLDEST -> dirs.sortedBy { it.mtime }
            else -> dirs.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
        }

        val sortedNonDirs = when (sort) {
            SortType.NAME_ASC -> nonDirs.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
            SortType.NAME_DESC -> nonDirs.sortedWith(compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.name })
            SortType.SIZE_ASC -> nonDirs.sortedBy { it.size }
            SortType.SIZE_DESC -> nonDirs.sortedByDescending { it.size }
            SortType.DATE_NEWEST -> nonDirs.sortedByDescending { it.mtime }
            SortType.DATE_OLDEST -> nonDirs.sortedBy { it.mtime }
        }

        sortedDirs + sortedNonDirs
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _clipboardItem = MutableStateFlow<ClipboardItem?>(null)
    val clipboardItem: StateFlow<ClipboardItem?> = _clipboardItem.asStateFlow()

    private val _selectedItems = MutableStateFlow<Set<SftpFileItem>>(emptySet())
    val selectedItems: StateFlow<Set<SftpFileItem>> = _selectedItems.asStateFlow()

    private val _editingTextFile = MutableStateFlow<Pair<SftpFileItem, String>?>(null)
    val editingTextFile: StateFlow<Pair<SftpFileItem, String>?> = _editingTextFile.asStateFlow()

    private val _isLoadingTextFile = MutableStateFlow(false)
    val isLoadingTextFile: StateFlow<Boolean> = _isLoadingTextFile.asStateFlow()

    private val _transferTasks = MutableStateFlow<List<TransferTask>>(emptyList())
    val transferTasks: StateFlow<List<TransferTask>> = _transferTasks.asStateFlow()

    private val _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage: StateFlow<String?> = _uiMessage.asStateFlow()

    fun clearUiMessage() {
        _uiMessage.value = null
    }

    fun showMessage(msg: String) {
        _uiMessage.value = msg
    }

    // --- Server Profile Management ---
    fun saveServer(server: SshServerEntity) {
        viewModelScope.launch {
            try {
                if (server.id == 0L) {
                    db.sshServerDao().insertServer(server)
                    _uiMessage.value = "Profil server berhasil ditambahkan"
                } else {
                    db.sshServerDao().updateServer(server)
                    _uiMessage.value = "Profil server berhasil diperbarui"
                }
            } catch (e: Exception) {
                _uiMessage.value = "Gagal menyimpan profil server: ${e.localizedMessage}"
            }
        }
    }

    fun deleteServer(server: SshServerEntity) {
        viewModelScope.launch {
            try {
                db.sshServerDao().deleteServer(server)
                _uiMessage.value = "Profil server berhasil dihapus"
            } catch (e: Exception) {
                _uiMessage.value = "Gagal menghapus profil server: ${e.localizedMessage}"
            }
        }
    }

    // --- Connect / Disconnect ---
    fun connectToServer(server: SshServerEntity) {
        viewModelScope.launch {
            _selectedItems.value = emptySet()
            _fileList.value = emptyList()
            val result = sessionManager.connect(server)
            if (result.isSuccess) {
                try {
                    db.sshServerDao().updateLastConnected(server.id, System.currentTimeMillis())
                } catch (_: Exception) {}
                _uiMessage.value = "Terhubung ke ${server.name} (${server.host})"
                refreshFiles()
            } else {
                val err = result.exceptionOrNull()?.message ?: "Gagal terhubung"
                _uiMessage.value = "Error: $err"
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            sessionManager.disconnect()
            _fileList.value = emptyList()
            _selectedItems.value = emptySet()
            _clipboardItem.value = null
            _editingTextFile.value = null
            _uiMessage.value = "Sesi SSH telah terputus"
        }
    }

    // --- File Browsing ---
    fun refreshFiles() {
        viewModelScope.launch {
            if (connectionState.value != ConnectionState.CONNECTED) return@launch
            _isLoadingFiles.value = true
            val res = sessionManager.listFiles()
            _isLoadingFiles.value = false
            if (res.isSuccess) {
                _fileList.value = res.getOrDefault(emptyList())
            } else {
                _uiMessage.value = res.exceptionOrNull()?.message ?: "Gagal memuat berkas"
            }
        }
    }

    fun navigateTo(path: String) {
        viewModelScope.launch {
            if (connectionState.value != ConnectionState.CONNECTED) return@launch
            _isLoadingFiles.value = true
            _selectedItems.value = emptySet()
            val res = sessionManager.changeDirectory(path)
            _isLoadingFiles.value = false
            if (res.isSuccess) {
                _fileList.value = res.getOrDefault(emptyList())
            } else {
                _uiMessage.value = res.exceptionOrNull()?.message ?: "Gagal membuka folder"
            }
        }
    }

    fun navigateUp() {
        val current = currentPath.value
        if (current == "/" || current.isEmpty()) return
        val parent = current.substringBeforeLast('/', "/").ifEmpty { "/" }
        navigateTo(parent)
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortType(type: SortType) {
        _sortType.value = type
    }

    // --- Selection ---
    fun toggleSelection(item: SftpFileItem) {
        val current = _selectedItems.value.toMutableSet()
        if (current.contains(item)) {
            current.remove(item)
        } else {
            current.add(item)
        }
        _selectedItems.value = current
    }

    fun clearSelection() {
        _selectedItems.value = emptySet()
    }

    // --- Clipboard (Copy / Move) ---
    fun copyToClipboard(item: SftpFileItem, isCutMode: Boolean) {
        _clipboardItem.value = ClipboardItem(
            sourcePath = item.path,
            fileName = item.name,
            isDirectory = item.isDirectory,
            isCutMode = isCutMode
        )
        val modeStr = if (isCutMode) "dipotong (pindah)" else "disalin"
        _uiMessage.value = "${item.name} $modeStr. Buka folder tujuan lalu tekan 'Tempel'."
    }

    fun pasteFromClipboard() {
        val clip = _clipboardItem.value ?: return
        val targetDir = currentPath.value

        viewModelScope.launch {
            _isLoadingFiles.value = true
            val res = if (clip.isCutMode) {
                sessionManager.moveItem(clip.sourcePath, targetDir, clip.fileName)
            } else {
                sessionManager.copyItem(clip.sourcePath, targetDir, clip.fileName, clip.isDirectory)
            }
            _isLoadingFiles.value = false

            if (res.isSuccess) {
                val modeStr = if (clip.isCutMode) "dipindahkan" else "disalin"
                _uiMessage.value = "${clip.fileName} berhasil $modeStr"
                if (clip.isCutMode) {
                    _clipboardItem.value = null
                }
                refreshFiles()
            } else {
                _uiMessage.value = res.exceptionOrNull()?.message ?: "Gagal menempelkan berkas"
            }
        }
    }

    fun clearClipboard() {
        _clipboardItem.value = null
    }

    // --- Operations: Rename, Delete, Create ---
    fun renameItem(item: SftpFileItem, newName: String) {
        if (newName.isBlank() || newName == item.name) return
        viewModelScope.launch {
            _isLoadingFiles.value = true
            val res = sessionManager.renameItem(item.path, newName)
            _isLoadingFiles.value = false
            if (res.isSuccess) {
                _uiMessage.value = "Nama berkas berhasil diubah"
                refreshFiles()
            } else {
                _uiMessage.value = res.exceptionOrNull()?.message ?: "Gagal mengubah nama"
            }
        }
    }

    fun deleteItem(item: SftpFileItem) {
        viewModelScope.launch {
            _isLoadingFiles.value = true
            val res = sessionManager.deleteItem(item.path, item.isDirectory)
            _isLoadingFiles.value = false
            if (res.isSuccess) {
                _selectedItems.value = _selectedItems.value - item
                _uiMessage.value = "${item.name} telah dihapus"
                refreshFiles()
            } else {
                _uiMessage.value = res.exceptionOrNull()?.message ?: "Gagal menghapus berkas"
            }
        }
    }

    fun deleteSelectedItems() {
        val selected = _selectedItems.value.toList()
        if (selected.isEmpty()) return
        viewModelScope.launch {
            _isLoadingFiles.value = true
            var count = 0
            for (item in selected) {
                val res = sessionManager.deleteItem(item.path, item.isDirectory)
                if (res.isSuccess) count++
            }
            _isLoadingFiles.value = false
            _selectedItems.value = emptySet()
            _uiMessage.value = "$count dari ${selected.size} item berhasil dihapus"
            refreshFiles()
        }
    }

    fun createFolder(folderName: String) {
        if (folderName.isBlank()) return
        viewModelScope.launch {
            _isLoadingFiles.value = true
            val res = sessionManager.createFolder(currentPath.value, folderName.trim())
            _isLoadingFiles.value = false
            if (res.isSuccess) {
                _uiMessage.value = "Folder '${folderName}' berhasil dibuat"
                refreshFiles()
            } else {
                _uiMessage.value = res.exceptionOrNull()?.message ?: "Gagal membuat folder"
            }
        }
    }

    fun createFile(fileName: String, initialContent: String = "") {
        if (fileName.isBlank()) return
        viewModelScope.launch {
            _isLoadingFiles.value = true
            val res = sessionManager.createFile(currentPath.value, fileName.trim(), initialContent)
            _isLoadingFiles.value = false
            if (res.isSuccess) {
                _uiMessage.value = "Berkas '${fileName}' berhasil dibuat"
                refreshFiles()
            } else {
                _uiMessage.value = res.exceptionOrNull()?.message ?: "Gagal membuat berkas"
            }
        }
    }

    // --- Upload File ---
    fun uploadFileFromUri(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val contentResolver = context.contentResolver
            var fileName = "upload_file"
            var fileSize: Long = 0

            try {
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (nameIndex != -1) {
                            val queriedName = cursor.getString(nameIndex)
                            if (!queriedName.isNullOrBlank()) fileName = queriedName
                        }
                        if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
                    }
                }
            } catch (_: Exception) {}

            val inputStream: InputStream? = try {
                contentResolver.openInputStream(uri)
            } catch (e: Exception) {
                _uiMessage.value = "Gagal membuka berkas lokal: ${e.localizedMessage}"
                return@launch
            }

            if (inputStream == null) {
                _uiMessage.value = "Berkas lokal tidak ditemukan"
                return@launch
            }

            val taskId = UUID.randomUUID().toString()
            val task = TransferTask(
                id = taskId,
                fileName = fileName,
                isUpload = true,
                totalBytes = fileSize
            )

            _transferTasks.value = _transferTasks.value + task
            _uiMessage.value = "Memulai pengunggahan ${fileName}..."

            val targetDir = currentPath.value
            val res = sessionManager.uploadFile(
                inputStream = inputStream,
                remoteFileName = fileName,
                targetDir = targetDir,
                totalBytes = fileSize,
                onProgress = { transferred ->
                    updateTaskProgress(taskId, transferred)
                }
            )

            if (res.isSuccess) {
                markTaskCompleted(taskId)
                _uiMessage.value = "Unggah ${fileName} selesai!"
                refreshFiles()
            } else {
                markTaskError(taskId, res.exceptionOrNull()?.message ?: "Gagal unggah")
                _uiMessage.value = "Gagal mengunggah ${fileName}"
            }
        }
    }

    // --- Download File ---
    fun downloadFileToLocal(context: Context, item: SftpFileItem, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val contentResolver = context.contentResolver
            val outputStream: OutputStream? = try {
                contentResolver.openOutputStream(uri, "wt")
            } catch (e: Exception) {
                _uiMessage.value = "Gagal membuka file penyimpanan lokal: ${e.localizedMessage}"
                return@launch
            }

            if (outputStream == null) {
                _uiMessage.value = "Tidak dapat menyimpan file lokal"
                return@launch
            }

            val taskId = UUID.randomUUID().toString()
            val task = TransferTask(
                id = taskId,
                fileName = item.name,
                isUpload = false,
                totalBytes = item.size
            )

            _transferTasks.value = _transferTasks.value + task
            _uiMessage.value = "Memulai pengunduhan ${item.name}..."

            val res = sessionManager.downloadFile(
                remotePath = item.path,
                outputStream = outputStream,
                onProgress = { transferred ->
                    updateTaskProgress(taskId, transferred)
                }
            )

            if (res.isSuccess) {
                markTaskCompleted(taskId)
                _uiMessage.value = "Unduh ${item.name} selesai!"
            } else {
                markTaskError(taskId, res.exceptionOrNull()?.message ?: "Gagal unduh")
                _uiMessage.value = "Gagal mengunduh ${item.name}"
            }
        }
    }

    private fun updateTaskProgress(taskId: String, transferred: Long) {
        val currentTasks = _transferTasks.value
        val existingTask = currentTasks.find { it.id == taskId }
        if (existingTask != null && existingTask.transferredBytes == transferred) return
        _transferTasks.value = currentTasks.map { task ->
            if (task.id == taskId) {
                task.copy(transferredBytes = transferred)
            } else task
        }
    }

    private fun markTaskCompleted(taskId: String) {
        _transferTasks.value = _transferTasks.value.map { task ->
            if (task.id == taskId) {
                task.copy(isCompleted = true)
            } else task
        }
    }

    private fun markTaskError(taskId: String, err: String) {
        _transferTasks.value = _transferTasks.value.map { task ->
            if (task.id == taskId) {
                task.copy(error = err)
            } else task
        }
    }

    fun clearCompletedTasks() {
        _transferTasks.value = _transferTasks.value.filter { !it.isCompleted && it.error == null }
    }

    // --- Remote Text Editor ---
    fun openTextFile(item: SftpFileItem) {
        viewModelScope.launch {
            _isLoadingTextFile.value = true
            val res = sessionManager.readTextFile(item.path)
            _isLoadingTextFile.value = false
            if (res.isSuccess) {
                val content = res.getOrDefault("")
                _editingTextFile.value = Pair(item, content)
            } else {
                _uiMessage.value = res.exceptionOrNull()?.message ?: "Gagal membaca isi berkas"
            }
        }
    }

    fun saveTextFile(newContent: String) {
        val current = _editingTextFile.value ?: return
        val item = current.first
        viewModelScope.launch {
            _isLoadingTextFile.value = true
            val res = sessionManager.saveTextFile(item.path, newContent)
            _isLoadingTextFile.value = false
            if (res.isSuccess) {
                _editingTextFile.value = Pair(item, newContent)
                _uiMessage.value = "Berkas '${item.name}' berhasil disimpan"
                refreshFiles()
            } else {
                _uiMessage.value = res.exceptionOrNull()?.message ?: "Gagal menyimpan berkas"
            }
        }
    }

    fun closeTextEditor() {
        _editingTextFile.value = null
    }
}

class SftpViewModelFactory(private val db: AppDatabase) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SftpViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SftpViewModel(db) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
