package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.data.model.MediaItem

@Composable
fun ContextMenuDialog(
    item: MediaItem,
    hasClipboardItem: Boolean,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onPaste: () -> Unit,
    onMove: (String) -> Unit,
    onDelete: () -> Unit
) {
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var newNameText by remember { mutableStateOf(item.name) }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Ubah Nama File / Folder") },
            text = {
                OutlinedTextField(
                    value = newNameText,
                    onValueChange = { newNameText = it },
                    label = { Text("Nama Baru") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showRenameDialog = false
                    onMove(newNameText)
                    onDismiss()
                }) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Hapus ${item.name}?") },
            text = { Text("Apakah Anda yakin ingin menghapus file ini via Samba? Tindakan ini tidak dapat dibatalkan.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirmDialog = false
                    onDelete()
                    onDismiss()
                }) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.name, maxLines = 1) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                ContextMenuItem(
                    icon = Icons.Default.ContentCopy,
                    label = "Salin (Copy)",
                    onClick = {
                        onCopy()
                        onDismiss()
                    }
                )

                if (hasClipboardItem) {
                    Spacer(modifier = Modifier.height(4.dp))
                    ContextMenuItem(
                        icon = Icons.Default.ContentPaste,
                        label = "Tempel (Paste) di Sini",
                        onClick = {
                            onPaste()
                            onDismiss()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                ContextMenuItem(
                    icon = Icons.Default.DriveFileRenameOutline,
                    label = "Ubah Nama / Pindah (Rename/Move)",
                    onClick = {
                        showRenameDialog = true
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))
                ContextMenuItem(
                    icon = Icons.Default.Delete,
                    label = "Hapus File (Delete)",
                    tint = MaterialTheme.colorScheme.error,
                    onClick = {
                        showDeleteConfirmDialog = true
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup")
            }
        }
    )
}

@Composable
private fun ContextMenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp)
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = tint)
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = tint)
    }
}
