package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.core.model.*
import com.example.player.FullscreenPhotoViewer
import com.example.player.VideoPlayerScreen
import com.example.repository.CloudMediaRepository
import com.example.settings.SettingsScreen
import com.example.settings.SettingsViewModel
import com.example.ui.components.BottomNavBar
import com.example.ui.components.FloatingUploadButton
import com.example.ui.components.HeaderSection
import com.example.ui.components.MainTab
import com.example.ui.screens.AlbumsScreen
import com.example.ui.screens.FilesScreen
import com.example.ui.screens.PhotosScreen
import com.example.ui.screens.VideosScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BarraCloudApp()
        }
    }
}

@Composable
fun BarraCloudApp() {
    val settingsViewModel: SettingsViewModel = viewModel()
    val repository = remember { settingsViewModel.repository }
    val scope = rememberCoroutineScope()

    val photos by repository.photos.collectAsStateWithLifecycle()
    val videos by repository.videos.collectAsStateWithLifecycle()
    val albums by repository.albums.collectAsStateWithLifecycle()
    val files by repository.files.collectAsStateWithLifecycle()
    val serverStats by repository.serverStats.collectAsStateWithLifecycle()
    val gridColumns by repository.gridColumns.collectAsStateWithLifecycle()
    val themeOption by repository.themeOption.collectAsStateWithLifecycle()
    val isSyncing by repository.isSyncing.collectAsStateWithLifecycle()
    val errorMessage by repository.errorMessage.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(MainTab.PHOTOS) }
    var isSettingsOpen by remember { mutableStateOf(false) }

    var selectedPhotoIndex by remember { mutableStateOf<Int?>(null) }
    var selectedVideoItem by remember { mutableStateOf<MediaItem?>(null) }

    val darkTheme = when (themeOption) {
        ThemeOption.LIGHT -> false
        ThemeOption.DARK -> true
        ThemeOption.SYSTEM -> isSystemInDarkTheme()
    }

    MyApplicationTheme(darkTheme = darkTheme) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (isSettingsOpen) {
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onBack = { isSettingsOpen = false },
                    modifier = Modifier.fillMaxSize()
                )
            } else if (selectedPhotoIndex != null) {
                FullscreenPhotoViewer(
                    photos = photos,
                    initialIndex = selectedPhotoIndex!!,
                    onClose = { selectedPhotoIndex = null },
                    modifier = Modifier.fillMaxSize()
                )
            } else if (selectedVideoItem != null) {
                VideoPlayerScreen(
                    videoItem = selectedVideoItem!!,
                    onClose = { selectedVideoItem = null },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Scaffold(
                    topBar = {
                        HeaderSection(
                            serverStats = serverStats,
                            onOpenSettings = { isSettingsOpen = true }
                        )
                    },
                    bottomBar = {
                        BottomNavBar(
                            selectedTab = selectedTab,
                            onTabSelected = { selectedTab = it }
                        )
                    },
                    floatingActionButton = {
                        FloatingUploadButton(
                            onFileSelected = { uri ->
                                scope.launch {
                                    val name = "Upload_${System.currentTimeMillis()}.jpg"
                                    val success = repository.uploadFile(name, 2_500_000L, "image/jpeg")
                                    if (success) {
                                        Toast.makeText(
                                            settingsViewModel.getApplication(),
                                            "File uploaded to BARRA CLOUD server!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        )
                    },
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (selectedTab) {
                            MainTab.PHOTOS -> PhotosScreen(
                                photos = photos,
                                columns = gridColumns,
                                isSyncing = isSyncing,
                                onPhotoClick = { photo ->
                                    val index = photos.indexOfFirst { it.id == photo.id }
                                    selectedPhotoIndex = if (index >= 0) index else 0
                                }
                            )

                            MainTab.VIDEOS -> VideosScreen(
                                videos = videos,
                                isSyncing = isSyncing,
                                onVideoClick = { video ->
                                    selectedVideoItem = video
                                }
                            )

                            MainTab.ALBUMS -> AlbumsScreen(
                                albums = albums,
                                onAlbumClick = { album ->
                                    selectedTab = MainTab.PHOTOS
                                }
                            )

                            MainTab.FILES -> FilesScreen(
                                files = files,
                                isSyncing = isSyncing,
                                onFileClick = { file ->
                                    if (file.isVideo) {
                                        selectedVideoItem = file
                                    } else if (file.mimeType.startsWith("image/")) {
                                        val idx = photos.indexOfFirst { it.id == file.id }
                                        selectedPhotoIndex = if (idx >= 0) idx else 0
                                    } else {
                                        Toast.makeText(
                                            settingsViewModel.getApplication(),
                                            "Opening ${file.name}...",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                onDeleteFile = { file ->
                                    scope.launch {
                                        repository.deleteItem(file)
                                    }
                                },
                                onRenameFile = { file, newName ->
                                    scope.launch {
                                        repository.renameItem(file, newName)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Connection Error Dialog
            if (errorMessage != null) {
                AlertDialog(
                    onDismissRequest = { repository.clearErrorMessage() },
                    title = { Text("SMB Connection Error") },
                    text = { Text(errorMessage ?: "") },
                    confirmButton = {
                        Button(
                            onClick = {
                                repository.clearErrorMessage()
                                scope.launch { repository.refreshMediaList() }
                            }
                        ) {
                            Text("Retry Connection")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { repository.clearErrorMessage() }) {
                            Text("Dismiss")
                        }
                    }
                )
            }
        }
    }
}
