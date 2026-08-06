package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.ServerConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class SecureStorage(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("barra_cloud_prefs", Context.MODE_PRIVATE)
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val configAdapter = moshi.adapter(ServerConfig::class.java)

    companion object {
        private const val KEY_CONFIG = "server_config"
        private const val KEY_AUTH_COOKIE = "barra_auth_cookie"
    }

    fun saveConfig(config: ServerConfig) {
        val json = configAdapter.toJson(config)
        prefs.edit().putString(KEY_CONFIG, json).apply()
    }

    fun getConfig(): ServerConfig {
        val json = prefs.getString(KEY_CONFIG, null)
        return if (json != null) {
            runCatching { configAdapter.fromJson(json) }.getOrNull() ?: ServerConfig()
        } else {
            ServerConfig()
        }
    }

    fun saveAuthCookie(cookie: String) {
        prefs.edit().putString(KEY_AUTH_COOKIE, cookie).apply()
    }

    fun getAuthCookie(): String? {
        return prefs.getString(KEY_AUTH_COOKIE, null)
    }

    fun clearAuth() {
        prefs.edit().remove(KEY_AUTH_COOKIE).apply()
    }
}
