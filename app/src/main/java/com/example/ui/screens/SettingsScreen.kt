package com.example.ui.screens

import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.VpnKey
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    config: ServerConfig,
    status: ConnectionStatus,
    cacheSizeText: String,
    onConfigChange: (ServerConfig) -> Unit,
    onSaveAuth: () -> Unit,
    onTestHttp: () -> Unit,
    onTestSamba: () -> Unit,
    onClearCache: () -> Unit,
    onNavigateBack: () -> Unit
) {
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

            // B. TAILSCALE VPN INTEGRATION
            SettingsCard(
                title = "B. Integrasi Tailscale VPN (Embedded)",
                icon = Icons.Default.VpnKey
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Gunakan Tailscale VPN",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = config.useTailscale,
                        onCheckedChange = { onConfigChange(config.copy(useTailscale = it)) }
                    )
                }

                if (config.useTailscale) {
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = config.tailscaleAuthKey,
                        onValueChange = { onConfigChange(config.copy(tailscaleAuthKey = it)) },
                        label = { Text("Tailscale Auth Key") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = config.tailnetHost,
                        onValueChange = { onConfigChange(config.copy(tailnetHost = it)) },
                        label = { Text("Tailnet Host Address") },
                        placeholder = { Text("100.x.y.z atau server.tailnet.ts.net") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // C. SAMBA / SMB CONFIGURATION
            SettingsCard(
                title = "C. Konfigurasi Samba / SMB (Full Control)",
                icon = Icons.Default.FolderShared
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Aktifkan Fitur Samba",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = config.enableSamba,
                        onCheckedChange = { onConfigChange(config.copy(enableSamba = it)) }
                    )
                }

                if (config.enableSamba) {
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = config.sambaHost,
                        onValueChange = { onConfigChange(config.copy(sambaHost = it)) },
                        label = { Text("Samba IP / Host") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = config.sambaPort.toString(),
                            onValueChange = {
                                val port = it.toIntOrNull() ?: 445
                                onConfigChange(config.copy(sambaPort = port))
                            },
                            label = { Text("Port") },
                            singleLine = true,
                            modifier = Modifier.weight(0.4f)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        OutlinedTextField(
                            value = config.sambaShare,
                            onValueChange = { onConfigChange(config.copy(sambaShare = it)) },
                            label = { Text("Share Name") },
                            placeholder = { Text("exthdd") },
                            singleLine = true,
                            modifier = Modifier.weight(0.6f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = config.sambaUsername,
                        onValueChange = { onConfigChange(config.copy(sambaUsername = it)) },
                        label = { Text("Username Samba") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = config.sambaPassword,
                        onValueChange = { onConfigChange(config.copy(sambaPassword = it)) },
                        label = { Text("Password Samba") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    StatusBadge(
                        state = status.sambaState,
                        message = status.sambaMessage,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = onTestSamba,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Tes Koneksi Samba")
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
