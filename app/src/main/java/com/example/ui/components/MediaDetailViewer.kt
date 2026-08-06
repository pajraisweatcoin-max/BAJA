package com.example.ui.components

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.MediaController
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.MediaItem

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaDetailViewer(
    mediaList: List<MediaItem>,
    initialIndex: Int,
    authCookie: String?,
    getMediaUrl: (MediaItem) -> String,
    getThumbUrl: (MediaItem) -> String = { "" },
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Intercept system back button to close viewer without exiting the app
    BackHandler {
        onDismiss()
    }

    val safeList = if (mediaList.isEmpty()) emptyList() else mediaList
    val context = LocalContext.current
    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, (safeList.size - 1).coerceAtLeast(0)),
        pageCount = { safeList.size }
    )

    var showOverlay by remember { mutableStateOf(true) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (safeList.isNotEmpty()) {
            val currentItem = safeList[pagerState.currentPage]

            HorizontalPager(
                state = pagerState,
                userScrollEnabled = true,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val item = safeList[page]
                val mediaUrl = getMediaUrl(item)
                val thumbUrl = getThumbUrl(item)

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (item.isImage) {
                        ZoomableImage(
                            item = item,
                            imageUrl = mediaUrl,
                            thumbUrl = thumbUrl,
                            authCookie = authCookie,
                            contentDescription = item.name,
                            onToggleOverlay = { showOverlay = !showOverlay }
                        )
                    } else if (item.isVideo) {
                        VideoPlayerWithProgress(
                            mediaUrl = mediaUrl,
                            authCookie = authCookie,
                            onToggleOverlay = { showOverlay = !showOverlay }
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .clickable { showOverlay = !showOverlay },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.size(16.dp))
                            Text(
                                text = "Pratinjau tidak tersedia untuk file ini",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }

            // Top Header Navigation Bar & Counter Overlay
            AnimatedVisibility(
                visible = showOverlay,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 8.dp, vertical = 12.dp)
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Tutup Media Viewer",
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentItem.name,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${pagerState.currentPage + 1} / ${safeList.size} • ${currentItem.path}",
                            color = Color.White.copy(alpha = 0.75f),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(onClick = {
                        val downloadUrl = getMediaUrl(currentItem)
                        try {
                            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                            val request = DownloadManager.Request(Uri.parse(downloadUrl))
                                .setTitle(currentItem.name)
                                .setDescription("Mengunduh dari Barra Cloud...")
                                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, currentItem.name)
                                .setAllowedOverMetered(true)
                                .setAllowedOverRoaming(true)

                            if (!authCookie.isNullOrEmpty()) {
                                request.addRequestHeader("Cookie", "barra_auth=$authCookie")
                            }

                            downloadManager.enqueue(request)
                            Toast.makeText(context, "Mengunduh ${currentItem.name} ke folder Downloads...", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Gagal mengunduh: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Unduh File",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoPlayerWithProgress(
    mediaUrl: String,
    authCookie: String?,
    onToggleOverlay: () -> Unit
) {
    var isPrepared by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(true) }
    var videoPercent by remember { mutableIntStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onToggleOverlay() },
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                VideoView(ctx).apply {
                    val uri = Uri.parse(mediaUrl)
                    val headers = HashMap<String, String>()
                    if (!authCookie.isNullOrEmpty()) {
                        headers["Cookie"] = "barra_auth=$authCookie"
                    }
                    setVideoURI(uri, headers)

                    val controller = MediaController(ctx)
                    controller.setAnchorView(this)
                    setMediaController(controller)

                    setOnPreparedListener { mp ->
                        isPrepared = true
                        isBuffering = false
                        mp.isLooping = true
                        mp.start()
                        mp.setOnBufferingUpdateListener { _, percent ->
                            videoPercent = percent
                        }
                    }

                    setOnInfoListener { _, what, _ ->
                        if (what == android.media.MediaPlayer.MEDIA_INFO_BUFFERING_START) {
                            isBuffering = true
                        } else if (what == android.media.MediaPlayer.MEDIA_INFO_BUFFERING_END) {
                            isBuffering = false
                        }
                        false
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (!isPrepared || isBuffering) {
            Box(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.75f), shape = RoundedCornerShape(12.dp))
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (videoPercent > 0) "Memuat video $videoPercent%" else "Memuat video...",
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

@Composable
private fun ZoomableImage(
    item: MediaItem,
    imageUrl: String,
    thumbUrl: String,
    authCookie: String?,
    contentDescription: String,
    onToggleOverlay: () -> Unit
) {
    val context = LocalContext.current
    var scale by remember(imageUrl) { mutableFloatStateOf(1f) }
    var offset by remember(imageUrl) { mutableStateOf(Offset.Zero) }
    var isFullLoaded by remember(imageUrl) { mutableStateOf(false) }
    var loadProgress by remember(imageUrl) { mutableIntStateOf(15) }

    LaunchedEffect(imageUrl, isFullLoaded) {
        if (!isFullLoaded) {
            loadProgress = 15
            while (!isFullLoaded && loadProgress < 95) {
                delay(100)
                loadProgress = (loadProgress + (3..8).random()).coerceAtMost(95)
            }
        } else {
            loadProgress = 100
        }
    }

    var boxModifier = Modifier
        .fillMaxSize()
        .pointerInput(imageUrl) {
            detectTapGestures(
                onTap = { onToggleOverlay() },
                onDoubleTap = {
                    if (scale > 1f) {
                        scale = 1f
                        offset = Offset.Zero
                    } else {
                        scale = 2.5f
                    }
                }
            )
        }

    if (scale > 1f) {
        boxModifier = boxModifier.pointerInput(imageUrl, scale) {
            detectTransformGestures { _, pan, zoom, _ ->
                scale = (scale * zoom).coerceIn(1f, 5f)
                if (scale > 1f) {
                    offset += pan
                } else {
                    offset = Offset.Zero
                }
            }
        }
    }

    Box(
        modifier = boxModifier,
        contentAlignment = Alignment.Center
    ) {
        // High Resolution Full Image
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(imageUrl)
                .apply {
                    if (!authCookie.isNullOrEmpty()) {
                        addHeader("Cookie", "barra_auth=$authCookie")
                    }
                }
                .crossfade(true)
                .build(),
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            onSuccess = {
                isFullLoaded = true
                loadProgress = 100
            },
            onError = {
                isFullLoaded = true
                loadProgress = 100
            },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
        )

        // Loading Percentage Overlay badge
        if (!isFullLoaded) {
            Box(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.75f), shape = RoundedCornerShape(12.dp))
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Memuat foto $loadProgress%",
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

