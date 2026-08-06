package com.example.util

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.example.data.local.SecureStorage

class ToastManager(private val context: Context) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val secureStorage = SecureStorage(context)

    fun showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
        val config = secureStorage.getConfig()
        if (!config.showConnectionToast) return

        mainHandler.post {
            runCatching {
                Toast.makeText(context.applicationContext, message, duration).show()
            }.onFailure { e ->
                AppLogger.e("TOAST", "Gagal menampilkan Toast: ${e.message}")
            }
        }
    }

    fun showDirectToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
        mainHandler.post {
            runCatching {
                Toast.makeText(context.applicationContext, message, duration).show()
            }.onFailure { e ->
                AppLogger.e("TOAST", "Gagal menampilkan Toast: ${e.message}")
            }
        }
    }
}
