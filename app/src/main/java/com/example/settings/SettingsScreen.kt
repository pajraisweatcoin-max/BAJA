package com.example.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sambaConfig by viewModel.sambaConfig.collectAsStateWithLifecycle()
    val tailscaleConfig by viewModel.tailscaleConfig.collectAsStateWithLifecycle()
    val gridColumns by viewModel.gridColumns.collectAsStateWithLifecycle()
    val thumbnailSize by viewModel.thumbnailSize.collectAsStateWithLifecycle()
    val themeOption by viewModel.themeOption.collectAsStateWithLifecycle()
    val isTesting by viewModel.isTestingConnection.collectAsStateWithLifecycle()
    val testResult by viewModel.testConnectionResult.collectAsStateWithLifecycle()

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "BARRA CLOUD Settings",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. SAMBA LOGIN
            SambaSettingsSection(
                initialConfig = sambaConfig,
                isTesting = isTesting,
                onSave = { viewModel.saveSambaConfig(it) },
                onTest = { viewModel.testSambaConnection(it) },
                onLogout = { viewModel.logoutSamba() }
            )

            // 2. TAILSCALE
            TailscaleSettingsSection(
                config = tailscaleConfig,
                onToggleEnabled = { viewModel.toggleTailscale(it) },
                onSaveConfig = { viewModel.saveTailscaleConfig(it) },
                onLogin = { viewModel.loginTailscale() },
                onLogout = { viewModel.logoutTailscale() },
                onReconnect = { viewModel.reconnectTailscale() }
            )

            // 3. GRID SETTINGS
            GridSettingsSection(
                currentColumns = gridColumns,
                onColumnsChange = { viewModel.setGridColumns(it) }
            )

            // 4. THUMBNAIL SIZE
            ThumbnailSettingsSection(
                currentSize = thumbnailSize,
                onSizeSelected = { viewModel.setThumbnailSize(it) }
            )

            // 5. THEME
            ThemeSettingsSection(
                currentTheme = themeOption,
                onThemeSelected = { viewModel.setTheme(it) }
            )

            // 6. CACHE
            CacheSettingsSection(
                onClearImageCache = { viewModel.clearImageCache() },
                onClearVideoCache = { viewModel.clearVideoCache() },
                onClearThumbnailCache = { viewModel.clearThumbnailCache() }
            )

            // 7. HELP
            HelpSettingsSection()

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Connection Test Dialog
    if (testResult != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissTestDialog() },
            title = {
                Text(
                    text = if (testResult?.contains("SUCCESS") == true) "Connection Success" else "Connection Failed",
                    fontWeight = FontWeight.Bold
                )
            },
            text = { Text(testResult ?: "") },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissTestDialog() }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SambaSettingsSection(
    initialConfig: SambaConfig,
    isTesting: Boolean,
    onSave: (SambaConfig) -> Unit,
    onTest: (SambaConfig) -> Unit,
    onLogout: () -> Unit
) {
    var host by remember(initialConfig) { mutableStateOf(initialConfig.host) }
    var port by remember(initialConfig) { mutableStateOf(initialConfig.port.toString()) }
    var shareName by remember(initialConfig) { mutableStateOf(initialConfig.shareName) }
    var username by remember(initialConfig) { mutableStateOf(initialConfig.username) }
    var password by remember(initialConfig) { mutableStateOf(initialConfig.password) }
    var autoConnect by remember(initialConfig) { mutableStateOf(initialConfig.autoConnect) }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SectionHeader("1. SAMBA LOGIN", Icons.Default.Dns)

            OutlinedTextField(
                value = host,
                onValueChange = { host = it },
                label = { Text("Host / IP Address") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it },
                    label = { Text("Port") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = shareName,
                    onValueChange = { shareName = it },
                    label = { Text("Share Name") },
                    singleLine = true,
                    modifier = Modifier.weight(2f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Auto Connect on Launch", fontWeight = FontWeight.Medium)
                Switch(
                    checked = autoConnect,
                    onCheckedChange = { autoConnect = it }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val currentConfig = SambaConfig(
                            host = host,
                            port = port.toIntOrNull() ?: 445,
                            shareName = shareName,
                            username = username,
                            password = password,
                            autoConnect = autoConnect
                        )
                        onSave(currentConfig)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save")
                }

                OutlinedButton(
                    onClick = {
                        val currentConfig = SambaConfig(
                            host = host,
                            port = port.toIntOrNull() ?: 445,
                            shareName = shareName,
                            username = username,
                            password = password,
                            autoConnect = autoConnect
                        )
                        onTest(currentConfig)
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isTesting,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Test")
                    }
                }

                OutlinedButton(
                    onClick = {
                        host = ""
                        shareName = ""
                        username = ""
                        password = ""
                        autoConnect = false
                        onLogout()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Logout")
                }
            }
        }
    }
}

@Composable
private fun TailscaleSettingsSection(
    config: TailscaleConfig,
    onToggleEnabled: (Boolean) -> Unit,
    onSaveConfig: (TailscaleConfig) -> Unit,
    onLogin: () -> Unit,
    onLogout: () -> Unit,
    onReconnect: () -> Unit
) {
    var authKey by remember(config.authKey) { mutableStateOf(config.authKey) }
    var nodeIp by remember(config.nodeIp) { mutableStateOf(config.nodeIp) }
    var deviceName by remember(config.deviceName) { mutableStateOf(config.deviceName) }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SectionHeader("2. TAILSCALE", Icons.Default.VpnKey)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Enable Tailscale Engine", fontWeight = FontWeight.SemiBold)
                    Text("Embedded userspace networking (tsnet)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = config.enabled,
                    onCheckedChange = onToggleEnabled
                )
            }

            AnimatedVisibility(visible = config.enabled) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Connection State:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = config.connectionState.label,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (config.connectionState == TailscaleConnectionState.CONNECTED) Color(0xFF10B981) else MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Tailscale Auth Key Input
                    OutlinedTextField(
                        value = authKey,
                        onValueChange = { authKey = it },
                        label = { Text("Tailscale Auth Key (Optional)") },
                        placeholder = { Text("tskey-auth-kXXXXX...") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        supportingText = {
                            Text("Dapatkan Auth Key di login.tailscale.com > Settings > Keys", fontSize = 11.sp)
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = nodeIp,
                            onValueChange = { nodeIp = it },
                            label = { Text("Server Node IP") },
                            placeholder = { Text("100.64.1.42") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = deviceName,
                            onValueChange = { deviceName = it },
                            label = { Text("Device Name") },
                            placeholder = { Text("barra-nas") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val updated = config.copy(
                                    authKey = authKey,
                                    nodeIp = nodeIp,
                                    deviceName = deviceName
                                )
                                onSaveConfig(updated)
                                onLogin()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Save & Connect")
                        }
                        OutlinedButton(
                            onClick = onReconnect,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Reconnect")
                        }
                        OutlinedButton(
                            onClick = onLogout,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Logout")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GridSettingsSection(
    currentColumns: Int,
    onColumnsChange: (Int) -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SectionHeader("3. GRID SETTINGS", Icons.Default.GridView)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Columns in Gallery Grid:", fontWeight = FontWeight.Medium)
                Text("$currentColumns Columns", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }

            Slider(
                value = currentColumns.toFloat(),
                onValueChange = { onColumnsChange(it.toInt()) },
                valueRange = 2f..5f,
                steps = 2
            )
        }
    }
}

@Composable
private fun ThumbnailSettingsSection(
    currentSize: ThumbnailSizeOption,
    onSizeSelected: (ThumbnailSizeOption) -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SectionHeader("4. THUMBNAIL SIZE", Icons.Default.PhotoSizeSelectActual)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThumbnailSizeOption.entries.forEach { option ->
                    val isSelected = option == currentSize
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSizeSelected(option) },
                        label = { Text(option.label) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeSettingsSection(
    currentTheme: ThemeOption,
    onThemeSelected: (ThemeOption) -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SectionHeader("5. THEME", Icons.Default.Palette)

            ThemeOption.entries.forEach { option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (option == currentTheme),
                        onClick = { onThemeSelected(option) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(option.label, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun CacheSettingsSection(
    onClearImageCache: () -> Unit,
    onClearVideoCache: () -> Unit,
    onClearThumbnailCache: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SectionHeader("6. CACHE MANAGEMENT", Icons.Default.CleaningServices)

            OutlinedButton(
                onClick = onClearImageCache,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Clear Image Cache")
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onClearVideoCache,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Clear Video Cache")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onClearThumbnailCache,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Clear All Thumbnail Caches")
            }
        }
    }
}

@Composable
private fun HelpSettingsSection() {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SectionHeader("7. HELP & FAQ", Icons.Default.HelpOutline)

            Text("How to login Samba:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(
                "1. Input Host IP (Local 192.168.x.x or Tailscale 100.x.x.x)\n" +
                "2. Set Port 445 and Share Name (e.g. Media)\n" +
                "3. Fill Username & Password and tap Save & Test.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text("How Tailscale integration works:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(
                "Turn ON Tailscale switch to run the embedded userspace networking engine directly in BARRA CLOUD without installing external VPN apps.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text("App Version:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("BARRA CLOUD v1.0.0 (Build 2026.08)", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
        }
    }
}
