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
    val context = androidx.compose.ui.platform.LocalContext.current
    val settingsViewModel: SettingsViewModel = viewModel()
    val repository = remember { settingsViewModel.repository }
    val scope = rememberCoroutineScope()

    val photos by repository.photos.collectAsStateWithLifecycle()
    val videos by repository.videos.collectAsStateWithLifecycle()
    val albums by repository.albums.collectAsStateWithLifecycle()
    val files by repository.files.collectAsStateWithLifecycle()
    val currentFolderFiles by repository.currentFolderFiles.collectAsStateWithLifecycle()
    val currentPath by repository.currentPath.collectAsStateWithLifecycle()
    val serverStats by repository.serverStats.collectAsStateWithLifecycle()
    val gridColumns by repository.gridColumns.collectAsStateWithLifecycle()
    val themeOption by repository.themeOption.collectAsStateWithLifecycle()
    val isSyncing by repository.isSyncing.collectAsStateWithLifecycle()
    val errorMessage by repository.errorMessage.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(MainTab.PHOTOS) }
    var isSettingsOpen by remember { mutableStateOf(false) }

    var selectedPhotoIndex by remember { mutableStateOf<Int?>(null) }
    var photoViewerList by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
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
                    photos = if (photoViewerList.isNotEmpty()) photoViewerList else photos,
                    initialIndex = selectedPhotoIndex!!,
                    onClose = { selectedPhotoIndex = null },
                    onPrepareFile = { photo -> repository.prepareLocalFile(photo) },
                    modifier = Modifier.fillMaxSize()
                )
            } else if (selectedVideoItem != null) {
                VideoPlayerScreen(
                    videoItem = selectedVideoItem!!,
                    onClose = { selectedVideoItem = null },
                    onPrepareFile = { video -> repository.prepareLocalFile(video) },
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
                                    photoViewerList = photos
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
                                files = currentFolderFiles,
                                currentPath = currentPath,
                                isSyncing = isSyncing,
                                onFolderClick = { folder ->
                                    repository.navigateToFolder(folder.path)
                                },
                                onNavigateUp = {
                                    repository.navigateUp()
                                },
                                onFileClick = { file: MediaItem ->
                                    if (file.isVideo) {
                                        selectedVideoItem = file
                                    } else if (file.mimeType.startsWith("image/")) {
                                        photoViewerList = listOf(file)
                                        selectedPhotoIndex = 0
                                    } else {
                                        scope.launch {
                                            Toast.makeText(
                                                context,
                                                "Downloading ${file.name} to view...",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            val localFile = repository.prepareLocalFile(file)
                                            if (localFile != null && localFile.exists()) {
                                                try {
                                                    val contentUri = androidx.core.content.FileProvider.getUriForFile(
                                                        context,
                                                        "${context.packageName}.fileprovider",
                                                        localFile
                                                    )
                                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                                        setDataAndType(contentUri, file.mimeType.ifBlank { "*/*" })
                                                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                                    }
                                                    context.startActivity(intent)
                                                } catch (e: Exception) {
                                                    Toast.makeText(
                                                        context,
                                                        "Downloaded to cache. No app available to open ${file.name}",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                }
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    "Failed to download ${file.name} from server",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
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
