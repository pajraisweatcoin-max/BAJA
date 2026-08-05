package com.example.util

import java.security.MessageDigest

object ThumbHashUtil {

    /**
     * Calculates the SHA-1 Hex Digest of a relative file path.
     * Matches backend server logic:
     * `crypto.createHash('sha1').update(relPath).digest('hex')`
     *
     * Example: "/Foto Keluarga/IMG_001.jpg" -> "a1b2c3d4e5f6..."
     */
    fun calculateSha1Hash(relativePath: String): String {
        val normalizedPath = if (relativePath.startsWith("/")) relativePath else "/$relativePath"
        val digest = MessageDigest.getInstance("SHA-1")
        val bytes = digest.digest(normalizedPath.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Constructs the server thumbnail path based on MEDIA_ROOT and relative path SHA-1 hash.
     * Example: MEDIA_ROOT = "/mnt/exthdd", relativePath = "/Foto Keluarga/IMG_001.jpg"
     * Result: "/mnt/exthdd/.thumbs/a1b2c3d4e5f6...jpg"
     */
    fun getThumbnailServerPath(mediaRoot: String, relativePath: String): String {
        val hash = calculateSha1Hash(relativePath)
        val cleanRoot = mediaRoot.trimEnd('/')
        return "$cleanRoot/.thumbs/$hash.jpg"
    }

    /**
     * Checks if a relative path or file name belongs to the hidden `.thumbs` folder.
     */
    fun isThumbsPath(path: String): Boolean {
        val normalized = path.replace("\\", "/")
        return normalized.contains("/.thumbs/") ||
               normalized.startsWith(".thumbs/") ||
               normalized == ".thumbs" ||
               normalized.endsWith("/.thumbs")
    }
}
