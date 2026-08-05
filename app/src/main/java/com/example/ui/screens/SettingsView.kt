package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppThemeMode
import com.example.model.LogEntry
import com.example.model.SftpConfig
import com.example.model.TailscaleStatus
import com.example.ui.components.TerminalLogView
import com.example.ui.theme.BarraAmberFolder
import com.example.ui.theme.BarraCyanPrimary
import com.example.ui.theme.BarraTextSecondary

@Composable
fun SettingsView(
    config: SftpConfig,
    tailscaleStatus: TailscaleStatus,
    gridColumns: Int,
    themeMode: AppThemeMode,
    formattedCacheSize: String,
    logs: List<LogEntry>,
    onUpdateConfig: (SftpConfig) -> Unit,
    onConnectTailscale: (authKey: String, targetIp: String, nodeName: String, onResult: (Boolean, String) -> Unit) -> Unit,
    onDisconnectTailscale: () -> Unit,
    onTestConnection: (onResult: (Boolean, String) -> Unit) -> Unit,
    onSetGridColumns: (Int) -> Unit,
    onSetThemeMode: (AppThemeMode) -> Unit,
    onClearCache: () -> Unit,
    onClearLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    var host by remember(config.host) { mutableStateOf(config.host) }
    var port by remember(config.port) { mutableStateOf(config.port.toString()) }
    var username by remember(config.username) { mutableStateOf(config.username) }
    var password by remember(config.password) { mutableStateOf(config.password) }
    var mediaRoot by remember(config.mediaRoot) { mutableStateOf(config.mediaRoot) }
    var isDemoMode by remember(config.isDemoMode) { mutableStateOf(config.isDemoMode) }

    var useTailscale by remember(config.useTailscale) { mutableStateOf(config.useTailscale) }
    var tailscaleAuthKey by remember(config.tailscaleAuthKey) { mutableStateOf(config.tailscaleAuthKey) }
    var tailscaleIp by remember(config.tailscaleIp) { mutableStateOf(config.tailscaleIp) }
    var tailscaleNodeName by remember(config.tailscaleNodeName) { mutableStateOf(config.tailscaleNodeName) }

    var isConnectingTailscale by remember { mutableStateOf(false) }
    var tailscaleTestResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    var isTestingConnection by remember { mutableStateOf(false) }
    var connectionTestResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section 1: Embedded Tailscale Engine
        SettingsSectionHeader(title = "EMBEDDED TAILSCALE ENGINE (LUAR JARINGAN / INTERNET)", icon = Icons.Default.Security)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Status Tunnel Tailscale",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = "Akses server via Mobile Data / Luar Rumah",
                            style = MaterialTheme.typography.labelSmall,
                            color = BarraTextSecondary
                        )
                    }

                    // Status Badge Indicator
                    val (badgeBg, badgeFg, badgeText) = when (tailscaleStatus) {
                        TailscaleStatus.CONNECTED -> Triple(Color(0xFF064E3B), Color(0xFF34D399), "CONNECTED ($tailscaleIp)")
                        TailscaleStatus.CONNECTING -> Triple(Color(0xFF78350F), Color(0xFFFBBF24), "CONNECTING...")
                        TailscaleStatus.DISCONNECTED -> Triple(Color(0xFF334155), Color(0xFF94A3B8), "DISCONNECTED")
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(badgeBg)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(badgeFg)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                color = badgeFg
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Gunakan Embedded Tailscale (On/Off)",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White
                    )
                    Switch(
                        checked = useTailscale,
                        onCheckedChange = {
                            useTailscale = it
                            val updated = config.copy(
                                useTailscale = it,
                                tailscaleAuthKey = tailscaleAuthKey,
                                tailscaleIp = tailscaleIp,
                                tailscaleNodeName = tailscaleNodeName
                            )
                            onUpdateConfig(updated)
                            if (!it) {
                                onDisconnectTailscale()
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = BarraCyanPrimary
                        ),
                        modifier = Modifier.testTag("use_tailscale_switch")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = tailscaleAuthKey,
                    onValueChange = {
                        tailscaleAuthKey = it
                        onUpdateConfig(config.copy(tailscaleAuthKey = it))
                    },
                    label = { Text("Tailscale Auth Key (tskey-auth-...)") },
                    placeholder = { Text("tskey-auth-k1234567890abcdef-...") },
                    singleLine = true,
                    colors = outlinedFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_tailscale_auth_key")
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = tailscaleIp,
                        onValueChange = {
                            tailscaleIp = it
                            onUpdateConfig(config.copy(tailscaleIp = it))
                        },
                        label = { Text("IP Server Tailscale") },
                        placeholder = { Text("100.x.y.z") },
                        singleLine = true,
                        colors = outlinedFieldColors(),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_tailscale_ip")
                    )

                    OutlinedTextField(
                        value = tailscaleNodeName,
                        onValueChange = {
                            tailscaleNodeName = it
                            onUpdateConfig(config.copy(tailscaleNodeName = it))
                        },
                        label = { Text("Nama Node HP") },
                        placeholder = { Text("barra-mobile-app") },
                        singleLine = true,
                        colors = outlinedFieldColors(),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_tailscale_node_name")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            isConnectingTailscale = true
                            tailscaleTestResult = null
                            onConnectTailscale(tailscaleAuthKey, tailscaleIp, tailscaleNodeName) { success, msg ->
                                isConnectingTailscale = false
                                tailscaleTestResult = Pair(success, msg)
                            }
                        },
                        enabled = !isConnectingTailscale && useTailscale,
                        colors = ButtonDefaults.buttonColors(containerColor = BarraCyanPrimary, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("connect_tailscale_button")
                    ) {
                        if (isConnectingTailscale) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.Black,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Handshake Tailscale...", fontWeight = FontWeight.Bold)
                        } else {
                            Text("Connect Tailscale", fontWeight = FontWeight.Bold)
                        }
                    }

                    if (tailscaleStatus == TailscaleStatus.CONNECTED) {
                        Button(
                            onClick = {
                                onDisconnectTailscale()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF991B1B), contentColor = Color.White),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Disconnect")
                        }
                    }
                }

                tailscaleTestResult?.let { (success, msg) ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = if (success) Color(0xFF34D399) else Color(0xFFFCA5A5)
                    )
                }
            }
        }

        // Section 2: SSH/SFTP Connection Settings
        SettingsSectionHeader(title = "PENGATURAN KONEKSI DIRECT SSH / SFTP", icon = Icons.Default.Router)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Mode Simulasi Demo Offline",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = "Aktifkan jika belum terhubung ke server fisik",
                            style = MaterialTheme.typography.labelSmall,
                            color = BarraTextSecondary
                        )
                    }
                    Switch(
                        checked = isDemoMode,
                        onCheckedChange = {
                            isDemoMode = it
                            onUpdateConfig(config.copy(isDemoMode = it))
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = BarraCyanPrimary
                        ),
                        modifier = Modifier.testTag("demo_mode_switch")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Quick Connection Mode Presets
                Text(
                    text = "Preset Cepat Alamat Server:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = BarraCyanPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = {
                            host = "barra.ddns.net"
                            port = "22"
                            username = "barra"
                            password = "secretpassword"
                            isDemoMode = false
                            onUpdateConfig(config.copy(host = host, port = 22, username = username, password = password, isDemoMode = false))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B), contentColor = BarraCyanPrimary),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("DDNS / Public IP", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            host = "192.168.1.100"
                            port = "22"
                            username = "barra"
                            password = "secretpassword"
                            isDemoMode = false
                            onUpdateConfig(config.copy(host = host, port = 22, username = username, password = password, isDemoMode = false))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B), contentColor = Color(0xFF34D399)),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("IP Lokal / LAN", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    label = { Text("Host / IP Publik / Domain DDNS") },
                    placeholder = { Text("180.252.x.x atau barra.ddns.net") },
                    singleLine = true,
                    colors = outlinedFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_host")
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = port,
                        onValueChange = { port = it },
                        label = { Text("Port SSH") },
                        singleLine = true,
                        colors = outlinedFieldColors(),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_port")
                    )
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username SSH") },
                        singleLine = true,
                        colors = outlinedFieldColors(),
                        modifier = Modifier
                            .weight(2f)
                            .testTag("input_username")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password / Private Key") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    colors = outlinedFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_password")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = mediaRoot,
                    onValueChange = { mediaRoot = it },
                    label = { Text("MEDIA_ROOT Server Path") },
                    placeholder = { Text("/mnt/exthdd") },
                    singleLine = true,
                    colors = outlinedFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_media_root")
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val parsedPort = port.toIntOrNull() ?: 22
                            val updated = config.copy(
                                host = host,
                                port = parsedPort,
                                username = username,
                                password = password,
                                mediaRoot = mediaRoot,
                                isDemoMode = isDemoMode,
                                useTailscale = useTailscale,
                                tailscaleAuthKey = tailscaleAuthKey,
                                tailscaleIp = tailscaleIp,
                                tailscaleNodeName = tailscaleNodeName
                            )
                            onUpdateConfig(updated)
                            isTestingConnection = true
                            connectionTestResult = null
                            onTestConnection { success, message ->
                                isTestingConnection = false
                                connectionTestResult = Pair(success, message)
                            }
                        },
                        enabled = !isTestingConnection,
                        colors = ButtonDefaults.buttonColors(containerColor = BarraCyanPrimary, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("test_connection_button")
                    ) {
                        if (isTestingConnection) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.Black,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Menguji SFTP...", fontWeight = FontWeight.Bold)
                        } else {
                            Text("Test Connection", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (isTestingConnection) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E293B))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = BarraCyanPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Menguji Handshake SSH & SFTP...",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White
                        )
                    }
                } else {
                    connectionTestResult?.let { (success, message) ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (success) Color(0xFF064E3B) else Color(0xFF7F1D1D))
                                .border(1.dp, if (success) Color(0xFF10B981) else Color(0xFFEF4444), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (success) Icons.Default.CheckCircle else Icons.Default.Error,
                                    contentDescription = null,
                                    tint = if (success) Color(0xFF34D399) else Color(0xFFFCA5A5),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = message,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 3: App Cache Management
        SettingsSectionHeader(title = "MANAJEMEN CACHE APLIKASI (LRU)", icon = Icons.Default.Storage)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Kapasitas Cache Saat Ini",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text = formattedCacheSize,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = BarraCyanPrimary,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                    Text(
                        text = "Batas maksimum: 200 MB (LRU Eviction Engine)",
                        style = MaterialTheme.typography.labelSmall,
                        color = BarraTextSecondary
                    )
                }

                Button(
                    onClick = onClearCache,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF991B1B), contentColor = Color.White),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("clear_cache_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CleaningServices,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Hapus Cache")
                }
            }
        }

        // Section 4: Grid Thumbnail Layout Settings
        SettingsSectionHeader(title = "PENGATURAN GRID THUMBNAIL", icon = Icons.Default.GridView)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Jumlah Kolom Grid",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text = "$gridColumns Kolom",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = BarraCyanPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = gridColumns.toFloat(),
                    onValueChange = { onSetGridColumns(it.toInt()) },
                    valueRange = 2f..6f,
                    steps = 3,
                    colors = SliderDefaults.colors(
                        thumbColor = BarraCyanPrimary,
                        activeTrackColor = BarraCyanPrimary,
                        inactiveTrackColor = Color(0xFF334155)
                    ),
                    modifier = Modifier.testTag("grid_columns_slider")
                )
            }
        }

        // Section 5: App Theme
        SettingsSectionHeader(title = "TEMA APLIKASI", icon = Icons.Default.Palette)

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FilterChip(
                selected = themeMode == AppThemeMode.DARK,
                onClick = { onSetThemeMode(AppThemeMode.DARK) },
                label = { Text("Dark Theme (#0D1117)") },
                colors = chipColors(),
                modifier = Modifier.testTag("theme_dark_chip")
            )
            FilterChip(
                selected = themeMode == AppThemeMode.OLED,
                onClick = { onSetThemeMode(AppThemeMode.OLED) },
                label = { Text("OLED Black (#000000)") },
                colors = chipColors(),
                modifier = Modifier.testTag("theme_oled_chip")
            )
        }

        // Section 6: User Help & Terminal Commands
        SettingsSectionHeader(title = "BANTUAN & PANDUAN SERVER", icon = Icons.Default.HelpOutline)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Logika Backend Node.js & FFmpeg .thumbs SHA-1:",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = BarraCyanPrimary
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "1. SHA-1 Digest kalkulasi relative path misal: SHA1('/Foto Keluarga/IMG_001.jpg') -> a1b2c3...\n" +
                            "2. Thumbnail tersimpan di /mnt/exthdd/.thumbs/a1b2c3....jpg\n" +
                            "3. Perintah FFmpeg generate thumbnail di server Linux:",
                    style = MaterialTheme.typography.bodySmall,
                    color = BarraTextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF0A0E14))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "ffmpeg -i input.mp4 -ss 00:00:02 -vframes 1 /mnt/exthdd/.thumbs/\$HASH.jpg",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        ),
                        color = Color(0xFF4ADE80)
                    )
                }
            }
        }

        // Section 7: Realtime Terminal Log (Paling Bawah Layar Setting)
        SettingsSectionHeader(title = "REALTIME TERMINAL LOG CONSOLE", icon = Icons.Default.Terminal)

        TerminalLogView(
            logs = logs,
            onClearLogs = onClearLogs,
            modifier = Modifier.testTag("settings_terminal_log_widget")
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SettingsSectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = BarraCyanPrimary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            ),
            color = BarraCyanPrimary
        )
    }
}

@Composable
private fun outlinedFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = BarraCyanPrimary,
    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
    focusedLabelColor = BarraCyanPrimary,
    unfocusedLabelColor = BarraTextSecondary,
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White
)

@Composable
private fun chipColors() = FilterChipDefaults.filterChipColors(
    containerColor = MaterialTheme.colorScheme.surface,
    labelColor = BarraTextSecondary,
    selectedContainerColor = BarraCyanPrimary.copy(alpha = 0.2f),
    selectedLabelColor = BarraCyanPrimary
)
