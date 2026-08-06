package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Http
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.ui.text.font.FontFamily
import com.example.util.LogEntry
import com.example.util.LogLevel
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.model.ConnectionStatus
import com.example.data.model.ServerConfig
import com.example.ui.components.StatusBadge

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    config: ServerConfig,
    status: ConnectionStatus,
    cacheSizeText: String,
    logs: List<LogEntry> = emptyList(),
    connectionLogs: List<String> = emptyList(),
    onConfigChange: (ServerConfig) -> Unit,
    onSaveAuth: () -> Unit,
    onTestHttp: () -> Unit,
    onTestSftp: () -> Unit,
    onConnectVpn: () -> Unit = {},
    onDisconnectVpn: () -> Unit = {},
    onAcquireLease: () -> Unit = {},
    onReleaseLease: () -> Unit = {},
    onOpenTailscale: () -> Unit = {},
    onClearCache: () -> Unit,
    onClearLogs: () -> Unit = {},
    onNavigateBack: () -> Unit
) {
    BackHandler {
        onNavigateBack()
    }

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pengaturan BARRA CLOUD") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            // A. HTTP BARRA CLOUD CONFIGURATION
            SettingsCard(
                title = "A. Konfigurasi HTTP BARRA CLOUD",
                icon = Icons.Default.Http
            ) {
                OutlinedTextField(
                    value = config.httpUrl,
                    onValueChange = { onConfigChange(config.copy(httpUrl = it)) },
                    label = { Text("Server Address / URL") },
                    placeholder = { Text("http://homeserver.local") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = config.adminPassword,
                    onValueChange = { onConfigChange(config.copy(adminPassword = it)) },
                    label = { Text("Password Admin") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                StatusBadge(
                    state = status.httpState,
                    message = status.httpMessage,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = onTestHttp,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Tes Koneksi HTTP")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = onSaveAuth,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Simpan & Auth")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // B. SFTP REMOTE FILE MANAGER CONFIGURATION
            SettingsCard(
                title = "B. SFTP Remote File Manager & Mode Admin",
                icon = Icons.Default.FolderShared
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Aktifkan Fitur SFTP",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = config.enableSftp,
                        onCheckedChange = { onConfigChange(config.copy(enableSftp = it)) }
                    )
                }

                if (config.enableSftp) {
                    Spacer(modifier = Modifier.height(12.dp))

                    SettingsToggleRow(
                        title = "Auto Connect SFTP",
                        subtitle = "Otomatis hubungkan SFTP Remote File Manager saat aplikasi aktif atau membuka folder",
                        checked = config.autoConnectSftp,
                        onCheckedChange = { onConfigChange(config.copy(autoConnectSftp = it)) }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Mode Admin SFTP Switch
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Mode Admin SFTP (Izin Hapus)",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (config.adminModeSftp) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = if (config.adminModeSftp)
                                    "ON: Fitur Hapus file di File Manager diizinkan."
                                else
                                    "OFF: Terkunci (hanya Copy, Paste, Move, Rename).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = config.adminModeSftp,
                            onCheckedChange = { onConfigChange(config.copy(adminModeSftp = it)) }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = config.sftpHost,
                            onValueChange = { onConfigChange(config.copy(sftpHost = it)) },
                            label = { Text("Host / IP SFTP") },
                            placeholder = { Text("192.168.1.100") },
                            singleLine = true,
                            modifier = Modifier.weight(2f)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        OutlinedTextField(
                            value = config.sftpPort.toString(),
                            onValueChange = {
                                val portVal = it.toIntOrNull() ?: 22
                                onConfigChange(config.copy(sftpPort = portVal))
                            },
                            label = { Text("Port SFTP") },
                            placeholder = { Text("22") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = config.sftpUsername,
                        onValueChange = { onConfigChange(config.copy(sftpUsername = it)) },
                        label = { Text("Username SSH / SFTP") },
                        placeholder = { Text("root") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = config.sftpPassword,
                        onValueChange = { onConfigChange(config.copy(sftpPassword = it)) },
                        label = { Text("Password SSH / SFTP") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    StatusBadge(
                        state = status.sftpState,
                        message = status.sftpMessage,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = onTestSftp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Tes Koneksi SFTP Handshake")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // C. TAILSCALE INTEGRATION & VPN LEASE SYSTEM
            SettingsCard(
                title = "C. Integrasi Tailscale & Secure VPN Ownership",
                icon = Icons.Default.VpnKey
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Aktifkan Integrasi Tailscale",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Pengelola utama koneksi VPN via Broadcast Intent resmi (com.tailscale.ipn)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = config.enableTailscale,
                        onCheckedChange = { onConfigChange(config.copy(enableTailscale = it)) }
                    )
                }

                if (config.enableTailscale) {
                    Spacer(modifier = Modifier.height(12.dp))

                    // Secure VPN Ownership Mode
                    SettingsToggleRow(
                        title = "Secure VPN Ownership Mode",
                        subtitle = "BARRA CLOUD sebagai pemilik utama VPN. VPN terputus saat aplikasi tidak digunakan.",
                        checked = config.secureVpnOwnership,
                        onCheckedChange = { onConfigChange(config.copy(secureVpnOwnership = it)) }
                    )

                    // Enable VPN Lease System
                    SettingsToggleRow(
                        title = "Enable VPN Lease System",
                        subtitle = "Gunakan Lease ID (Session ID, Owner, Connection Time) untuk kontrol kepemilikan VPN.",
                        checked = config.enableVpnLease,
                        onCheckedChange = { onConfigChange(config.copy(enableVpnLease = it)) }
                    )

                    // Auto Connect / Disconnect / Reconnect
                    SettingsToggleRow(
                        title = "Auto Connect VPN",
                        subtitle = "Otomatis kirim broadcast CONNECT_VPN saat BARRA CLOUD aktif",
                        checked = config.autoConnectVpn,
                        onCheckedChange = { onConfigChange(config.copy(autoConnectVpn = it)) }
                    )

                    SettingsToggleRow(
                        title = "Auto Disconnect VPN",
                        subtitle = "Otomatis kirim broadcast DISCONNECT_VPN saat aplikasi ditutup/background timer habis",
                        checked = config.autoDisconnectVpn,
                        onCheckedChange = { onConfigChange(config.copy(autoDisconnectVpn = it)) }
                    )

                    SettingsToggleRow(
                        title = "Auto Reconnect VPN",
                        subtitle = "Otomatis hubungkan kembali jika VPN terputus saat BARRA CLOUD masih aktif",
                        checked = config.autoReconnectVpn,
                        onCheckedChange = { onConfigChange(config.copy(autoReconnectVpn = it)) }
                    )

                    // Monitoring
                    SettingsToggleRow(
                        title = "Monitor VPN",
                        subtitle = "Pantau status interface VPN via ConnectivityManager.NetworkCallback",
                        checked = config.monitorVpn,
                        onCheckedChange = { onConfigChange(config.copy(monitorVpn = it)) }
                    )

                    SettingsToggleRow(
                        title = "Monitor Tailscale",
                        subtitle = "Deteksi perubahan status dan pemutusan eksternal dari aplikasi Tailscale",
                        checked = config.monitorTailscale,
                        onCheckedChange = { onConfigChange(config.copy(monitorTailscale = it)) }
                    )

                    // Disconnect Rules
                    SettingsToggleRow(
                        title = "Disconnect when Background",
                        subtitle = "Putus VPN saat aplikasi berada di latar belakang melebihi timer",
                        checked = config.disconnectOnBackground,
                        onCheckedChange = { onConfigChange(config.copy(disconnectOnBackground = it)) }
                    )

                    SettingsToggleRow(
                        title = "Disconnect when Exit",
                        subtitle = "Putus VPN saat seluruh Activity aplikasi selesai",
                        checked = config.disconnectOnExit,
                        onCheckedChange = { onConfigChange(config.copy(disconnectOnExit = it)) }
                    )

                    SettingsToggleRow(
                        title = "Keep VPN Alive while BarraCloud Running",
                        subtitle = "Jaga VPN tetap aktif selama BARRA CLOUD digunakan",
                        checked = config.keepVpnAlive,
                        onCheckedChange = { onConfigChange(config.copy(keepVpnAlive = it)) }
                    )

                    SettingsToggleRow(
                        title = "Auto Open Tailscale if Login Required",
                        subtitle = "Buka aplikasi Tailscale jika user belum login",
                        checked = config.autoOpenTailscaleIfLoginRequired,
                        onCheckedChange = { onConfigChange(config.copy(autoOpenTailscaleIfLoginRequired = it)) }
                    )

                    // Exit Node
                    SettingsToggleRow(
                        title = "Enable Exit Node",
                        subtitle = "Kirim broadcast USE_EXIT_NODE dengan nama exit node spesifik",
                        checked = config.enableExitNode,
                        onCheckedChange = { onConfigChange(config.copy(enableExitNode = it)) }
                    )

                    if (config.enableExitNode) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = config.exitNodeName,
                            onValueChange = { onConfigChange(config.copy(exitNodeName = it)) },
                            label = { Text("Exit Node Name") },
                            placeholder = { Text("misal: my-exit-node") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // UI Feedback Settings
                    SettingsToggleRow(
                        title = "Show Connection Toast",
                        subtitle = "Tampilkan Toast untuk seluruh aktivitas koneksi dan lease",
                        checked = config.showConnectionToast,
                        onCheckedChange = { onConfigChange(config.copy(showConnectionToast = it)) }
                    )

                    SettingsToggleRow(
                        title = "Connection Notification",
                        subtitle = "Tampilkan Foreground Notification status VPN saat aplikasi aktif",
                        checked = config.connectionNotification,
                        onCheckedChange = { onConfigChange(config.copy(connectionNotification = it)) }
                    )

                    SettingsToggleRow(
                        title = "Save Connection History",
                        subtitle = "Simpan log riwayat aktivitas koneksi dan lease",
                        checked = config.saveConnectionHistory,
                        onCheckedChange = { onConfigChange(config.copy(saveConnectionHistory = it)) }
                    )

                    SettingsToggleRow(
                        title = "Allow Manual VPN Control",
                        subtitle = "Izinkan kontrol VPN manual di luar kepemilikan BARRA CLOUD (Default OFF)",
                        checked = config.allowManualVpnControl,
                        onCheckedChange = { onConfigChange(config.copy(allowManualVpnControl = it)) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Numeric Configurations
                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = config.reconnectDelay.toString(),
                            onValueChange = { val v = it.toLongOrNull() ?: 1000L; onConfigChange(config.copy(reconnectDelay = v)) },
                            label = { Text("Reconnect Delay (ms)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = config.reconnectRetry.toString(),
                            onValueChange = { val v = it.toIntOrNull() ?: 5; onConfigChange(config.copy(reconnectRetry = v)) },
                            label = { Text("Reconnect Retry") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = config.backgroundDisconnectDelay.toString(),
                        onValueChange = { val v = it.toLongOrNull() ?: 30L; onConfigChange(config.copy(backgroundDisconnectDelay = v)) },
                        label = { Text("Background Disconnect Delay (detik)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Action Controls
                    Text(
                        text = "Aksi Langsung Intent & Lease",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = onConnectVpn,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Connect VPN")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = onDisconnectVpn,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Disconnect VPN")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = onAcquireLease,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Acquire Lease")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = onReleaseLease,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Release Lease")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = onOpenTailscale,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Buka Aplikasi Tailscale")
                    }

                    // Connection History Preview
                    if (connectionLogs.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Riwayat Aktivitas Koneksi VPN (${connectionLogs.size}):",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(8.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            connectionLogs.forEach { logLine ->
                                Text(
                                    text = logLine,
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // D. TAMPILAN & KUSTOMISASI GRID
            SettingsCard(
                title = "D. Tampilan & Kustomisasi Grid",
                icon = Icons.Default.GridView
            ) {
                Text(
                    text = "Jumlah Kolom Grid: ${config.gridColumns} Kolom",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                )

                Slider(
                    value = config.gridColumns.toFloat(),
                    onValueChange = { onConfigChange(config.copy(gridColumns = it.toInt())) },
                    valueRange = 2f..8f,
                    steps = 5,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                var themeExpanded by remember { mutableStateOf(false) }
                val themeOptions = listOf("SYSTEM" to "Ikuti Sistem HP", "DARK" to "Gelap (Dark Mode)", "LIGHT" to "Terang (Light Mode)")
                val currentThemeLabel = themeOptions.firstOrNull { it.first == config.appTheme }?.second ?: "Ikuti Sistem HP"

                ExposedDropdownMenuBox(
                    expanded = themeExpanded,
                    onExpandedChange = { themeExpanded = !themeExpanded }
                ) {
                    OutlinedTextField(
                        value = currentThemeLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Pilihan Tema Aplikasi") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = themeExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = themeExpanded,
                        onDismissRequest = { themeExpanded = false }
                    ) {
                        themeOptions.forEach { (key, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    onConfigChange(config.copy(appTheme = key))
                                    themeExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // E. THUMBNAIL CACHE MANAGER
            SettingsCard(
                title = "E. Thumbnail Cache Manager",
                icon = Icons.Default.CleaningServices
            ) {
                Text(
                    text = "Ukuran Cache Lokal: $cacheSizeText",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )

                Spacer(modifier = Modifier.height(12.dp))

                var limitExpanded by remember { mutableStateOf(false) }
                val limitOptions = listOf(500L to "500 MB", 1000L to "1 GB", -1L to "Unlimited")
                val currentLimitLabel = limitOptions.firstOrNull { it.first == config.cacheLimitMb }?.second ?: "1 GB"

                ExposedDropdownMenuBox(
                    expanded = limitExpanded,
                    onExpandedChange = { limitExpanded = !limitExpanded }
                ) {
                    OutlinedTextField(
                        value = currentLimitLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Opsi Limit Cache") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = limitExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = limitExpanded,
                        onDismissRequest = { limitExpanded = false }
                    ) {
                        limitOptions.forEach { (limit, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    onConfigChange(config.copy(cacheLimitMb = limit))
                                    limitExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Sync Thumbnail via Wi-Fi Only",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = config.syncWifiOnly,
                        onCheckedChange = { onConfigChange(config.copy(syncWifiOnly = it)) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onClearCache,
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Clear Thumbnail Cache")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // F. TERMINAL LOG REALTIME (LIVE APP TERMINAL)
            SettingsCard(
                title = "F. Terminal Log Realtime (Live App Terminal)",
                icon = Icons.Default.Terminal
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Aktivitas Background (${logs.size} log)",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedButton(
                        onClick = onClearLogs,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("Clear Log", style = MaterialTheme.typography.labelSmall)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                val logListState = rememberLazyListState()
                LaunchedEffect(logs.size) {
                    if (logs.isNotEmpty()) {
                        logListState.animateScrollToItem(logs.size - 1)
                    }
                }

                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                ) {
                    if (logs.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Belum ada log aktivitas...",
                                color = Color.Gray,
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    } else {
                        LazyColumn(
                            state = logListState,
                            contentPadding = PaddingValues(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(logs) { entry ->
                                val textColor = when (entry.level) {
                                    LogLevel.SUCCESS -> Color(0xFF4ADE80)
                                    LogLevel.ERROR -> Color(0xFFF87171)
                                    LogLevel.WARNING -> Color(0xFFFACC15)
                                    LogLevel.INFO -> Color(0xFF38BDF8)
                                }
                                Text(
                                    text = "[${entry.timestamp}] [${entry.tag}] ${entry.message}",
                                    color = textColor,
                                    fontFamily = FontFamily.Monospace,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

