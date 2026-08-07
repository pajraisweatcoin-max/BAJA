package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.db.AppDatabase
import com.example.data.sftp.ConnectionState
import com.example.ui.screens.ServerListScreen
import com.example.ui.screens.SftpExplorerScreen
import com.example.ui.screens.TextEditorScreen
import com.example.ui.screens.TransfersScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.SftpViewModel
import com.example.viewmodel.SftpViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = AppDatabase.getDatabase(applicationContext)

        setContent {
            MyApplicationTheme {
                val viewModel: SftpViewModel = viewModel(
                    factory = SftpViewModelFactory(db)
                )

                MainAppContent(viewModel = viewModel)
            }
        }
    }
}

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Explorer : BottomNavItem("explorer", "SFTP File", Icons.Default.Folder)
    object Servers : BottomNavItem("servers", "Server SSH", Icons.Default.Storage)
    object Transfers : BottomNavItem("transfers", "Transfer", Icons.Default.SwapVert)
}

@Composable
fun MainAppContent(viewModel: SftpViewModel) {
    val context = LocalContext.current
    val navController = rememberNavController()

    val editingTextFile by viewModel.editingTextFile.collectAsState()
    val uiMessage by viewModel.uiMessage.collectAsState()
    val transferTasks by viewModel.transferTasks.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()

    val activeTransfersCount = remember(transferTasks) {
        transferTasks.count { !it.isCompleted && it.error == null }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.uiMessage.collect { msg ->
            if (!msg.isNullOrBlank()) {
                val currentMsg = msg
                viewModel.clearUiMessage()
                snackbarHostState.showSnackbar(currentMsg)
            }
        }
    }

    if (editingTextFile != null) {
        TextEditorScreen(
            viewModel = viewModel,
            onClose = { viewModel.closeTextEditor() }
        )
    } else {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                NavigationBar {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route

                    val items = listOf(
                        BottomNavItem.Explorer,
                        BottomNavItem.Servers,
                        BottomNavItem.Transfers
                    )

                    items.forEach { item ->
                        val isSelected = currentRoute == item.route

                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                if (item == BottomNavItem.Transfers && activeTransfersCount > 0) {
                                    BadgedBox(
                                        badge = { Badge { Text("$activeTransfersCount") } }
                                    ) {
                                        Icon(item.icon, contentDescription = item.title)
                                    }
                                } else {
                                    Icon(item.icon, contentDescription = item.title)
                                }
                            },
                            label = { Text(item.title) },
                            modifier = Modifier.testTag("nav_${item.route}")
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = BottomNavItem.Servers.route,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                composable(BottomNavItem.Servers.route) {
                    ServerListScreen(
                        viewModel = viewModel,
                        onNavigateToExplorer = {
                            navController.navigate(BottomNavItem.Explorer.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }

                composable(BottomNavItem.Explorer.route) {
                    SftpExplorerScreen(
                        viewModel = viewModel,
                        onNavigateToServers = {
                            navController.navigate(BottomNavItem.Servers.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }

                composable(BottomNavItem.Transfers.route) {
                    TransfersScreen(viewModel = viewModel)
                }
            }
        }
    }
}
