package com.example.settings

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.model.*
import com.example.repository.CloudMediaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    val repository = CloudMediaRepository(application.applicationContext)

    val sambaConfig = repository.sambaConfig
    val tailscaleConfig = repository.tailscaleConfig
    val gridColumns = repository.gridColumns
    val thumbnailSize = repository.thumbnailSize
    val themeOption = repository.themeOption

    private val _isTestingConnection = MutableStateFlow(false)
    val isTestingConnection: StateFlow<Boolean> = _isTestingConnection.asStateFlow()

    private val _testConnectionResult = MutableStateFlow<String?>(null)
    val testConnectionResult: StateFlow<String?> = _testConnectionResult.asStateFlow()

    fun saveSambaConfig(config: SambaConfig) {
        repository.saveSambaConfig(config)
        Toast.makeText(getApplication(), "Samba configuration saved", Toast.LENGTH_SHORT).show()
    }

    fun logoutSamba() {
        viewModelScope.launch {
            repository.logoutSamba()
            Toast.makeText(getApplication(), "Logged out from Samba & cache cleared", Toast.LENGTH_SHORT).show()
        }
    }

    fun testSambaConnection(config: SambaConfig) {
        viewModelScope.launch {
            _isTestingConnection.value = true
            _testConnectionResult.value = null
            val result = repository.testSambaConnection(config)
            _isTestingConnection.value = false
            if (result.isSuccess) {
                _testConnectionResult.value = "SUCCESS: Connected to Samba share '${config.shareName}'!"
            } else {
                _testConnectionResult.value = "FAILED: ${result.exceptionOrNull()?.message ?: "Unknown error"}"
            }
        }
    }

    fun dismissTestDialog() {
        _testConnectionResult.value = null
    }

    fun toggleTailscale(enabled: Boolean) {
        val current = tailscaleConfig.value
        repository.saveTailscaleConfig(current.copy(enabled = enabled))
    }

    fun saveTailscaleConfig(config: TailscaleConfig) {
        repository.saveTailscaleConfig(config)
        Toast.makeText(getApplication(), "Tailscale configuration saved", Toast.LENGTH_SHORT).show()
    }

    fun loginTailscale() {
        repository.tailscaleManager.login()
        Toast.makeText(getApplication(), "Tailscale Engine Authenticating...", Toast.LENGTH_SHORT).show()
    }

    fun logoutTailscale() {
        viewModelScope.launch {
            repository.logoutTailscale()
            Toast.makeText(getApplication(), "Tailscale Disconnected", Toast.LENGTH_SHORT).show()
        }
    }

    fun reconnectTailscale() {
        repository.tailscaleManager.reconnect()
        Toast.makeText(getApplication(), "Reconnecting Tailscale Engine...", Toast.LENGTH_SHORT).show()
    }

    fun setGridColumns(columns: Int) {
        repository.setGridColumns(columns)
    }

    fun setThumbnailSize(size: ThumbnailSizeOption) {
        repository.setThumbnailSize(size)
    }

    fun setTheme(theme: ThemeOption) {
        repository.setTheme(theme)
    }

    fun clearImageCache() {
        viewModelScope.launch {
            repository.clearImageCache()
            Toast.makeText(getApplication(), "Image cache cleared!", Toast.LENGTH_SHORT).show()
        }
    }

    fun clearVideoCache() {
        viewModelScope.launch {
            repository.clearVideoCache()
            Toast.makeText(getApplication(), "Video cache cleared!", Toast.LENGTH_SHORT).show()
        }
    }

    fun clearThumbnailCache() {
        viewModelScope.launch {
            repository.clearThumbnailCache()
            Toast.makeText(getApplication(), "All thumbnail caches cleared!", Toast.LENGTH_SHORT).show()
        }
    }
}
