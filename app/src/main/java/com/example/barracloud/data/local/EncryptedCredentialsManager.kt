package com.example.barracloud.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.barracloud.data.models.SmbCredentials

class EncryptedCredentialsManager(private val context: Context) {

    private val prefs: SharedPreferences by lazy {
        getSafeSharedPreferences()
    }

    private fun getSafeSharedPreferences(): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                "barracloud_secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (t: Throwable) {
            Log.e("EncryptedCreds", "Failed to create EncryptedSharedPreferences, fallback to standard", t)
            context.getSharedPreferences("barracloud_fallback_prefs", Context.MODE_PRIVATE)
        }
    }

    fun saveCredentials(credentials: SmbCredentials) {
        try {
            prefs.edit().apply {
                putString(KEY_SERVER, credentials.server)
                putInt(KEY_PORT, credentials.port)
                putString(KEY_SHARE, credentials.shareName)
                putString(KEY_DOMAIN, credentials.domain)
                putString(KEY_USERNAME, credentials.username)
                putString(KEY_PASSWORD, credentials.password)
                putString(KEY_VERSION, credentials.smbVersion)
                putBoolean(KEY_REMEMBER, credentials.rememberLogin)
                putBoolean(KEY_AUTO_CONNECT, credentials.autoConnect)
                apply()
            }
        } catch (t: Throwable) {
            Log.e("EncryptedCreds", "Failed to save credentials", t)
        }
    }

    fun loadCredentials(): SmbCredentials {
        return try {
            val server = prefs.getString(KEY_SERVER, "") ?: ""
            val port = prefs.getInt(KEY_PORT, 445)
            val share = prefs.getString(KEY_SHARE, "") ?: ""
            val domain = prefs.getString(KEY_DOMAIN, "") ?: ""
            val username = prefs.getString(KEY_USERNAME, "") ?: ""
            val password = prefs.getString(KEY_PASSWORD, "") ?: ""
            val version = prefs.getString(KEY_VERSION, "SMB2") ?: "SMB2"
            val remember = prefs.getBoolean(KEY_REMEMBER, true)
            val autoConnect = prefs.getBoolean(KEY_AUTO_CONNECT, true)

            SmbCredentials(
                server = server,
                port = port,
                shareName = share,
                domain = domain,
                username = username,
                password = password,
                smbVersion = version,
                rememberLogin = remember,
                autoConnect = autoConnect
            )
        } catch (t: Throwable) {
            Log.e("EncryptedCreds", "Error loading credentials, clearing corrupt prefs", t)
            runCatching { context.getSharedPreferences("barracloud_fallback_prefs", Context.MODE_PRIVATE).edit().clear().apply() }
            SmbCredentials()
        }
    }

    fun clearCredentials() {
        try {
            prefs.edit().clear().apply()
        } catch (t: Throwable) {
            Log.e("EncryptedCreds", "Failed to clear credentials", t)
        }
    }

    companion object {
        private const val KEY_SERVER = "smb_server"
        private const val KEY_PORT = "smb_port"
        private const val KEY_SHARE = "smb_share"
        private const val KEY_DOMAIN = "smb_domain"
        private const val KEY_USERNAME = "smb_username"
        private const val KEY_PASSWORD = "smb_password"
        private const val KEY_VERSION = "smb_version"
        private const val KEY_REMEMBER = "smb_remember"
        private const val KEY_AUTO_CONNECT = "smb_auto_connect"
    }
}
