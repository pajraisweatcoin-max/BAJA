package com.example.model

data class TailnetNode(
    val id: String,
    val name: String,
    val ip: String,
    val location: String,
    val isDerpRelay: Boolean = false,
    val isOnline: Boolean = true,
    var latencyMs: Long = 0,
    val osType: String = "Linux", // "Linux", "Windows", "macOS", "Android", "DERP"
    val isDirect: Boolean = true, // Direct WireGuard Peer vs Relayed
    val tags: List<String> = emptyList()
)
