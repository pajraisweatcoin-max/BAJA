package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.MediaItem
import com.example.ui.components.ContextMenuDialog

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileManagerScreen(
    currentPath: String,
    items: List<MediaItem>,
    isLoading: Boolean,
    isSftpConnected: Boolean,
    adminModeSftp: Boolean,
    hasClipboardItem: Boolean,
    onNavigateDirectory: (String) -> Unit,
    onNavigateUp: () -> Unit,
    onMediaItemClick: (MediaItem) -> Unit,
    onUploadFile: (Uri, String) -> Unit,
    onCopyItem: (MediaItem) -> Unit,
    onPasteItem: () -> Unit,
    onMoveItem: (MediaItem, String) -> Unit,
    onDeleteItem: (MediaItem) -> Unit,
    onConnectClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedContextItem by remember { mutableStateOf<MediaItem?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = uri.lastPathSegment?.substringAfterLast("/") ?: "uploaded_file"
            onUploadFile(uri, fileName)
        }
    }

    if (selectedContextItem != null && isSftpConnected) {
        ContextMenuDialog(
            item = selectedContextItem!!,
            hasClipboardItem = hasClipboardItem,
            adminModeSftp = adminModeSftp,
            onDismiss = { selectedContextItem = null },
            onCopy = { onCopyItem(selectedContextItem!!) },
            onPaste = onPasteItem,
            onMove = { newName -> onMoveItem(selectedContextItem!!, newName) },
            onDelete = { onDeleteItem(selectedContextItem!!) }
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Path Navigation Breadcrumb Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                if (currentPath != "/") {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Kembali ke Folder Atas"
                        )
                    }
                }

                Text(
                    text = currentPath,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // SFTP Mode Status Tag
                val modeLabel = when {
                    isSftpConnected && adminModeSftp -> "SFTP (Admin Mode)"
                    isSftpConnected -> "SFTP (User Mode)"
                    else -> "HTTP Read-Only"
                }
                val modeBg = when {
                    isSftpConnected && adminModeSftp -> Color(0xFFDC2626)
                    isSftpConnected -> Color(0xFF16A34A)
                    else -> Color(0xFF6B7280)
                }

                Text(
                    text = modeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(modeBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (!isSftpConnected && items.isEmpty()) {
                // If SFTP is not connected and no items are available, show connect banner
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderSpecial,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Koneksi Server SFTP Terputus",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Hubungkan ke server SFTP untuk mengelola dan membaca direktori /",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            androidx.compose.material3.Button(
                                onClick = onConnectClick
                            ) {
                                Text("Hubungkan SFTP Sekarang")
                            }
                        }
                    }
                }
            } else if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderSpecial,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (!isSftpConnected) "Hubungkan ke server untuk menggunakan file manager" else "Folder Ini Kosong",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 80.dp)
                ) {
                    items(items) { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .combinedClickable(
                                    onClick = {
                                        if (item.isDir) {
                                            onNavigateDirectory(item.path)
                                        } else {
                                            onMediaItemClick(item)
                                        }
                                    },
                                    onLongClick = {
                                        if (isSftpConnected) {
                                            selectedContextItem = item
                                        }
                                    }
                                ),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                val itemIcon = when {
                                    item.name == ".thumbs" -> Icons.Default.FolderSpecial
                                    item.isDir -> Icons.Default.Folder
                                    item.isImage -> Icons.Default.Image
                                    item.isVideo -> Icons.Default.Movie
                                    else -> Icons.Default.Description
                                }

                                val iconTint = when {
                                    item.name == ".thumbs" -> Color(0xFFEC4899)
                                    item.isDir -> Color(0xFFEAB308)
                                    item.isImage -> Color(0xFF06B6D4)
                                    item.isVideo -> Color(0xFF8B5CF6)
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }

                                Icon(
                                    imageVector = itemIcon,
                                    contentDescription = null,
                                    tint = iconTint,
                                    modifier = Modifier.size(32.dp)
                                )

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val subtext = if (item.isDir) "Folder" else formatFileSize(item.size)
                                    Text(
                                        text = subtext,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (isSftpConnected) {
                                    IconButton(onClick = { selectedContextItem = item }) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = "Menu Opsi"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Floating Action Button (FAB Upload +) in SFTP Mode
        if (isSftpConnected) {
            FloatingActionButton(
                onClick = { filePickerLauncher.launch("*/*") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 88.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Unggah File Baru via SFTP"
                )
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format("%.2f GB", gb)
        mb >= 1.0 -> String.format("%.1f MB", mb)
        kb >= 1.0 -> String.format("%.0f KB", kb)
        else -> "$bytes B"
    }
}
