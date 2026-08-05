package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MediaItem
import com.example.ui.theme.BarraCyanPrimary
import com.example.util.CacheManager

@Composable
fun MediaItemCard(
    item: MediaItem,
    cacheManager: CacheManager,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cachedBitmap: Bitmap? = remember(item.sha1Hash) {
        cacheManager.getThumbnail(item.sha1Hash)
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .testTag("media_card_${item.sha1Hash.take(8)}")
    ) {
        // Thumbnail Image or Fallback Visual
        if (cachedBitmap != null) {
            Image(
                bitmap = cachedBitmap.asImageBitmap(),
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Elegant gradient placeholder when SHA-1 thumbnail is loaded via stream
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = if (item.isVideo) listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                            else listOf(Color(0xFF162032), Color(0xFF0D131F))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (item.isVideo) Icons.Default.Videocam else Icons.Outlined.Image,
                    contentDescription = null,
                    tint = BarraCyanPrimary.copy(alpha = 0.4f),
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        // Gradient overlay at bottom for readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.2f),
                            Color.Black.copy(alpha = 0.85f)
                        ),
                        startY = 60f
                    )
                )
        )

        // Center Big Play Icon overlay for Videos
        if (item.isVideo) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .border(1.5.dp, BarraCyanPrimary, CircleShape)
                    .align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play Video",
                    tint = Color.White,
                    modifier = Modifier
                        .size(28.dp)
                        .padding(start = 2.dp)
                )
            }
        }

        // Bottom Details: Name & Size & Media Type Badge
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                // Badge indikator media di pojok kiri bawah
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (item.isVideo) BarraCyanPrimary.copy(alpha = 0.85f)
                            else Color(0xFF334155).copy(alpha = 0.85f)
                        )
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = if (item.isVideo) "VIDEO" else "FOTO",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = item.formattedSize,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}
