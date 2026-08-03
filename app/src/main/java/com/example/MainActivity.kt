package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import com.example.data.local.SecureStorage
import com.example.ui.components.MediaDetailViewer
import com.example.ui.screens.FileManagerScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PhotosScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.VideosScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.SettingsViewModel

sealed class TabScreen(val route: String, val title: String, val icon: ImageVector) {
    object Home : TabScreen("home", "Home", Icons.Default.Collections)
    object Photos : TabScreen("photos", "Foto", Icons.Default.Image)
    object Videos : TabScreen("videos", "Video", Icons.Default.Videocam)
    object Files : TabScreen("files", "File", Icons.Default.Folder)
}

class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val config by settingsViewModel.config.collectAsState()
            val isDarkTheme = when (config.appTheme) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemInDarkTheme()
            }

            MyApplicationTheme(darkTheme = isDarkTheme) {
                val snackbarHostState = remember { SnackbarHostState() }
                var selectedTabIndex by remember { mutableIntStateOf(0) }
                var isSettingsOpen by remember { mutableStateOf(false) }

                val mainSnackbar by mainViewModel.snackbarMessage.collectAsState()
                val settingsSnackbar by settingsViewModel.userMessage.collectAsState()

                LaunchedEffect(mainSnackbar) {
                    mainSnackbar?.let {
                        snackbarHostState.showSnackbar(it)
                        mainViewModel.clearSnackbar()
                    }
                }

                LaunchedEffect(settingsSnackbar) {
                    settingsSnackbar?.let {
                        snackbarHostState.showSnackbar(it)
                        settingsViewModel.clearUserMessage()
                    }
                }

                val tabs = listOf(TabScreen.Home, TabScreen.Photos, TabScreen.Videos, TabScreen.Files)
                val selectedMedia by mainViewModel.selectedMedia.collectAsState()

                if (isSettingsOpen) {
                    val status by settingsViewModel.connectionStatus.collectAsState()
                    val cacheSizeText by settingsViewModel.cacheSize.collectAsState()

                    SettingsScreen(
                        config = config,
                        status = status,
                        cacheSizeText = cacheSizeText,
                        onConfigChange = { settingsViewModel.updateConfig(it) },
                        onSaveAuth = { settingsViewModel.saveAndAuthenticate() },
                        onTestHttp = { settingsViewModel.testHttpConnection() },
                        onTestSamba = { settingsViewModel.testSambaConnection() },
                        onClearCache = { settingsViewModel.clearThumbnailCache() },
                        onNavigateBack = {
                            isSettingsOpen = false
                            mainViewModel.checkSambaConnection()
                            mainViewModel.refreshAllMedia()
                        }
                    )
                } else {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = {
                                    Text(
                                        text = "BARRA CLOUD",
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                                    )
                                },
                                actions = {
                                    IconButton(onClick = {
                                        mainViewModel.refreshAllMedia()
                                        mainViewModel.loadDirectory(mainViewModel.currentPath.value)
                                    }) {
                                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh Data")
                                    }

                                    // Top Right Header Gear Icon for Settings
                                    IconButton(onClick = { isSettingsOpen = true }) {
                                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Pengaturan")
                                    }
                                }
                            )
                        },
                        bottomBar = {
                            NavigationBar {
                                tabs.forEachIndexed { index, tab ->
                                    NavigationBarItem(
                                        selected = selectedTabIndex == index,
                                        onClick = { selectedTabIndex = index },
                                        icon = { Icon(imageVector = tab.icon, contentDescription = tab.title) },
                                        label = { Text(tab.title) }
                                    )
                                }
                            }
                        },
                        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                        modifier = Modifier.fillMaxSize()
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            val timelineGroups by mainViewModel.timelineGroups.collectAsState()
                            val photoItems by mainViewModel.photoMedia.collectAsState()
                            val videoItems by mainViewModel.videoMedia.collectAsState()
                            val fileItems by mainViewModel.fileManagerItems.collectAsState()
                            val currentPath by mainViewModel.currentPath.collectAsState()
                            val isLoading by mainViewModel.isLoading.collectAsState()
                            val isSambaConnected by mainViewModel.isSambaConnected.collectAsState()
                            val clipboardItem by mainViewModel.clipboardItem.collectAsState()

                            when (selectedTabIndex) {
                                0 -> HomeScreen(
                                    groups = timelineGroups,
                                    gridColumns = config.gridColumns,
                                    isLoading = isLoading,
                                    getThumbnailUrl = { item -> mainViewModel.apiService.getStreamUrl(config.httpUrl, item.path, isThumb = true) },
                                    onMediaClick = { item -> mainViewModel.selectMedia(item) },
                                    onRefresh = { mainViewModel.refreshAllMedia() }
                                )

                                1 -> PhotosScreen(
                                    photos = photoItems,
                                    gridColumns = config.gridColumns,
                                    isLoading = isLoading,
                                    getThumbnailUrl = { item -> mainViewModel.apiService.getStreamUrl(config.httpUrl, item.path, isThumb = true) },
                                    onMediaClick = { item -> mainViewModel.selectMedia(item) }
                                )

                                2 -> VideosScreen(
                                    videos = videoItems,
                                    gridColumns = config.gridColumns,
                                    isLoading = isLoading,
                                    getThumbnailUrl = { item -> mainViewModel.apiService.getStreamUrl(config.httpUrl, item.path, isThumb = true) },
                                    onMediaClick = { item -> mainViewModel.selectMedia(item) }
                                )

                                3 -> FileManagerScreen(
                                    currentPath = currentPath,
                                    items = fileItems,
                                    isLoading = isLoading,
                                    isSambaConnected = isSambaConnected,
                                    hasClipboardItem = clipboardItem != null,
                                    onNavigateDirectory = { path -> mainViewModel.loadDirectory(path) },
                                    onNavigateUp = { mainViewModel.navigateUpDirectory() },
                                    onMediaItemClick = { item -> mainViewModel.selectMedia(item) },
                                    onUploadFile = { uri, name -> mainViewModel.uploadFileToCurrentDirectory(uri, name) },
                                    onCopyItem = { item -> mainViewModel.copyItemToClipboard(item) },
                                    onPasteItem = { mainViewModel.pasteItemFromClipboard() },
                                    onMoveItem = { item, newName -> mainViewModel.moveItem(item, newName) },
                                    onDeleteItem = { item -> mainViewModel.deleteMediaItem(item) }
                                )
                            }

                            // Fullscreen detail viewer overlay
                            if (selectedMedia != null) {
                                val fullUrl = mainViewModel.apiService.getStreamUrl(config.httpUrl, selectedMedia!!.path, isThumb = false)
                                val secureStorage = remember { SecureStorage(this@MainActivity) }
                                val authCookie = secureStorage.getAuthCookie()

                                MediaDetailViewer(
                                    item = selectedMedia!!,
                                    fullMediaUrl = fullUrl,
                                    authCookie = authCookie,
                                    onDismiss = { mainViewModel.selectMedia(null) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
