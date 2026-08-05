package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.SpeedTestPhase
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCard
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonYellow
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SpeedometerGauge(
    currentSpeedMbps: Double,
    maxGaugeSpeedMbps: Double = 300.0,
    phase: SpeedTestPhase,
    modifier: Modifier = Modifier
) {
    val animatedSpeed = remember { Animatable(0f) }

    LaunchedEffect(currentSpeedMbps) {
        animatedSpeed.animateTo(
            targetValue = currentSpeedMbps.toFloat(),
            animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
        )
    }

    val speedFraction = (animatedSpeed.value / maxGaugeSpeedMbps.toFloat()).coerceIn(0f, 1f)
    val startAngle = 135f
    val sweepAngle = 270f
    val currentAngle = startAngle + (speedFraction * sweepAngle)

    val activeColor = when (phase) {
        SpeedTestPhase.PINGING -> NeonYellow
        SpeedTestPhase.DOWNLOADING -> NeonCyan
        SpeedTestPhase.UPLOADING -> NeonGreen
        SpeedTestPhase.COMPLETED -> NeonPurple
        else -> NeonCyan
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.2f)
            .testTag("speedometer_gauge_container"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val center = Offset(canvasWidth / 2f, canvasHeight * 0.52f)
            val radius = size.minDimension * 0.38f
            val strokeWidth = 24.dp.toPx()

            // 1. Background Arc Track
            drawArc(
                color = CyberBorder,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // 2. Active Gradient Arc
            if (speedFraction > 0.001f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            NeonCyan.copy(alpha = 0.6f),
                            activeColor,
                            activeColor
                        ),
                        center = center
                    ),
                    startAngle = startAngle,
                    sweepAngle = speedFraction * sweepAngle,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            // 3. Tick Marks around speedometer
            val totalTicks = 10
            for (i in 0..totalTicks) {
                val tickFraction = i.toFloat() / totalTicks
                val tickAngleDeg = startAngle + (tickFraction * sweepAngle)
                val tickAngleRad = Math.toRadians(tickAngleDeg.toDouble())

                val innerR = radius - (strokeWidth / 2f) - 12.dp.toPx()
                val outerR = radius - (strokeWidth / 2f) - 20.dp.toPx()

                val startX = center.x + (innerR * cos(tickAngleRad)).toFloat()
                val startY = center.y + (innerR * sin(tickAngleRad)).toFloat()
                val endX = center.x + (outerR * cos(tickAngleRad)).toFloat()
                val endY = center.y + (outerR * sin(tickAngleRad)).toFloat()

                val isHighlighted = tickFraction <= speedFraction
                drawLine(
                    color = if (isHighlighted) activeColor else TextMuted.copy(alpha = 0.4f),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = if (isHighlighted) 3.dp.toPx() else 1.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // 4. Center Needle Pointer
            val needleLength = radius * 0.78f
            val needleAngleRad = Math.toRadians(currentAngle.toDouble())
            val needleEnd = Offset(
                x = center.x + (needleLength * cos(needleAngleRad)).toFloat(),
                y = center.y + (needleLength * sin(needleAngleRad)).toFloat()
            )

            val needlePath = Path().apply {
                val baseWidth = 8.dp.toPx()
                val perpAngleRad = needleAngleRad + (Math.PI / 2.0)
                val p1 = Offset(
                    center.x + (baseWidth * cos(perpAngleRad)).toFloat(),
                    center.y + (baseWidth * sin(perpAngleRad)).toFloat()
                )
                val p2 = Offset(
                    center.x - (baseWidth * cos(perpAngleRad)).toFloat(),
                    center.y - (baseWidth * sin(perpAngleRad)).toFloat()
                )
                moveTo(p1.x, p1.y)
                lineTo(needleEnd.x, needleEnd.y)
                lineTo(p2.x, p2.y)
                close()
            }

            drawPath(
                path = needlePath,
                color = activeColor
            )

            // Pivot Circle at center
            drawCircle(
                color = CyberCard,
                radius = 16.dp.toPx(),
                center = center
            )
            drawCircle(
                color = activeColor,
                radius = 8.dp.toPx(),
                center = center
            )
        }

        // Center Digital Display Readout
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val speedText = String.format(Locale.US, "%.1f", animatedSpeed.value)
            Text(
                text = speedText,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                ),
                modifier = Modifier.testTag("digital_speed_readout")
            )
            Text(
                text = "Mbps",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = activeColor,
                    fontWeight = FontWeight.SemiBold
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Phase indicator tag
            val (phaseLabel, phaseColor) = when (phase) {
                SpeedTestPhase.IDLE -> "SIAP" to TextSecondary
                SpeedTestPhase.PINGING -> "MENGUKUR LATENSI..." to NeonYellow
                SpeedTestPhase.DOWNLOADING -> "DOWNLOAD..." to NeonCyan
                SpeedTestPhase.UPLOADING -> "UPLOAD..." to NeonGreen
                SpeedTestPhase.COMPLETED -> "SELESAI" to NeonPurple
                SpeedTestPhase.CANCELLED -> "DIBATALKAN" to TextMuted
                SpeedTestPhase.ERROR -> "GAGAL" to Color.Red
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(phaseColor.copy(alpha = 0.15f))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when (phase) {
                            SpeedTestPhase.DOWNLOADING -> Icons.Default.ArrowDownward
                            SpeedTestPhase.UPLOADING -> Icons.Default.ArrowUpward
                            else -> Icons.Default.Speed
                        },
                        contentDescription = null,
                        tint = phaseColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = phaseLabel,
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = phaseColor,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}
