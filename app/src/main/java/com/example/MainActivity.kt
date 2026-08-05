package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.AppThemeMode
import com.example.model.BarraTab
import com.example.ui.components.BarraBottomNavBar
import com.example.ui.components.BarraTopAppBar
import com.example.ui.components.MediaViewerModal
import com.example.ui.screens.FileManagerView
import com.example.ui.screens.HomeView
import com.example.ui.screens.PhotoView
import com.example.ui.screens.SettingsView
import com.example.ui.screens.VideoView
import com.example.ui.theme.BarraCloudTheme
import com.example.viewmodel.BarraCloudViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: BarraCloudViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val isOledMode = themeMode == AppThemeMode.OLED

            BarraCloudTheme(isOledMode = isOledMode) {
                BarraCloudApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun BarraCloudApp(viewModel: BarraCloudViewModel) {
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val gridColumnCount by viewModel.gridColumnCount.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val isConnected by viewModel.isConnected.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val connectionStatusText by viewModel.connectionStatusText.collectAsStateWithLifecycle()

    val mediaList by viewModel.mediaList.collectAsStateWithLifecycle()
    val selectedMedia by viewModel.selectedMedia.collectAsStateWithLifecycle()

    val currentFilePath by viewModel.currentFilePath.collectAsStateWithLifecycle()
    val fileDirectoryNodes by viewModel.fileDirectoryNodes.collectAsStateWithLifecycle()

    val formattedCacheSize by viewModel.formattedCacheSize.collectAsStateWithLifecycle()
    val terminalLogs by viewModel.terminalLogs.collectAsStateWithLifecycle()
    val sftpConfig by viewModel.sftpConfig.collectAsStateWithLifecycle()
    val tailscaleStatus by viewModel.tailscaleStatus.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("main_scaffold"),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            BarraTopAppBar(
                connectionStatus = connectionStatusText,
                isConnected = isConnected,
                onRefresh = { viewModel.refreshAllData() },
                onOpenSettings = { viewModel.setTab(BarraTab.SETTINGS) }
            )
        },
        bottomBar = {
            BarraBottomNavBar(
                activeTab = activeTab,
                onTabSelected = { viewModel.setTab(it) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (activeTab) {
                BarraTab.HOME -> {
                    HomeView(
                        mediaList = mediaList,
                        gridColumnCount = gridColumnCount,
                        isRefreshing = isRefreshing,
                        cacheManager = viewModel.cacheManager,
                        onMediaClick = { viewModel.selectMedia(it) }
                    )
                }
                BarraTab.PHOTO -> {
                    PhotoView(
                        mediaList = mediaList,
                        gridColumnCount = gridColumnCount,
                        cacheManager = viewModel.cacheManager,
                        onMediaClick = { viewModel.selectMedia(it) }
                    )
                }
                BarraTab.VIDEO -> {
                    VideoView(
                        mediaList = mediaList,
                        gridColumnCount = gridColumnCount,
                        cacheManager = viewModel.cacheManager,
                        onMediaClick = { viewModel.selectMedia(it) }
                    )
                }
                BarraTab.FILE -> {
                    FileManagerView(
                        currentPath = currentFilePath,
                        directoryNodes = fileDirectoryNodes,
                        onFolderClick = { viewModel.loadDirectory(it) },
                        onNavigateUp = { viewModel.navigateUpDirectory() },
                        onUploadClick = {
                            scope.launch {
                                snackbarHostState.showSnackbar("Upload file SFTP ke $currentFilePath siap dipicu.")
                            }
                        }
                    )
                }
                BarraTab.SETTINGS -> {
                    SettingsView(
                        config = sftpConfig,
                        tailscaleStatus = tailscaleStatus,
                        gridColumns = gridColumnCount,
                        themeMode = themeMode,
                        formattedCacheSize = formattedCacheSize,
                        logs = terminalLogs,
                        onUpdateConfig = { viewModel.updateConfig(it) },
                        onConnectTailscale = { authKey, targetIp, nodeName, callback ->
                            viewModel.connectTailscale(authKey, targetIp, nodeName, callback)
                        },
                        onDisconnectTailscale = { viewModel.disconnectTailscale() },
                        onTestConnection = { callback -> viewModel.testConnection(callback) },
                        onSetGridColumns = { viewModel.setGridColumns(it) },
                        onSetThemeMode = { viewModel.setThemeMode(it) },
                        onClearCache = { viewModel.clearCache() },
                        onClearLogs = { viewModel.clearLogs() }
                    )
                }
            }

            // Fullscreen Preview Streaming Modal
            MediaViewerModal(
                item = selectedMedia,
                onDismiss = { viewModel.selectMedia(null) }
            )
        }
    }
}
