package com.example.data.remote

import com.example.data.model.ServerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TailscaleManager {
    private var isConnected = false

    suspend fun updateState(config: ServerConfig): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            if (config.useTailscale) {
                if (config.tailnetHost.isBlank()) {
                    throw Exception("Alamat Host Tailnet belum diisi di Pengaturan")
                }
                // Activate Tailscale app-level tunnel
                isConnected = true
                true
            } else {
                isConnected = false
                false
            }
        }
    }

    fun isTailscaleActive(): Boolean = isConnected
}
