package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import com.example.util.AppLifecycleManager

sealed class TabScreen(val route: String, val title: String, val icon: ImageVector) {
    object Home : TabScreen("home", "Home", Icons.Default.Home)
    object Photos : TabScreen("photos", "Foto", Icons.Default.Image)
    object Videos : TabScreen("videos", "Video", Icons.Default.Videocam)
    object Files : TabScreen("files", "File", Icons.Default.Folder)
}

class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()
    private lateinit var lifecycleManager: AppLifecycleManager

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        lifecycleManager = AppLifecycleManager.getInstance(this)
        lifecycleManager.initObserver()

        setContent {
            val config by settingsViewModel.config.collectAsState()
            val showMissingDialog by lifecycleManager.showTailscaleMissingDialog.collectAsState()
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

                val secureStorage = remember { SecureStorage(this@MainActivity) }
                val authCookie = secureStorage.getAuthCookie()

                if (showMissingDialog) {
                    AlertDialog(
                        onDismissRequest = { lifecycleManager.dismissMissingDialog() },
                        title = { Text("Tailscale Belum Terpasang") },
                        text = { Text("Aplikasi Tailscale belum terpasang di perangkat ini. BARRA CLOUD memerlukan aplikasi Tailscale (com.tailscale.ipn) untuk integrasi VPN resmi.") },
                        confirmButton = {
                            TextButton(onClick = { lifecycleManager.dismissMissingDialog() }) {
                                Text("Mengerti")
                            }
                        }
                    )
                }

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
                val currentPath by mainViewModel.currentPath.collectAsState()

                // Global System Back Button Handling
                if (!isSettingsOpen && selectedMedia == null) {
                    if (selectedTabIndex == 3 && currentPath != "/") {
                        BackHandler {
                            mainViewModel.navigateUpDirectory()
                        }
                    } else if (selectedTabIndex != 0) {
                        BackHandler {
                            selectedTabIndex = 0
                        }
                    }
                }

                if (isSettingsOpen) {
                    val status by settingsViewModel.connectionStatus.collectAsState()
                    val cacheSizeText by settingsViewModel.cacheSize.collectAsState()
                    val logs by settingsViewModel.logs.collectAsState()
                    val connectionLogs by settingsViewModel.connectionLogs.collectAsState()

                    SettingsScreen(
                        config = config,
                        status = status,
                        cacheSizeText = cacheSizeText,
                        logs = logs,
                        connectionLogs = connectionLogs,
                        onConfigChange = { settingsViewModel.updateConfig(it) },
                        onSaveAuth = { settingsViewModel.saveAndAuthenticate() },
                        onTestHttp = { settingsViewModel.testHttpConnection() },
                        onTestSftp = { settingsViewModel.testSftpConnection() },
                        onConnectVpn = { settingsViewModel.connectTailscaleVpn() },
                        onDisconnectVpn = { settingsViewModel.disconnectTailscaleVpn() },
                        onAcquireLease = { settingsViewModel.acquireVpnLease() },
                        onReleaseLease = { settingsViewModel.releaseVpnLease() },
                        onOpenTailscale = { settingsViewModel.openTailscaleApp() },
                        onClearCache = { settingsViewModel.clearThumbnailCache() },
                        onClearLogs = { settingsViewModel.clearLogs() },
                        onNavigateBack = {
                            isSettingsOpen = false
                            mainViewModel.checkSftpConnection()
                            mainViewModel.refreshAllMedia()
                        }
                    )
                } else {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Cloud,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "BARRA CLOUD",
                                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                },
                                actions = {
                                    IconButton(onClick = {
                                        mainViewModel.startInitialConnectionSequence()
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
                                        onClick = {
                                            mainViewModel.selectMedia(null)
                                            selectedTabIndex = index
                                        },
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
                            val allMediaItems by mainViewModel.allMedia.collectAsState()
                            val photoItems by mainViewModel.photoMedia.collectAsState()
                            val videoItems by mainViewModel.videoMedia.collectAsState()
                            val fileItems by mainViewModel.fileManagerItems.collectAsState()
                            val isLoading by mainViewModel.isLoading.collectAsState()
                            val isSftpConnected by mainViewModel.isSftpConnected.collectAsState()
                            val clipboardItem by mainViewModel.clipboardItem.collectAsState()

                            when (selectedTabIndex) {
                                0 -> HomeScreen(
                                    groups = timelineGroups,
                                    gridColumns = config.gridColumns,
                                    isLoading = isLoading,
                                    authCookie = authCookie,
                                    getThumbnailUrl = { item -> mainViewModel.apiService.getStreamUrl(config.httpUrl, item.path, isThumb = true, authCookie = authCookie) },
                                    onMediaClick = { item -> mainViewModel.selectMedia(item) },
                                    onRefresh = { mainViewModel.refreshAllMedia() }
                                )

                                1 -> PhotosScreen(
                                    photos = photoItems,
                                    gridColumns = config.gridColumns,
                                    isLoading = isLoading,
                                    authCookie = authCookie,
                                    getThumbnailUrl = { item -> mainViewModel.apiService.getStreamUrl(config.httpUrl, item.path, isThumb = true, authCookie = authCookie) },
                                    onMediaClick = { item -> mainViewModel.selectMedia(item) }
                                )

                                2 -> VideosScreen(
                                    videos = videoItems,
                                    gridColumns = config.gridColumns,
                                    isLoading = isLoading,
                                    authCookie = authCookie,
                                    getThumbnailUrl = { item -> mainViewModel.apiService.getStreamUrl(config.httpUrl, item.path, isThumb = true, authCookie = authCookie) },
                                    onMediaClick = { item -> mainViewModel.selectMedia(item) }
                                )

                                3 -> FileManagerScreen(
                                    currentPath = currentPath,
                                    items = fileItems,
                                    isLoading = isLoading,
                                    isSftpConnected = isSftpConnected,
                                    adminModeSftp = config.adminModeSftp,
                                    hasClipboardItem = clipboardItem != null,
                                    onNavigateDirectory = { path -> mainViewModel.loadDirectory(path) },
                                    onNavigateUp = { mainViewModel.navigateUpDirectory() },
                                    onMediaItemClick = { item -> mainViewModel.selectMedia(item) },
                                    onUploadFile = { uri, name -> mainViewModel.uploadFileToCurrentDirectory(uri, name) },
                                    onCopyItem = { item -> mainViewModel.copyItemToClipboard(item) },
                                    onPasteItem = { mainViewModel.pasteItemFromClipboard() },
                                    onMoveItem = { item, newName -> mainViewModel.moveItem(item, newName) },
                                    onDeleteItem = { item -> mainViewModel.deleteMediaItem(item) },
                                    onConnectClick = {
                                        mainViewModel.checkSftpConnection()
                                        mainViewModel.loadDirectory(mainViewModel.currentPath.value)
                                    }
                                )
                            }

                            // Fullscreen detail viewer overlay with swipe & zoom support
                            if (selectedMedia != null) {
                                val currentMediaList = when (selectedTabIndex) {
                                    1 -> photoItems
                                    2 -> videoItems
                                    3 -> fileItems.filter { !it.isDir }
                                    else -> if (allMediaItems.isNotEmpty()) allMediaItems else listOf(selectedMedia!!)
                                }
                                val initialIndex = currentMediaList.indexOfFirst { it.path == selectedMedia?.path }.coerceAtLeast(0)

                                MediaDetailViewer(
                                    mediaList = currentMediaList,
                                    initialIndex = initialIndex,
                                    authCookie = authCookie,
                                    getMediaUrl = { item -> mainViewModel.apiService.getStreamUrl(config.httpUrl, item.path, isThumb = false, authCookie = authCookie) },
                                    getThumbUrl = { item -> mainViewModel.apiService.getStreamUrl(config.httpUrl, item.path, isThumb = true, authCookie = authCookie) },
                                    onDismiss = { mainViewModel.selectMedia(null) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            lifecycleManager.onAppExit()
        }
    }
}

