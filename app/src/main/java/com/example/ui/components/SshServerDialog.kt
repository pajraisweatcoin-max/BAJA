package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.db.SshServerEntity

@Composable
fun SshServerDialog(
    initialServer: SshServerEntity? = null,
    onDismiss: () -> Unit,
    onSave: (SshServerEntity) -> Unit
) {
    var name by remember { mutableStateOf(initialServer?.name ?: "") }
    var host by remember { mutableStateOf(initialServer?.host ?: "") }
    var portStr by remember { mutableStateOf(initialServer?.port?.toString() ?: "22") }
    var username by remember { mutableStateOf(initialServer?.username ?: "root") }
    var authType by remember { mutableStateOf(initialServer?.authType ?: "PASSWORD") }
    var password by remember { mutableStateOf(initialServer?.password ?: "") }
    var privateKey by remember { mutableStateOf(initialServer?.privateKey ?: "") }
    var passphrase by remember { mutableStateOf(initialServer?.passphrase ?: "") }
    var defaultPath by remember { mutableStateOf(initialServer?.defaultPath ?: "/") }

    var passwordVisible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (initialServer == null) "Tambah Profil Server SSH" else "Edit Profil Server SSH")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Name / Label
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Profil / Label") },
                    placeholder = { Text("misal: VPS Ubuntu / Server Prod") },
                    leadingIcon = { Icon(Icons.Default.Title, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().testTag("server_name_input"),
                    singleLine = true
                )

                // Host & Port
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = host,
                        onValueChange = { host = it },
                        label = { Text("Host / IP Address") },
                        placeholder = { Text("192.168.1.100 atau domain.com") },
                        leadingIcon = { Icon(Icons.Default.Dns, contentDescription = null) },
                        modifier = Modifier.weight(1f).testTag("server_host_input"),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = portStr,
                        onValueChange = { portStr = it },
                        label = { Text("Port") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(90.dp).testTag("server_port_input"),
                        singleLine = true
                    )
                }

                // Username
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    placeholder = { Text("root / ubuntu") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().testTag("server_user_input"),
                    singleLine = true
                )

                // Auth Method Selector
                Text(
                    text = "Metode Otentikasi:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = authType == "PASSWORD",
                        onClick = { authType = "PASSWORD" },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text("Kata Sandi")
                    }
                    SegmentedButton(
                        selected = authType == "PRIVATE_KEY",
                        onClick = { authType = "PRIVATE_KEY" },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text("Private Key")
                    }
                }

                if (authType == "PASSWORD") {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Kata Sandi SSH") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (passwordVisible) "Sembunyikan kata sandi" else "Tampilkan kata sandi"
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth().testTag("server_password_input"),
                        singleLine = true
                    )
                } else {
                    OutlinedTextField(
                        value = privateKey,
                        onValueChange = { privateKey = it },
                        label = { Text("Private Key (PEM / OpenSSH)") },
                        placeholder = { Text("-----BEGIN OPENSSH PRIVATE KEY-----\n...") },
                        leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth().height(120.dp).testTag("server_key_input"),
                        maxLines = 5
                    )
                    OutlinedTextField(
                        value = passphrase,
                        onValueChange = { passphrase = it },
                        label = { Text("Passphrase Key (Opsional)") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth().testTag("server_passphrase_input"),
                        singleLine = true
                    )
                }

                // Default Directory Path
                OutlinedTextField(
                    value = defaultPath,
                    onValueChange = { defaultPath = it },
                    label = { Text("Folder Awal Remote") },
                    placeholder = { Text("/ atau /home/username") },
                    leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().testTag("server_path_input"),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (host.isNotBlank() && name.isNotBlank() && username.isNotBlank()) {
                        val port = portStr.toIntOrNull() ?: 22
                        val entity = SshServerEntity(
                            id = initialServer?.id ?: 0L,
                            name = name.trim(),
                            host = host.trim(),
                            port = port,
                            username = username.trim(),
                            authType = authType,
                            password = password,
                            privateKey = privateKey.trim(),
                            passphrase = passphrase,
                            defaultPath = if (defaultPath.isBlank()) "/" else defaultPath.trim()
                        )
                        onSave(entity)
                    }
                },
                modifier = Modifier.testTag("save_server_button")
            ) {
                Text("Simpan Profil")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
