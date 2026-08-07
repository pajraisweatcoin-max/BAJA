package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.PowerOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.sftp.ConnectionState
import com.example.data.sftp.SftpFileItem
import com.example.ui.components.BreadcrumbBar
import com.example.ui.components.CreateItemDialog
import com.example.ui.components.DeleteConfirmDialog
import com.example.ui.components.RenameDialog
import com.example.viewmodel.SftpViewModel
import com.example.viewmodel.SortType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SftpExplorerScreen(
    viewModel: SftpViewModel,
    onNavigateToServers: () -> Unit
) {
    val context = LocalContext.current

    val connectionState by viewModel.connectionState.collectAsState()
    val activeServer by viewModel.activeServer.collectAsState()
    val currentPath by viewModel.currentPath.collectAsState()
    val fileList by viewModel.filteredFileList.collectAsState()
    val isLoadingFiles by viewModel.isLoadingFiles.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortType by viewModel.sortType.collectAsState()
    val clipboardItem by viewModel.clipboardItem.collectAsState()
    val selectedItems by viewModel.selectedItems.collectAsState()

    var showSearchField by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    var showCreateDialog by remember { mutableStateOf(false) }
    var isCreateFolder by remember { mutableStateOf(true) }

    var actionTargetItem by remember { mutableStateOf<SftpFileItem?>(null) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    var showFabMenu by remember { mutableStateOf(false) }

    var pendingDownloadItem by remember { mutableStateOf<SftpFileItem?>(null) }

    // Upload launcher
    val uploadPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (!uris.isNullOrEmpty()) {
            uris.forEach { uri ->
                viewModel.uploadFileFromUri(context, uri)
            }
        }
    }

    // Download launcher
    val downloadSaveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri: Uri? ->
        val item = pendingDownloadItem
        if (uri != null && item != null) {
            viewModel.downloadFileToLocal(context, item, uri)
        }
        pendingDownloadItem = null
    }

    val sheetState = rememberModalBottomSheetState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = activeServer?.name ?: "SFTP File Manager",
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val (statusText, statusColor) = when (connectionState) {
                                ConnectionState.CONNECTED -> "Terhubung" to MaterialTheme.colorScheme.primary
                                ConnectionState.CONNECTING -> "Menghubungkan..." to MaterialTheme.colorScheme.secondary
                                ConnectionState.DISCONNECTED -> "Terputus" to MaterialTheme.colorScheme.error
                                ConnectionState.ERROR -> "Gagal" to MaterialTheme.colorScheme.error
                            }
                            Surface(
                                color = statusColor.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = statusText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = statusColor,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                            if (activeServer != null) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${activeServer?.username}@${activeServer?.host}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showSearchField = !showSearchField }) {
                        Icon(Icons.Default.Search, contentDescription = "Cari Berkas")
                    }

                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Default.Sort, contentDescription = "Urutkan")
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Nama (A - Z)") },
                                onClick = {
                                    viewModel.setSortType(SortType.NAME_ASC)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Nama (Z - A)") },
                                onClick = {
                                    viewModel.setSortType(SortType.NAME_DESC)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Ukuran (Terkecil)") },
                                onClick = {
                                    viewModel.setSortType(SortType.SIZE_ASC)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Ukuran (Terbesar)") },
                                onClick = {
                                    viewModel.setSortType(SortType.SIZE_DESC)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Tanggal (Terbaru)") },
                                onClick = {
                                    viewModel.setSortType(SortType.DATE_NEWEST)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Tanggal (Terlama)") },
                                onClick = {
                                    viewModel.setSortType(SortType.DATE_OLDEST)
                                    showSortMenu = false
                                }
                            )
                        }
                    }

                    IconButton(onClick = { viewModel.refreshFiles() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Muat Ulang")
                    }

                    if (connectionState == ConnectionState.CONNECTED) {
                        IconButton(onClick = { viewModel.disconnect() }) {
                            Icon(
                                Icons.Default.PowerOff,
                                contentDescription = "Putuskan",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            if (connectionState == ConnectionState.CONNECTED) {
                Column(horizontalAlignment = Alignment.End) {
                    AnimatedVisibility(visible = showFabMenu) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SmallFloatingActionButton(
                                onClick = {
                                    showFabMenu = false
                                    uploadPickerLauncher.launch("*/*")
                                },
                                modifier = Modifier.testTag("upload_fab")
                            ) {
                                Row(modifier = Modifier.padding(horizontal = 8.dp)) {
                                    Icon(Icons.Default.UploadFile, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Unggah Berkas")
                                }
                            }

                            SmallFloatingActionButton(
                                onClick = {
                                    showFabMenu = false
                                    isCreateFolder = true
                                    showCreateDialog = true
                                },
                                modifier = Modifier.testTag("new_folder_fab")
                            ) {
                                Row(modifier = Modifier.padding(horizontal = 8.dp)) {
                                    Icon(Icons.Default.CreateNewFolder, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Folder Baru")
                                }
                            }

                            SmallFloatingActionButton(
                                onClick = {
                                    showFabMenu = false
                                    isCreateFolder = false
                                    showCreateDialog = true
                                },
                                modifier = Modifier.testTag("new_file_fab")
                            ) {
                                Row(modifier = Modifier.padding(horizontal = 8.dp)) {
                                    Icon(Icons.Default.NoteAdd, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Berkas Baru")
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    FloatingActionButton(
                        onClick = { showFabMenu = !showFabMenu },
                        modifier = Modifier.testTag("main_action_fab")
                    ) {
                        Icon(
                            if (showFabMenu) Icons.Default.Close else Icons.Default.Add,
                            contentDescription = "Menu Aksi"
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Bar Filter
            AnimatedVisibility(visible = showSearchField) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Filter nama berkas di folder ini...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("explorer_search_input"),
                    singleLine = true
                )
            }

            // Breadcrumb Folder Path
            BreadcrumbBar(
                currentPath = currentPath,
                onNavigateTo = { viewModel.navigateTo(it) },
                onNavigateUp = { viewModel.navigateUp() }
            )

            // Selection Bar (if items selected)
            if (selectedItems.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${selectedItems.size} item terpilih",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        Row {
                            if (selectedItems.size == 1) {
                                val item = selectedItems.first()
                                IconButton(onClick = {
                                    viewModel.copyToClipboard(item, isCutMode = false)
                                    viewModel.clearSelection()
                                }) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Salin")
                                }
                                IconButton(onClick = {
                                    viewModel.copyToClipboard(item, isCutMode = true)
                                    viewModel.clearSelection()
                                }) {
                                    Icon(Icons.Default.ContentCut, contentDescription = "Potong")
                                }
                            }
                            IconButton(onClick = {
                                showDeleteDialog = true
                            }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Hapus Terpilih",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                            TextButton(onClick = { viewModel.clearSelection() }) {
                                Text("Batal")
                            }
                        }
                    }
                }
            }

            // Clipboard Action Banner
            if (clipboardItem != null && selectedItems.isEmpty()) {
                val clip = clipboardItem!!
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                if (clip.isCutMode) Icons.Default.ContentCut else Icons.Default.ContentCopy,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${clip.fileName} (${if (clip.isCutMode) "Pindah" else "Salin"})",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }

                        Row {
                            Button(
                                onClick = { viewModel.pasteFromClipboard() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.testTag("paste_button")
                            ) {
                                Icon(Icons.Default.ContentPaste, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Tempel Di Sini")
                            }
                            IconButton(onClick = { viewModel.clearClipboard() }) {
                                Icon(Icons.Default.Close, contentDescription = "Batal Clipboard")
                            }
                        }
                    }
                }
            }

            // Content Body
            if (connectionState != ConnectionState.CONNECTED) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.PowerOff,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Sesi SSH Terputus",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Silakan hubungkan kembali ke server SSH dari menu daftar server.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onNavigateToServers,
                            modifier = Modifier.testTag("back_to_servers_button")
                        ) {
                            Text("Buka Daftar Server")
                        }
                    }
                }
            } else if (isLoadingFiles) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Memuat berkas dari server SSH...")
                    }
                }
            } else if (fileList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "Berkas tidak ditemukan" else "Folder ini kosong",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = {
                            uploadPickerLauncher.launch("*/*")
                        }) {
                            Icon(Icons.Default.UploadFile, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Unggah Berkas Ke Sini")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(fileList, key = { it.path }) { item ->
                        val isSelected = selectedItems.contains(item)
                        FileItemRow(
                            item = item,
                            isSelected = isSelected,
                            isInSelectionMode = selectedItems.isNotEmpty(),
                            onClick = {
                                if (selectedItems.isNotEmpty()) {
                                    viewModel.toggleSelection(item)
                                } else {
                                    if (item.isDirectory) {
                                        viewModel.navigateTo(item.path)
                                    } else if (item.isTextEditable) {
                                        viewModel.openTextFile(item)
                                    } else {
                                        actionTargetItem = item
                                    }
                                }
                            },
                            onLongClick = {
                                viewModel.toggleSelection(item)
                            },
                            onMoreClick = {
                                actionTargetItem = item
                            }
                        )
                    }
                }
            }
        }
    }

    // Action Bottom Sheet for selected file item
    if (actionTargetItem != null) {
        val target = actionTargetItem!!
        ModalBottomSheet(
            onDismissRequest = { actionTargetItem = null },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Icon(
                        imageVector = getFileIcon(target),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = target.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${target.formattedSize} • ${target.permissions} • ${target.formattedDate}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (!target.isDirectory) {
                    if (target.isTextEditable) {
                        DropdownMenuItem(
                            text = { Text("Edit Teks Remote") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                val itemToEdit = target
                                actionTargetItem = null
                                viewModel.openTextFile(itemToEdit)
                            }
                        )
                    }

                    DropdownMenuItem(
                        text = { Text("Unduh Ke HP") },
                        leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) },
                        onClick = {
                            val itemToDownload = target
                            actionTargetItem = null
                            pendingDownloadItem = itemToDownload
                            downloadSaveLauncher.launch(itemToDownload.name)
                        }
                    )
                }

                DropdownMenuItem(
                    text = { Text("Salin (Copy)") },
                    leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                    onClick = {
                        val itemToCopy = target
                        actionTargetItem = null
                        viewModel.copyToClipboard(itemToCopy, isCutMode = false)
                    }
                )

                DropdownMenuItem(
                    text = { Text("Potong (Move/Cut)") },
                    leadingIcon = { Icon(Icons.Default.ContentCut, contentDescription = null) },
                    onClick = {
                        val itemToCut = target
                        actionTargetItem = null
                        viewModel.copyToClipboard(itemToCut, isCutMode = true)
                    }
                )

                DropdownMenuItem(
                    text = { Text("Ubah Nama (Rename)") },
                    leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null) },
                    onClick = {
                        showRenameDialog = true
                    }
                )

                DropdownMenuItem(
                    text = { Text("Hapus Permanen", color = MaterialTheme.colorScheme.error) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    },
                    onClick = {
                        showDeleteDialog = true
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Create Folder/File Dialog
    if (showCreateDialog) {
        CreateItemDialog(
            isFolder = isCreateFolder,
            onDismiss = { showCreateDialog = false },
            onCreate = { name, content ->
                if (isCreateFolder) {
                    viewModel.createFolder(name)
                } else {
                    viewModel.createFile(name, content)
                }
                showCreateDialog = false
            }
        )
    }

    // Rename Dialog
    if (showRenameDialog && actionTargetItem != null) {
        val target = actionTargetItem!!
        RenameDialog(
            currentName = target.name,
            onDismiss = { showRenameDialog = false },
            onRename = { newName ->
                showRenameDialog = false
                actionTargetItem = null
                viewModel.renameItem(target, newName)
            }
        )
    }

    // Delete Dialog
    if (showDeleteDialog) {
        val isTargetAction = actionTargetItem != null
        val count = if (isTargetAction) 1 else if (selectedItems.isNotEmpty()) selectedItems.size else 1
        val targetName = actionTargetItem?.name ?: selectedItems.firstOrNull()?.name ?: ""
        DeleteConfirmDialog(
            itemName = targetName,
            count = count,
            onDismiss = {
                showDeleteDialog = false
                actionTargetItem = null
            },
            onConfirm = {
                showDeleteDialog = false
                if (actionTargetItem != null) {
                    val itemToDelete = actionTargetItem!!
                    actionTargetItem = null
                    viewModel.deleteItem(itemToDelete)
                } else if (selectedItems.isNotEmpty()) {
                    viewModel.deleteSelectedItems()
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileItemRow(
    item: SftpFileItem,
    isSelected: Boolean,
    isInSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
            else MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("file_item_${item.name}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isInSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }

            Icon(
                imageVector = getFileIcon(item),
                contentDescription = null,
                tint = if (item.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.formattedSize,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "• ${item.permissions}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "• ${item.formattedDate}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1
                    )
                }
            }

            IconButton(onClick = onMoreClick) {
                Icon(Icons.Default.MoreVert, contentDescription = "Pilihan berkas")
            }
        }
    }
}

fun getFileIcon(item: SftpFileItem): ImageVector {
    if (item.isDirectory) return Icons.Default.Folder
    val ext = item.fileExtension
    return when (ext) {
        "sh", "bash", "py", "js", "ts", "kt", "java", "c", "cpp", "html", "css", "json", "xml", "yml" -> Icons.Default.Terminal
        "txt", "md", "log", "conf", "ini", "env" -> Icons.Default.Edit
        else -> Icons.AutoMirrored.Filled.InsertDriveFile
    }
}
