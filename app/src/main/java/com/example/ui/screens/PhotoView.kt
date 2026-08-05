package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ImageSearch
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.MediaItem
import com.example.ui.components.MediaItemCard
import com.example.ui.theme.BarraCyanPrimary
import com.example.ui.theme.BarraTextSecondary
import com.example.util.CacheManager

@Composable
fun PhotoView(
    mediaList: List<MediaItem>,
    gridColumnCount: Int,
    cacheManager: CacheManager,
    onMediaClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val photoItems = remember(mediaList) {
        mediaList.filter { !it.isVideo }
    }

    if (photoItems.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Outlined.ImageSearch,
                    contentDescription = null,
                    tint = BarraTextSecondary,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Tidak ada foto di server",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Folder .thumbs disembunyikan secara otomatis.",
                    style = MaterialTheme.typography.bodySmall,
                    color = BarraTextSecondary
                )
            }
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(gridColumnCount),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxSize()
            .testTag("photo_gallery_grid")
    ) {
        items(photoItems, key = { it.id }) { item ->
            MediaItemCard(
                item = item,
                cacheManager = cacheManager,
                onClick = { onMediaClick(item) }
            )
        }
    }
}
