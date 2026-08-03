package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.FileType
import com.example.core.model.MediaItem
import com.example.core.model.SortType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilesScreen(
    files: List<MediaItem>,
    isSyncing: Boolean,
    onFileClick: (MediaItem) -> Unit,
    onDeleteFile: (MediaItem) -> Unit,
    onRenameFile: (MediaItem, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var sortType by remember { mutableStateOf(SortType.NAME) }
    var selectedItemForAction by remember { mutableStateOf<MediaItem?>(null) }
    var renameTargetItem by remember { mutableStateOf<MediaItem?>(null) }
    var renameNewNameText by remember { mutableStateOf("") }

    // Filter out files/folders starting with '.' like .thumb cache
    val filteredFiles = remember(files, sortType) {
        val cleanList = files.filter { !it.name.startsWith(".") }
        when (sortType) {
            SortType.NAME -> cleanList.sortedBy { it.name.lowercase() }
            SortType.DATE -> cleanList.sortedByDescending { it.lastModified }
            SortType.SIZE -> cleanList.sortedByDescending { it.sizeBytes }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Sorting Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${filteredFiles.size} items",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    SortType.entries.forEach { option ->
                        FilterChip(
                            selected = (option == sortType),
                            onClick = { sortType = option },
                            label = { Text(option.name, fontSize = 11.sp) }
                        )
                    }
                }
            }

            Divider()

            if (filteredFiles.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (isSyncing) "Syncing SMB Files..." else "No files found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredFiles, key = { it.id }) { item ->
                        FileListItem(
                            item = item,
                            onClick = { onFileClick(item) },
                            onMoreClick = { selectedItemForAction = item }
                        )
                    }
                }
            }
        }

        if (isSyncing) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            )
        }
    }

    // Action BottomSheet / Dialog
    if (selectedItemForAction != null) {
        val item = selectedItemForAction!!
        ModalBottomSheet(
            onDismissRequest = { selectedItemForAction = null }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = item.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1
                )
                Text(
                    text = "${item.formattedSize} • ${formatDate(item.lastModified)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Rename
                ListItem(
                    headlineContent = { Text("Rename") },
                    leadingContent = { Icon(Icons.Default.Edit, contentDescription = null) },
                    modifier = Modifier.clickable {
                        renameTargetItem = item
                        renameNewNameText = item.name
                        selectedItemForAction = null
                    }
                )

                // Delete
                ListItem(
                    headlineContent = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                    leadingContent = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    modifier = Modifier.clickable {
                        onDeleteFile(item)
                        selectedItemForAction = null
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Rename Dialog
    if (renameTargetItem != null) {
        AlertDialog(
            onDismissRequest = { renameTargetItem = null },
            title = { Text("Rename File") },
            text = {
                OutlinedTextField(
                    value = renameNewNameText,
                    onValueChange = { renameNewNameText = it },
                    singleLine = true,
                    label = { Text("New Name") }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        renameTargetItem?.let { target ->
                            if (renameNewNameText.isNotBlank()) {
                                onRenameFile(target, renameNewNameText)
                            }
                        }
                        renameTargetItem = null
                    }
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { renameTargetItem = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun FileListItem(
    item: MediaItem,
    onClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(getFileTypeColor(item.fileType).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getFileTypeIcon(item.fileType),
                    contentDescription = null,
                    tint = getFileTypeColor(item.fileType),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1
                )
                Text(
                    text = if (item.isFolder) "Folder" else "${item.formattedSize} • ${formatDate(item.lastModified)}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onMoreClick) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Actions",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun getFileTypeIcon(type: FileType): ImageVector {
    return when (type) {
        FileType.IMAGE -> Icons.Default.Image
        FileType.VIDEO -> Icons.Default.Movie
        FileType.AUDIO -> Icons.Default.MusicNote
        FileType.DOCUMENT -> Icons.Default.Description
        FileType.ARCHIVE -> Icons.Default.FolderZip
        FileType.CODE -> Icons.Default.Code
        FileType.FOLDER -> Icons.Default.Folder
        FileType.OTHER -> Icons.Default.InsertDriveFile
    }
}

private fun getFileTypeColor(type: FileType): Color {
    return when (type) {
        FileType.IMAGE -> Color(0xFF0EA5E9)
        FileType.VIDEO -> Color(0xFF8B5CF6)
        FileType.AUDIO -> Color(0xFFEC4899)
        FileType.DOCUMENT -> Color(0xFFF59E0B)
        FileType.ARCHIVE -> Color(0xFF10B981)
        FileType.CODE -> Color(0xFF6366F1)
        FileType.FOLDER -> Color(0xFF0284C7)
        FileType.OTHER -> Color(0xFF64748B)
    }
}

private fun formatDate(timestamp: Long): String {
    if (timestamp <= 0) return ""
    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
