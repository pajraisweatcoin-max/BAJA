package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.SpeedTestPhase
import com.example.engine.SpeedTestProgress
import com.example.model.TailnetNode
import com.example.model.TailscaleEngineState
import com.example.ui.components.SpeedChart
import com.example.ui.components.SpeedometerGauge
import com.example.ui.components.TailscaleStatusCard
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCard
import com.example.ui.theme.CyberDarkSurface
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonYellow
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun SpeedTestScreen(
    engineState: TailscaleEngineState,
    progress: SpeedTestProgress,
    selectedNode: TailnetNode,
    peersList: List<TailnetNode>,
    onSelectNode: (TailnetNode) -> Unit,
    onStartTest: () -> Unit,
    onCancelTest: () -> Unit,
    onUpdateAuthKey: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAuthDialog by remember { mutableStateOf(false) }
    var showPeerPicker by remember { mutableStateOf(false) }

    val isRunning = progress.phase == SpeedTestPhase.PINGING ||
            progress.phase == SpeedTestPhase.DOWNLOADING ||
            progress.phase == SpeedTestPhase.UPLOADING

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // 1. Tailscale Status Banner
        TailscaleStatusCard(
            engineState = engineState,
            onAuthClick = { showAuthDialog = true }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Target Node Selector Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CyberCard)
                .border(1.dp, CyberBorder, RoundedCornerShape(16.dp))
                .clickable { if (!isRunning) showPeerPicker = true }
                .padding(14.dp)
                .testTag("target_node_selector")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (selectedNode.isDerpRelay) NeonYellow.copy(alpha = 0.2f) else NeonGreen.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Router,
                            contentDescription = null,
                            tint = if (selectedNode.isDerpRelay) NeonYellow else NeonGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "TARGET NODE TAILSCALE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextMuted,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "${selectedNode.name} (${selectedNode.ip})",
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                Text(
                    text = "GANTI >",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Main Speedometer Gauge Display
        SpeedometerGauge(
            currentSpeedMbps = progress.currentSpeedMbps,
            phase = progress.phase
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 4. Real-time Metrics 4-Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricCard(
                title = "PING",
                value = String.format(Locale.US, "%.0f", progress.pingMs),
                unit = "ms",
                icon = Icons.Default.Timer,
                color = NeonYellow,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "JITTER",
                value = String.format(Locale.US, "%.1f", progress.jitterMs),
                unit = "ms",
                icon = Icons.Default.NetworkCheck,
                color = NeonPurple,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "DOWNLOAD",
                value = String.format(Locale.US, "%.1f", progress.downloadMbps),
                unit = "Mbps",
                icon = Icons.Default.ArrowDownward,
                color = NeonCyan,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "UPLOAD",
                value = String.format(Locale.US, "%.1f", progress.uploadMbps),
                unit = "Mbps",
                icon = Icons.Default.ArrowUpward,
                color = NeonGreen,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 5. Live Throughput Graph
        SpeedChart(
            points = progress.liveSpeedGraph
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 6. Action Control Button (Start / Stop)
        Button(
            onClick = {
                if (isRunning) onCancelTest() else onStartTest()
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isRunning) MaterialTheme.colorScheme.error else NeonCyan,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag(if (isRunning) "cancel_speed_test_button" else "start_speed_test_button")
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isRunning) Icons.Default.Cancel else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isRunning) "HENTIKAN TEST" else "MULAI SPEED TEST TAILSCALE",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Modal Dialog 1: Target Peer Picker
    if (showPeerPicker) {
        AlertDialog(
            onDismissRequest = { showPeerPicker = false },
            title = {
                Text(
                    text = "Pilih Node Target Speed Test",
                    style = MaterialTheme.typography.titleMedium.copy(color = TextPrimary)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    peersList.forEach { node ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (node.id == selectedNode.id) NeonCyan.copy(alpha = 0.2f) else CyberCard)
                                .clickable {
                                    onSelectNode(node)
                                    showPeerPicker = false
                                }
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = node.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Text(
                                        text = "${node.ip} • ${node.location}",
                                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                                    )
                                }

                                Text(
                                    text = if (node.isDerpRelay) "DERP" else "Direct WireGuard",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (node.isDerpRelay) NeonYellow else NeonGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPeerPicker = false }) {
                    Text("Tutup", color = NeonCyan)
                }
            },
            containerColor = CyberDarkSurface
        )
    }

    // Modal Dialog 2: Auth Key Configuration
    if (showAuthDialog) {
        var keyInput by remember { mutableStateOf(engineState.authKey) }
        var tailnetInput by remember { mutableStateOf(engineState.tailnetName) }

        AlertDialog(
            onDismissRequest = { showAuthDialog = false },
            title = {
                Text(
                    text = "Pengaturan Auth Key Tailscale",
                    style = MaterialTheme.typography.titleMedium.copy(color = TextPrimary)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = tailnetInput,
                        onValueChange = { tailnetInput = it },
                        label = { Text("Nama Tailnet") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = CyberBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = keyInput,
                        onValueChange = { keyInput = it },
                        label = { Text("Tailscale Auth Key (tskey-auth-...)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = CyberBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Auth Key digunakan oleh embedded engine Tailscale untuk autentikasi aman dengan Tailnet controller.",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateAuthKey(keyInput, tailnetInput)
                        showAuthDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black)
                ) {
                    Text("Simpan & Hubungkan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAuthDialog = false }) {
                    Text("Batal", color = TextMuted)
                }
            },
            containerColor = CyberDarkSurface
        )
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    unit: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(CyberCard)
            .border(1.dp, CyberBorder, RoundedCornerShape(14.dp))
            .padding(10.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextMuted,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = color,
                        fontWeight = FontWeight.SemiBold
                    ),
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }
    }
}
