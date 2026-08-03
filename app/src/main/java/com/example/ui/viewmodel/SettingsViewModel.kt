package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.SecureStorage
import com.example.data.model.ConnectionState
import com.example.data.model.ConnectionStatus
import com.example.data.model.ServerConfig
import com.example.data.remote.BarraApiService
import com.example.data.remote.SambaManager
import com.example.data.remote.TailscaleManager
import com.example.data.remote.ThumbnailCacheManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val secureStorage = SecureStorage(application)
    private val apiService = BarraApiService(secureStorage)
    private val sambaManager = SambaManager(application)
    private val tailscaleManager = TailscaleManager()
    private val cacheManager = ThumbnailCacheManager(application)

    private val _config = MutableStateFlow(secureStorage.getConfig())
    val config: StateFlow<ServerConfig> = _config.asStateFlow()

    private val _connectionStatus = MutableStateFlow(ConnectionStatus())
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _cacheSize = MutableStateFlow("Sedang menghitung...")
    val cacheSize: StateFlow<String> = _cacheSize.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    init {
        refreshCacheSize()
    }

    fun updateConfig(newConfig: ServerConfig) {
        _config.value = newConfig
        secureStorage.saveConfig(newConfig)
    }

    fun saveAndAuthenticate() {
        viewModelScope.launch {
            val current = _config.value
            _connectionStatus.update { it.copy(httpState = ConnectionState.CONNECTING, httpMessage = "Menghubungkan...") }

            val loginResult = apiService.login(current.httpUrl, current.adminPassword)
            if (loginResult.isSuccess) {
                _connectionStatus.update {
                    it.copy(
                        httpState = ConnectionState.CONNECTED,
                        httpMessage = "Berhasil Autentikasi HTTP & Menyimpan Cookie Auth"
                    )
                }
                _userMessage.value = "Pengaturan berhasil disimpan & terautentikasi"
            } else {
                val err = loginResult.exceptionOrNull()?.message ?: "Gagal terhubung ke HTTP API"
                _connectionStatus.update {
                    it.copy(
                        httpState = ConnectionState.ERROR,
                        httpMessage = err
                    )
                }
                _userMessage.value = err
            }

            // Also test Tailscale if enabled
            if (current.useTailscale) {
                tailscaleManager.updateState(current)
            }
        }
    }

    fun testHttpConnection() {
        viewModelScope.launch {
            val current = _config.value
            _connectionStatus.update { it.copy(httpState = ConnectionState.CONNECTING, httpMessage = "Menguji HTTP...") }

            val res = apiService.login(current.httpUrl, current.adminPassword)
            if (res.isSuccess) {
                _connectionStatus.update {
                    it.copy(
                        httpState = ConnectionState.CONNECTED,
                        httpMessage = "Terhubung ke BARRA CLOUD HTTP API (200 OK)"
                    )
                }
            } else {
                _connectionStatus.update {
                    it.copy(
                        httpState = ConnectionState.ERROR,
                        httpMessage = res.exceptionOrNull()?.message ?: "Koneksi HTTP Gagal"
                    )
                }
            }
        }
    }

    fun testSambaConnection() {
        viewModelScope.launch {
            val current = _config.value
            _connectionStatus.update { it.copy(sambaState = ConnectionState.CONNECTING, sambaMessage = "Menguji Samba...") }

            val res = sambaManager.testConnection(current)
            if (res.isSuccess) {
                _connectionStatus.update {
                    it.copy(
                        sambaState = ConnectionState.CONNECTED,
                        sambaMessage = res.getOrDefault("Koneksi Samba Berhasil")
                    )
                }
            } else {
                _connectionStatus.update {
                    it.copy(
                        sambaState = ConnectionState.ERROR,
                        sambaMessage = res.exceptionOrNull()?.message ?: "Koneksi Samba Gagal"
                    )
                }
            }
        }
    }

    fun refreshCacheSize() {
        viewModelScope.launch {
            _cacheSize.value = cacheManager.getFormattedCacheSize()
        }
    }

    fun clearThumbnailCache() {
        viewModelScope.launch {
            val success = cacheManager.clearCache()
            if (success) {
                _userMessage.value = "Cache thumbnail lokal berhasil dibersihkan"
            } else {
                _userMessage.value = "Gagal membersihkan cache thumbnail"
            }
            refreshCacheSize()
        }
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }
}
