package com.example.engine

import com.example.model.TailnetNode

enum class SpeedTestPhase {
    IDLE,
    PINGING,
    DOWNLOADING,
    UPLOADING,
    COMPLETED,
    CANCELLED,
    ERROR
}

data class SpeedTestProgress(
    val phase: SpeedTestPhase = SpeedTestPhase.IDLE,
    val currentSpeedMbps: Double = 0.0,
    val progressPercent: Float = 0f,
    val pingMs: Double = 0.0,
    val jitterMs: Double = 0.0,
    val downloadMbps: Double = 0.0,
    val uploadMbps: Double = 0.0,
    val targetNode: TailnetNode? = null,
    val liveSpeedGraph: List<Pair<Float, Float>> = emptyList(), // Pair(progressPercent, speedMbps)
    val errorMessage: String? = null
)
