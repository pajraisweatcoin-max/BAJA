package com.example.barracloud.data.models

data class SmbCredentials(
    val server: String = "",
    val port: Int = 445,
    val shareName: String = "",
    val domain: String = "",
    val username: String = "",
    val password: String = "",
    val smbVersion: String = "SMB2", // "SMB2" or "SMB3"
    val rememberLogin: Boolean = true,
    val autoConnect: Boolean = true
) {
    val isValid: Boolean
        get() = server.isNotBlank() && shareName.isNotBlank()

    fun buildSmbUrl(relativePath: String): String {
        val cleanPath = relativePath.trim('/').replace('\\', '/')
        val cleanShare = shareName.trim('/')
        return if (cleanPath.isEmpty()) {
            "smb://$server/$cleanShare"
        } else {
            "smb://$server/$cleanShare/$cleanPath"
        }
    }
}
