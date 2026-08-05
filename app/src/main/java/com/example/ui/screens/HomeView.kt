package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MediaItem
import com.example.ui.components.MediaItemCard
import com.example.ui.theme.BarraCyanPrimary
import com.example.ui.theme.BarraTextSecondary
import com.example.util.CacheManager

@Composable
fun HomeView(
    mediaList: List<MediaItem>,
    gridColumnCount: Int,
    isRefreshing: Boolean,
    cacheManager: CacheManager,
    onMediaClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    // Group media by formatted date string (e.g. "26 November 2025")
    val groupedMedia = remember(mediaList) {
        mediaList.groupBy { it.formattedDate }
    }

    if (isRefreshing && mediaList.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = BarraCyanPrimary)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Menghubungkan ke SFTP Server & Generasi Hash SHA-1...",
                    style = MaterialTheme.typography.bodyMedium,
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
            .testTag("home_feed_grid")
    ) {
        // Top Banner Info
        item(span = { GridItemSpan(gridColumnCount) }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF161B22))
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(BarraCyanPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = null,
                            tint = BarraCyanPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "MEDIA RECENT SERVER FEED",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = BarraCyanPrimary
                        )
                        Text(
                            text = "Streaming media tanpa menyita memori telepon. Preview via .thumbs SHA-1.",
                            style = MaterialTheme.typography.bodySmall,
                            color = BarraTextSecondary
                        )
                    }
                }
            }
        }

        // Date Grouped Sections
        groupedMedia.forEach { (dateHeader, items) ->
            item(span = { GridItemSpan(gridColumnCount) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = BarraCyanPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = dateHeader,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "(${items.size} media)",
                        style = MaterialTheme.typography.labelMedium,
                        color = BarraTextSecondary
                    )
                }
            }

            items(items, key = { it.id }) { item ->
                MediaItemCard(
                    item = item,
                    cacheManager = cacheManager,
                    onClick = { onMediaClick(item) }
                )
            }
        }
    }
}
