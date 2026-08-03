package com.example.barracloud.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.barracloud.data.models.MediaItem
import com.example.barracloud.data.models.MediaType
import com.example.barracloud.smb.SmbConnectionState
import com.example.barracloud.ui.screens.FavoritesScreen
import com.example.barracloud.ui.screens.HomeScreen
import com.example.barracloud.ui.screens.LoginScreen
import com.example.barracloud.ui.screens.PhotoViewerScreen
import com.example.barracloud.ui.screens.RecentsScreen
import com.example.barracloud.ui.screens.SearchScreen
import com.example.barracloud.ui.screens.SettingsScreen
import com.example.barracloud.ui.screens.VideoPlayerScreen

enum class ScreenRoute {
    LOGIN,
    HOME,
    FAVORITES,
    RECENTS,
    SEARCH,
    SETTINGS,
    PHOTO_VIEWER,
    VIDEO_PLAYER
}

@Composable
fun MainContainer(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val credentials by viewModel.credentials.collectAsState()
    val filteredMedia by viewModel.filteredMediaItems.collectAsState()
    val favoriteMedia by viewModel.favoriteMediaItems.collectAsState()
    val recentEntities by viewModel.recentEntities.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var currentRoute by remember {
        mutableStateOf(
            if (connectionState is SmbConnectionState.Connected) ScreenRoute.HOME else ScreenRoute.LOGIN
        )
    }

    var selectedMediaItem by remember { mutableStateOf<MediaItem?>(null) }
    var initialPhotoIndex by remember { mutableStateOf(0) }
    var initialVideoPositionMs by remember { mutableStateOf(0L) }

    val isFullScreenViewer = currentRoute == ScreenRoute.PHOTO_VIEWER || currentRoute == ScreenRoute.VIDEO_PLAYER

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            if (!isFullScreenViewer && currentRoute != ScreenRoute.LOGIN) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    NavigationBarItem(
                        selected = currentRoute == ScreenRoute.HOME,
                        onClick = { currentRoute = ScreenRoute.HOME },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Gallery") },
                        modifier = Modifier.testTag("nav_home")
                    )

                    NavigationBarItem(
                        selected = currentRoute == ScreenRoute.FAVORITES,
                        onClick = { currentRoute = ScreenRoute.FAVORITES },
                        icon = { Icon(Icons.Default.Favorite, contentDescription = "Favorites") },
                        label = { Text("Favorit") },
                        modifier = Modifier.testTag("nav_favorites")
                    )

                    NavigationBarItem(
                        selected = currentRoute == ScreenRoute.RECENTS,
                        onClick = { currentRoute = ScreenRoute.RECENTS },
                        icon = { Icon(Icons.Default.History, contentDescription = "Recents") },
                        label = { Text("Riwayat") },
                        modifier = Modifier.testTag("nav_recents")
                    )

                    NavigationBarItem(
                        selected = currentRoute == ScreenRoute.SEARCH,
                        onClick = { currentRoute = ScreenRoute.SEARCH },
                        icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                        label = { Text("Cari") },
                        modifier = Modifier.testTag("nav_search")
                    )

                    NavigationBarItem(
                        selected = currentRoute == ScreenRoute.SETTINGS,
                        onClick = { currentRoute = ScreenRoute.SETTINGS },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Pengaturan") },
                        modifier = Modifier.testTag("nav_settings")
                    )
                }
            }
        }
    ) { innerPadding ->
        when (currentRoute) {
            ScreenRoute.LOGIN -> {
                LoginScreen(
                    credentials = credentials,
                    connectionState = connectionState,
                    isLoading = isLoading,
                    errorMessage = errorMessage,
                    onConnect = { creds ->
                        viewModel.updateCredentials(creds)
                        viewModel.connectSmb(creds)
                    },
                    onDisconnect = { viewModel.disconnectSmb() },
                    onLoginSuccess = { currentRoute = ScreenRoute.HOME },
                    modifier = Modifier.padding(innerPadding)
                )
            }

            ScreenRoute.HOME -> {
                HomeScreen(
                    mediaItems = filteredMedia,
                    selectedFilter = selectedFilter,
                    gridColumns = settings.gridColumns,
                    isLoading = isLoading,
                    errorMessage = errorMessage,
                    onFilterSelected = { viewModel.setFilter(it) },
                    onGridColumnsChanged = { viewModel.updateGridColumns(it) },
                    onMediaClick = { item ->
                        viewModel.addRecent(item)
                        selectedMediaItem = item
                        if (item.type == MediaType.VIDEO) {
                            currentRoute = ScreenRoute.VIDEO_PLAYER
                        } else {
                            val photoList = filteredMedia.filter { it.type == MediaType.PHOTO || it.type == MediaType.RAW }
                            initialPhotoIndex = photoList.indexOfFirst { it.id == item.id }.coerceAtLeast(0)
                            currentRoute = ScreenRoute.PHOTO_VIEWER
                        }
                    },
                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                    isFavorite = { path -> favoriteMedia.any { it.path == path } },
                    getStreamUrl = { viewModel.getStreamUrl(it) },
                    onRefresh = { viewModel.refreshMedia() },
                    onOpenSettings = { currentRoute = ScreenRoute.SETTINGS },
                    modifier = Modifier.padding(innerPadding)
                )
            }

            ScreenRoute.FAVORITES -> {
                FavoritesScreen(
                    favoriteItems = favoriteMedia,
                    gridColumns = settings.gridColumns,
                    onMediaClick = { item ->
                        viewModel.addRecent(item)
                        selectedMediaItem = item
                        if (item.type == MediaType.VIDEO) {
                            currentRoute = ScreenRoute.VIDEO_PLAYER
                        } else {
                            val photoList = favoriteMedia.filter { it.type == MediaType.PHOTO || it.type == MediaType.RAW }
                            initialPhotoIndex = photoList.indexOfFirst { it.id == item.id }.coerceAtLeast(0)
                            currentRoute = ScreenRoute.PHOTO_VIEWER
                        }
                    },
                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                    getStreamUrl = { viewModel.getStreamUrl(it) },
                    modifier = Modifier.padding(innerPadding)
                )
            }

            ScreenRoute.RECENTS -> {
                RecentsScreen(
                    recentEntities = recentEntities,
                    onMediaClick = { item ->
                        selectedMediaItem = item
                        if (item.type == MediaType.VIDEO) {
                            currentRoute = ScreenRoute.VIDEO_PLAYER
                        } else {
                            currentRoute = ScreenRoute.PHOTO_VIEWER
                        }
                    },
                    getStreamUrl = { viewModel.getStreamUrl(it) },
                    modifier = Modifier.padding(innerPadding)
                )
            }

            ScreenRoute.SEARCH -> {
                SearchScreen(
                    query = searchQuery,
                    filteredItems = filteredMedia,
                    gridColumns = settings.gridColumns,
                    onQueryChange = { viewModel.setSearchQuery(it) },
                    onMediaClick = { item ->
                        viewModel.addRecent(item)
                        selectedMediaItem = item
                        if (item.type == MediaType.VIDEO) {
                            currentRoute = ScreenRoute.VIDEO_PLAYER
                        } else {
                            val photoList = filteredMedia.filter { it.type == MediaType.PHOTO || it.type == MediaType.RAW }
                            initialPhotoIndex = photoList.indexOfFirst { it.id == item.id }.coerceAtLeast(0)
                            currentRoute = ScreenRoute.PHOTO_VIEWER
                        }
                    },
                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                    isFavorite = { path -> favoriteMedia.any { it.path == path } },
                    getStreamUrl = { viewModel.getStreamUrl(it) },
                    modifier = Modifier.padding(innerPadding)
                )
            }

            ScreenRoute.SETTINGS -> {
                SettingsScreen(
                    settings = settings,
                    credentials = credentials,
                    connectionState = connectionState,
                    onUpdateTheme = { viewModel.updateTheme(it) },
                    onUpdateGridColumns = { viewModel.updateGridColumns(it) },
                    onReconnect = { viewModel.connectSmb() },
                    onOpenLogin = { currentRoute = ScreenRoute.LOGIN },
                    modifier = Modifier.padding(innerPadding)
                )
            }

            ScreenRoute.PHOTO_VIEWER -> {
                val photoList = filteredMedia.filter { it.type == MediaType.PHOTO || it.type == MediaType.RAW }
                    .ifEmpty { listOfNotNull(selectedMediaItem) }

                PhotoViewerScreen(
                    photos = photoList,
                    initialIndex = initialPhotoIndex,
                    getStreamUrl = { viewModel.getStreamUrl(it) },
                    isFavorite = { path -> favoriteMedia.any { it.path == path } },
                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                    onBack = { currentRoute = ScreenRoute.HOME },
                    modifier = Modifier.padding(innerPadding)
                )
            }

            ScreenRoute.VIDEO_PLAYER -> {
                val item = selectedMediaItem
                if (item != null) {
                    VideoPlayerScreen(
                        videoItem = item,
                        streamUrl = viewModel.getStreamUrl(item.path),
                        initialPositionMs = initialVideoPositionMs,
                        onSavePosition = { pos ->
                            viewModel.updatePlaybackPosition(item.path, pos)
                        },
                        onBack = { currentRoute = ScreenRoute.HOME },
                        modifier = Modifier.padding(innerPadding)
                    )
                } else {
                    LaunchedEffect(Unit) {
                        currentRoute = ScreenRoute.HOME
                    }
                }
            }
        }
    }
}
