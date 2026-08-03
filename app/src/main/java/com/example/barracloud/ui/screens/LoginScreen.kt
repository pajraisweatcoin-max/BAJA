package com.example.barracloud.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.barracloud.data.models.SmbCredentials
import com.example.barracloud.smb.SmbConnectionState

@Composable
fun LoginScreen(
    credentials: SmbCredentials,
    connectionState: SmbConnectionState,
    isLoading: Boolean,
    errorMessage: String?,
    onConnect: (SmbCredentials) -> Unit,
    onDisconnect: () -> Unit,
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    var server by remember(credentials) { mutableStateOf(credentials.server) }
    var port by remember(credentials) { mutableStateOf(credentials.port.toString()) }
    var shareName by remember(credentials) { mutableStateOf(credentials.shareName) }
    var domain by remember(credentials) { mutableStateOf(credentials.domain) }
    var username by remember(credentials) { mutableStateOf(credentials.username) }
    var password by remember(credentials) { mutableStateOf(credentials.password) }
    var smbVersion by remember(credentials) { mutableStateOf(credentials.smbVersion) }
    var rememberLogin by remember(credentials) { mutableStateOf(credentials.rememberLogin) }
    var autoConnect by remember(credentials) { mutableStateOf(credentials.autoConnect) }
    var showPassword by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // App Logo Header
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CloudQueue,
                contentDescription = "Logo",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "BARRA CLOUD",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = "Samba (SMB) Media Streamer & Viewer",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Connection Status Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = when (connectionState) {
                    is SmbConnectionState.Connected -> Color(0xFF065F46)
                    is SmbConnectionState.Connecting -> Color(0xFF1E3A8A)
                    is SmbConnectionState.Error -> Color(0xFF991B1B)
                    is SmbConnectionState.Disconnected -> MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isLoading || connectionState is SmbConnectionState.Connecting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }

                Text(
                    text = when (connectionState) {
                        is SmbConnectionState.Connected -> "Terhubung: ${connectionState.server}/${connectionState.share}"
                        is SmbConnectionState.Connecting -> "Menghubungkan ke server SMB..."
                        is SmbConnectionState.Error -> connectionState.message
                        is SmbConnectionState.Disconnected -> "Belum terhubung ke server SMB"
                    },
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )

                if (connectionState is SmbConnectionState.Connected) {
                    Button(
                        onClick = onLoginSuccess,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Buka Gallery", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (errorMessage != null && connectionState !is SmbConnectionState.Error) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = errorMessage,
                color = Color(0xFFEF4444),
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Form Fields
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Pengaturan Koneksi SMB",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Server IP & Port
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = server,
                        onValueChange = { server = it },
                        label = { Text("Server / IP Address") },
                        placeholder = { Text("192.168.1.100") },
                        leadingIcon = { Icon(Icons.Default.Dns, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(2f)
                            .testTag("input_server")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = port,
                        onValueChange = { port = it },
                        label = { Text("Port") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_port")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Shared Folder
                OutlinedTextField(
                    value = shareName,
                    onValueChange = { shareName = it },
                    label = { Text("Shared Folder") },
                    placeholder = { Text("Media / Photos") },
                    leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_share")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Username & Password
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username (opsional)") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_username")
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password (opsional)") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle Password"
                            )
                        }
                    },
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_password")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // SMB Protocol Version Selection
                Text(
                    text = "Protokol SMB",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        RadioButton(
                            selected = smbVersion == "SMB2",
                            onClick = { smbVersion = "SMB2" }
                        )
                        Text("SMB2", fontSize = 14.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = smbVersion == "SMB3",
                            onClick = { smbVersion = "SMB3" }
                        )
                        Text("SMB3", fontSize = 14.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Checkboxes
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = rememberLogin,
                        onCheckedChange = { rememberLogin = it }
                    )
                    Text("Remember Login", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(16.dp))
                    Checkbox(
                        checked = autoConnect,
                        onCheckedChange = { autoConnect = it }
                    )
                    Text("Auto Connect", fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val parsedPort = port.toIntOrNull() ?: 445
                            val creds = SmbCredentials(
                                server = server.trim(),
                                port = parsedPort,
                                shareName = shareName.trim(),
                                domain = domain.trim(),
                                username = username.trim(),
                                password = password,
                                smbVersion = smbVersion,
                                rememberLogin = rememberLogin,
                                autoConnect = autoConnect
                            )
                            onConnect(creds)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("connect_button"),
                        enabled = !isLoading && server.isNotBlank() && shareName.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Connect", fontWeight = FontWeight.Bold, color = Color.Black)
                    }

                    if (connectionState is SmbConnectionState.Connected) {
                        OutlinedButton(
                            onClick = onDisconnect,
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(Icons.Default.PowerSettingsNew, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Disconnect", color = Color(0xFFEF4444))
                        }
                    }
                }
            }
        }
    }
}
