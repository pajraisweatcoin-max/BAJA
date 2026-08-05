package com.example.model

data class TailscaleEngineState(
    val isConnected: Boolean = true,
    val tailnetName: String = "private-tailnet.ts.net",
    val assignedIp: String = "100.82.112.45",
    val activePeersCount: Int = 8,
    val authKey: String = "tskey-auth-k98f237198273...",
    val derpRegion: String = "sgp - Singapore (DERP 12)",
    val encryptionStatus: String = "WireGuard ChaCha20-Poly1305 (Active)",
    val mtuSize: Int = 1280,
    val isServerRunning: Boolean = false,
    val serverPort: Int = 8088
)
