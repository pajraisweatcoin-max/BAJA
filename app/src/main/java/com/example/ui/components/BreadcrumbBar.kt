package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun BreadcrumbBar(
    currentPath: String,
    onNavigateTo: (String) -> Unit,
    onNavigateUp: () -> Unit
) {
    val scrollState = rememberScrollState()

    // Break down path into segments
    val segments = remember(currentPath) { currentPath.split("/").filter { it.isNotEmpty() } }

    LaunchedEffect(currentPath) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Up folder button
            IconButton(
                onClick = onNavigateUp,
                enabled = currentPath != "/" && currentPath.isNotEmpty(),
                modifier = Modifier.testTag("navigate_up_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Naik ke folder induk"
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Scrollable breadcrumbs
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(scrollState),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Root chip
                AssistChip(
                    onClick = { onNavigateTo("/") },
                    label = { Text("root") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Home,
                            contentDescription = "Root",
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = if (currentPath == "/") {
                        AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    } else {
                        AssistChipDefaults.assistChipColors()
                    }
                )

                var accumulatedPath = ""
                for (index in segments.indices) {
                    val segment = segments[index]
                    accumulatedPath += "/$segment"
                    val targetPath = accumulatedPath

                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )

                    val isLast = (index == segments.size - 1)
                    AssistChip(
                        onClick = { onNavigateTo(targetPath) },
                        label = { Text(segment) },
                        colors = if (isLast) {
                            AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        } else {
                            AssistChipDefaults.assistChipColors()
                        }
                    )
                }
            }
        }
    }
}
