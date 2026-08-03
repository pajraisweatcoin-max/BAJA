package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.core.model.*
import java.nio.charset.StandardCharsets
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

class EncryptedPrefsManager(private val context: Context) {

    private val prefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                "barra_cloud_secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            context.getSharedPreferences("barra_cloud_prefs_fallback", Context.MODE_PRIVATE)
        }
    }

    fun saveSambaConfig(config: SambaConfig) {
        prefs.edit().apply {
            putString(KEY_SMB_HOST, encrypt(config.host))
            putInt(KEY_SMB_PORT, config.port)
            putString(KEY_SMB_SHARE, encrypt(config.shareName))
            putString(KEY_SMB_USER, encrypt(config.username))
            putString(KEY_SMB_PASS, encrypt(config.password))
            putBoolean(KEY_SMB_AUTO_CONNECT, config.autoConnect)
            apply()
        }
    }

    fun getSambaConfig(): SambaConfig {
        return SambaConfig(
            host = decrypt(prefs.getString(KEY_SMB_HOST, null)) ?: "",
            port = prefs.getInt(KEY_SMB_PORT, 445),
            shareName = decrypt(prefs.getString(KEY_SMB_SHARE, null)) ?: "",
            username = decrypt(prefs.getString(KEY_SMB_USER, null)) ?: "",
            password = decrypt(prefs.getString(KEY_SMB_PASS, null)) ?: "",
            autoConnect = prefs.getBoolean(KEY_SMB_AUTO_CONNECT, false)
        )
    }

    fun saveTailscaleConfig(config: TailscaleConfig) {
        prefs.edit().apply {
            putBoolean(KEY_TS_ENABLED, config.enabled)
            putBoolean(KEY_TS_AUTO_START, config.autoStart)
            putBoolean(KEY_TS_AUTO_RECONNECT, config.autoReconnect)
            putString(KEY_TS_AUTH_KEY, config.authKey)
            putString(KEY_TS_NODE_IP, config.nodeIp)
            putString(KEY_TS_DEVICE_NAME, config.deviceName)
            putString(KEY_TS_EXIT_NODE, config.exitNode)
            putString(KEY_TS_STATE, config.connectionState.name)
            apply()
        }
    }

    fun getTailscaleConfig(): TailscaleConfig {
        val stateName = prefs.getString(KEY_TS_STATE, TailscaleConnectionState.DISCONNECTED.name)
        val state = try {
            TailscaleConnectionState.valueOf(stateName ?: "DISCONNECTED")
        } catch (e: Exception) {
            TailscaleConnectionState.DISCONNECTED
        }

        return TailscaleConfig(
            enabled = prefs.getBoolean(KEY_TS_ENABLED, false),
            autoStart = prefs.getBoolean(KEY_TS_AUTO_START, false),
            autoReconnect = prefs.getBoolean(KEY_TS_AUTO_RECONNECT, false),
            authKey = prefs.getString(KEY_TS_AUTH_KEY, "") ?: "",
            nodeIp = prefs.getString(KEY_TS_NODE_IP, "") ?: "",
            deviceName = prefs.getString(KEY_TS_DEVICE_NAME, "") ?: "",
            exitNode = prefs.getString(KEY_TS_EXIT_NODE, "None (Direct)") ?: "None (Direct)",
            connectionState = state
        )
    }

    fun saveGridColumns(columns: Int) {
        prefs.edit().putInt(KEY_GRID_COLUMNS, columns).apply()
    }

    fun getGridColumns(): Int {
        return prefs.getInt(KEY_GRID_COLUMNS, 3)
    }

    fun saveThumbnailSize(size: ThumbnailSizeOption) {
        prefs.edit().putString(KEY_THUMB_SIZE, size.name).apply()
    }

    fun getThumbnailSize(): ThumbnailSizeOption {
        val name = prefs.getString(KEY_THUMB_SIZE, ThumbnailSizeOption.MEDIUM.name)
        return try {
            ThumbnailSizeOption.valueOf(name ?: "MEDIUM")
        } catch (e: Exception) {
            ThumbnailSizeOption.MEDIUM
        }
    }

    fun saveTheme(theme: ThemeOption) {
        prefs.edit().putString(KEY_THEME, theme.name).apply()
    }

    fun getTheme(): ThemeOption {
        val name = prefs.getString(KEY_THEME, ThemeOption.SYSTEM.name)
        return try {
            ThemeOption.valueOf(name ?: "SYSTEM")
        } catch (e: Exception) {
            ThemeOption.SYSTEM
        }
    }

    private fun encrypt(value: String?): String? {
        if (value == null) return null
        return try {
            val keyBytes = "BARRA_CLOUD_2026".toByteArray(StandardCharsets.UTF_8)
            val keySpec = SecretKeySpec(keyBytes, "AES")
            val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, keySpec)
            val encryptedBytes = cipher.doFinal(value.toByteArray(StandardCharsets.UTF_8))
            Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            value
        }
    }

    private fun decrypt(value: String?): String? {
        if (value == null) return null
        return try {
            val keyBytes = "BARRA_CLOUD_2026".toByteArray(StandardCharsets.UTF_8)
            val keySpec = SecretKeySpec(keyBytes, "AES")
            val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, keySpec)
            val decodedBytes = Base64.decode(value, Base64.NO_WRAP)
            String(cipher.doFinal(decodedBytes), StandardCharsets.UTF_8)
        } catch (e: Exception) {
            value
        }
    }

    companion object {
        private const val KEY_SMB_HOST = "smb_host"
        private const val KEY_SMB_PORT = "smb_port"
        private const val KEY_SMB_SHARE = "smb_share"
        private const val KEY_SMB_USER = "smb_user"
        private const val KEY_SMB_PASS = "smb_pass"
        private const val KEY_SMB_AUTO_CONNECT = "smb_auto_connect"

        private const val KEY_TS_ENABLED = "ts_enabled"
        private const val KEY_TS_AUTO_START = "ts_auto_start"
        private const val KEY_TS_AUTO_RECONNECT = "ts_auto_reconnect"
        private const val KEY_TS_AUTH_KEY = "ts_auth_key"
        private const val KEY_TS_NODE_IP = "ts_node_ip"
        private const val KEY_TS_DEVICE_NAME = "ts_device_name"
        private const val KEY_TS_EXIT_NODE = "ts_exit_node"
        private const val KEY_TS_STATE = "ts_state"

        private const val KEY_GRID_COLUMNS = "grid_columns"
        private const val KEY_THUMB_SIZE = "thumb_size"
        private const val KEY_THEME = "app_theme"
    }
}
