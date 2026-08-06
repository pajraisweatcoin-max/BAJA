package com.example.util

import android.content.Context
import com.example.data.local.SecureStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ConnectionLogger(context: Context) {
    private val secureStorage = SecureStorage(context)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    companion object {
        private val _logs = MutableStateFlow<List<String>>(emptyList())
        val logs: StateFlow<List<String>> = _logs.asStateFlow()

        fun logEvent(event: String, detail: String = "") {
            val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            val formatted = "[$timestamp] $event ${if (detail.isNotBlank()) "- $detail" else ""}".trim()
            AppLogger.i("TAILSCALE_EVENT", formatted)
            val currentList = _logs.value.toMutableList()
            currentList.add(0, formatted)
            if (currentList.size > 200) {
                currentList.removeAt(currentList.lastIndex)
            }
            _logs.value = currentList
        }

        fun clearHistory() {
            _logs.value = emptyList()
        }
    }

    fun log(event: String, detail: String = "") {
        val config = secureStorage.getConfig()
        if (config.saveConnectionHistory) {
            logEvent(event, detail)
        } else {
            AppLogger.i("TAILSCALE_LOG", "$event $detail".trim())
        }
    }
}
