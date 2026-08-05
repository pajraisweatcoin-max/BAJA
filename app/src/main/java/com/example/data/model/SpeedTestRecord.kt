package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "speed_test_records")
data class SpeedTestRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val nodeName: String,
    val ipAddress: String,
    val pingMs: Double,
    val jitterMs: Double,
    val downloadMbps: Double,
    val uploadMbps: Double,
    val connectionType: String, // e.g. "Tailnet Direct (WireGuard)", "DERP Relay (SGP)"
    val networkRating: String, // e.g. "Excellent - Low Latency Private", "Good - DERP Relayed"
    val bytesTransferredMb: Double = 0.0
)
