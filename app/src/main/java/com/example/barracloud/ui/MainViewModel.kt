package com.example.barracloud.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.barracloud.data.local.AppThemeMode
import com.example.barracloud.data.local.RecentEntity
import com.example.barracloud.data.models.MediaItem
import com.example.barracloud.data.models.MediaType
import com.example.barracloud.data.models.SmbCredentials
import com.example.barracloud.data.repository.MediaRepository
import com.example.barracloud.smb.SmbConnectionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class MediaCategoryFilter {
    ALL,
    PHOTOS,
    VIDEOS
}

class MainViewModel(val repository: MediaRepository) : ViewModel() {

    val connectionState: StateFlow<SmbConnectionState> = repository.connectionState
    val isLoading: StateFlow<Boolean> = repository.isLoading
    val errorMessage: StateFlow<String?> = repository.errorMessage

    val settings = repository.settingsRepository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = repository.settingsRepository.settings.value
    )

    private val _credentials = MutableStateFlow(repository.credentialsManager.loadCredentials())
    val credentials: StateFlow<SmbCredentials> = _credentials.asStateFlow()

    private val _selectedFilter = MutableStateFlow(MediaCategoryFilter.ALL)
    val selectedFilter: StateFlow<MediaCategoryFilter> = _selectedFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val allMediaItems: StateFlow<List<MediaItem>> = repository.allMediaItems

    val filteredMediaItems: StateFlow<List<MediaItem>> = combine(
        allMediaItems,
        selectedFilter,
        searchQuery
    ) { items, filter, query ->
        items.filter { item ->
            val matchesFilter = when (filter) {
                MediaCategoryFilter.ALL -> true
                MediaCategoryFilter.PHOTOS -> item.type == MediaType.PHOTO || item.type == MediaType.RAW
                MediaCategoryFilter.VIDEOS -> item.type == MediaType.VIDEO
            }
            val matchesQuery = if (query.isBlank()) {
                true
            } else {
                item.name.contains(query, ignoreCase = true) ||
                        item.extension.contains(query, ignoreCase = true) ||
                        item.parentFolder.contains(query, ignoreCase = true)
            }
            matchesFilter && matchesQuery
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val favoriteMediaItems: StateFlow<List<MediaItem>> = repository.favoriteItems.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val recentEntities: StateFlow<List<RecentEntity>> = repository.recentItems.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun updateCredentials(newCreds: SmbCredentials) {
        _credentials.value = newCreds
    }

    fun connectSmb(creds: SmbCredentials = _credentials.value) {
        viewModelScope.launch {
            repository.connectAndFetchMedia(creds)
        }
    }

    fun disconnectSmb() {
        viewModelScope.launch {
            repository.disconnect()
        }
    }

    fun setFilter(filter: MediaCategoryFilter) {
        _selectedFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun refreshMedia() {
        viewModelScope.launch {
            repository.refreshMediaFiles()
        }
    }

    fun toggleFavorite(item: MediaItem) {
        viewModelScope.launch {
            repository.toggleFavorite(item)
        }
    }

    fun isFavorite(itemPath: String) = repository.isFavorite(itemPath)

    fun addRecent(item: MediaItem, playbackPositionMs: Long = 0L) {
        viewModelScope.launch {
            repository.addRecent(item, playbackPositionMs)
        }
    }

    fun updatePlaybackPosition(itemPath: String, positionMs: Long) {
        viewModelScope.launch {
            repository.updatePlaybackPosition(itemPath, positionMs)
        }
    }

    suspend fun getRecentPosition(itemPath: String): Long {
        return repository.getRecentPosition(itemPath)
    }

    fun getStreamUrl(mediaPath: String): String {
        return repository.getStreamUrl(mediaPath)
    }

    fun updateTheme(mode: AppThemeMode) {
        repository.settingsRepository.updateThemeMode(mode)
    }

    fun updateGridColumns(cols: Int) {
        repository.settingsRepository.updateGridColumns(cols)
    }

    class Factory(private val repository: MediaRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(repository) as T
        }
    }
}
