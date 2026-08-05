package com.example.service

import com.example.model.FileNode
import com.example.model.MediaItem
import com.example.model.SftpConfig
import com.example.util.ThumbHashUtil
import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Vector

class SftpService {

    private var jsch: JSch? = null
    private var session: Session? = null
    private var channelSftp: ChannelSftp? = null
    private var isConnected = false

    /**
     * Test connection to SSH/SFTP server or simulate demo mode
     */
    suspend fun testConnection(config: SftpConfig): Result<String> = withContext(Dispatchers.IO) {
        if (config.isDemoMode) {
            return@withContext Result.success("Koneksi berhasil! (Demo Simulation Active - Offline Dataset)")
        }

        val targetHost = if (config.useTailscale && config.tailscaleIp.isNotBlank()) config.tailscaleIp else config.host
        val modeLabel = if (config.useTailscale) "Embedded Tailscale Tunnel" else "Direct LAN/Public IP"

        try {
            close()
            jsch = JSch()
            val sess = jsch!!.getSession(config.username, targetHost, config.port)
            sess.setPassword(config.password)
            sess.setConfig("StrictHostKeyChecking", "no")
            sess.timeout = 8000
            sess.connect()

            val channel = sess.openChannel("sftp") as ChannelSftp
            channel.connect(6000)

            channel.disconnect()
            sess.disconnect()
            Result.success("Terhubung via $modeLabel ke SSH/SFTP Server ($targetHost:${config.port})!")
        } catch (e: Exception) {
            val causeStr = e.message ?: e.javaClass.simpleName
            Result.failure(Exception("Gagal terhubung via $modeLabel ($targetHost:${config.port}) - $causeStr."))
        }
    }

    /**
     * Ensure active SFTP session
     */
    suspend fun connect(config: SftpConfig): Boolean = withContext(Dispatchers.IO) {
        if (config.isDemoMode) {
            isConnected = true
            return@withContext true
        }

        if (isConnected && session?.isConnected == true && channelSftp?.isConnected == true) {
            return@withContext true
        }

        val targetHost = if (config.useTailscale && config.tailscaleIp.isNotBlank()) config.tailscaleIp else config.host

        try {
            close()
            jsch = JSch()
            session = jsch!!.getSession(config.username, targetHost, config.port).apply {
                setPassword(config.password)
                setConfig("StrictHostKeyChecking", "no")
                timeout = 10000
                connect()
            }
            channelSftp = (session!!.openChannel("sftp") as ChannelSftp).apply {
                connect(5000)
            }
            isConnected = true
            true
        } catch (e: Exception) {
            e.printStackTrace()
            isConnected = false
            false
        }
    }

    /**
     * Fetch media list from server (recursively scanning root while ignoring `.thumbs`)
     */
    suspend fun fetchMediaList(
        config: SftpConfig,
        onLog: (String, Boolean) -> Unit
    ): List<MediaItem> = withContext(Dispatchers.IO) {
        val mediaItems = mutableListOf<MediaItem>()

        if (config.isDemoMode) {
            onLog("Scanning server directory [${config.mediaRoot}] (Demo Engine)...", false)
            val demoList = getDemoMediaList(config.mediaRoot)
            onLog("Ditemukan ${demoList.size} file media (.thumbs folder dikecualikan).", false)
            return@withContext demoList
        }

        val connected = connect(config)
        if (!connected || channelSftp == null) {
            onLog("Gagal menghubungkan SFTP. Menggunakan dataset demo lokal.", true)
            return@withContext getDemoMediaList(config.mediaRoot)
        }

        val root = config.mediaRoot.trimEnd('/')
        onLog("Scanning SFTP directory: $root...", false)
        try {
            scanDirectoryRecursive(channelSftp!!, root, "", mediaItems)
            onLog("Scan selesai! Total ${mediaItems.size} file media terdaftar.", false)
        } catch (e: Exception) {
            onLog("SFTP Scan Warning: ${e.message}. Fallback demo items.", true)
            return@withContext getDemoMediaList(config.mediaRoot)
        }

        mediaItems.sortedByDescending { it.mtimeMs }
    }

    private fun scanDirectoryRecursive(
        sftp: ChannelSftp,
        baseRoot: String,
        subPath: String,
        outList: MutableList<MediaItem>
    ) {
        val currentDir = if (subPath.isEmpty()) baseRoot else "$baseRoot$subPath"
        @Suppress("UNCHECKED_CAST")
        val entries = sftp.ls(currentDir) as Vector<ChannelSftp.LsEntry>

        val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))

        for (entry in entries) {
            val name = entry.filename
            if (name == "." || name == "..") continue

            val relPath = if (subPath.isEmpty()) "/$name" else "$subPath/$name"

            // EXCLUSION RULE: Skip .thumbs folder strictly
            if (ThumbHashUtil.isThumbsPath(relPath) || name == ".thumbs") {
                continue
            }

            val attrs = entry.attrs
            if (attrs.isDir) {
                scanDirectoryRecursive(sftp, baseRoot, relPath, outList)
            } else {
                val lowerName = name.lowercase(Locale.ROOT)
                val isPhoto = lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") ||
                        lowerName.endsWith(".png") || lowerName.endsWith(".webp")
                val isVideo = lowerName.endsWith(".mp4") || lowerName.endsWith(".mkv") ||
                        lowerName.endsWith(".mov") || lowerName.endsWith(".webm")

                if (isPhoto || isVideo) {
                    val mtimeMs = attrs.mTime.toLong() * 1000L
                    val sha1 = ThumbHashUtil.calculateSha1Hash(relPath)
                    outList.add(
                        MediaItem(
                            id = sha1,
                            name = name,
                            relativePath = relPath,
                            isVideo = isVideo,
                            sizeBytes = attrs.size,
                            mtimeMs = mtimeMs,
                            sha1Hash = sha1,
                            formattedDate = dateFormat.format(Date(mtimeMs)),
                            formattedSize = formatBytes(attrs.size)
                        )
                    )
                }
            }
        }
    }

    /**
     * Download thumbnail file from `.thumbs/[HASH].jpg` via SFTP
     */
    suspend fun downloadThumbnailBytes(
        config: SftpConfig,
        sha1Hash: String
    ): ByteArray? = withContext(Dispatchers.IO) {
        if (config.isDemoMode) {
            return@withContext null
        }

        val connected = connect(config)
        if (!connected || channelSftp == null) return@withContext null

        val thumbServerPath = "${config.mediaRoot.trimEnd('/')}/.thumbs/$sha1Hash.jpg"
        try {
            val baos = ByteArrayOutputStream()
            channelSftp!!.get(thumbServerPath, baos)
            baos.toByteArray()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Fetch file nodes for SFTP File Manager view
     */
    suspend fun listDirectory(
        config: SftpConfig,
        relativePath: String
    ): List<FileNode> = withContext(Dispatchers.IO) {
        if (config.isDemoMode) {
            return@withContext getDemoDirectoryNodes(relativePath)
        }

        val connected = connect(config)
        if (!connected || channelSftp == null) {
            return@withContext getDemoDirectoryNodes(relativePath)
        }

        val fullPath = if (relativePath == "/") config.mediaRoot else "${config.mediaRoot.trimEnd('/')}$relativePath"
        val nodes = mutableListOf<FileNode>()

        try {
            @Suppress("UNCHECKED_CAST")
            val entries = channelSftp!!.ls(fullPath) as Vector<ChannelSftp.LsEntry>
            for (entry in entries) {
                val name = entry.filename
                if (name == "." || name == "..") continue
                if (name == ".thumbs") continue // Hide .thumbs folder

                val nodeRelPath = if (relativePath == "/") "/$name" else "$relativePath/$name"
                nodes.add(
                    FileNode(
                        name = name,
                        relativePath = nodeRelPath,
                        isDirectory = entry.attrs.isDir,
                        sizeBytes = entry.attrs.size,
                        mtimeMs = entry.attrs.mTime.toLong() * 1000L,
                        permissions = entry.attrs.permissionsString ?: "rw-r--r--"
                    )
                )
            }
        } catch (e: Exception) {
            return@withContext getDemoDirectoryNodes(relativePath)
        }

        nodes.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
    }

    fun close() {
        try {
            channelSftp?.disconnect()
            session?.disconnect()
        } catch (e: Exception) {
            // ignore
        } finally {
            channelSftp = null
            session = null
            isConnected = false
        }
    }

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
            else -> "%.2f MB".format(bytes / (1024.0 * 1024.0))
        }
    }

    private fun getDemoMediaList(mediaRoot: String): List<MediaItem> {
        val now = System.currentTimeMillis()
        val dayMs = 86400000L
        val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))

        val items = listOf(
            Triple("/Foto Keluarga/Liburan_Bali_01.jpg", false, now - (dayMs * 0.2).toLong()),
            Triple("/Foto Keluarga/Sunset_Kuta.mp4", true, now - (dayMs * 0.3).toLong()),
            Triple("/Foto Keluarga/Panorama_Bedugul.jpg", false, now - (dayMs * 0.5).toLong()),
            Triple("/Proyek Barra/Server_Rack_Node01.jpg", false, now - (dayMs * 1.1).toLong()),
            Triple("/Proyek Barra/Tailscale_Benchmark.mp4", true, now - (dayMs * 1.2).toLong()),
            Triple("/Dokumen Visual/Struktur_Jaringan.png", false, now - (dayMs * 2.3).toLong()),
            Triple("/Dokumen Visual/Demo_Stream_4K.mp4", true, now - (dayMs * 2.4).toLong()),
            Triple("/Foto Keluarga/Kuliner_Malam.jpg", false, now - (dayMs * 3.1).toLong()),
            Triple("/Foto Keluarga/Pantai_Pandawa.jpg", false, now - (dayMs * 3.2).toLong()),
            Triple("/Video Drone/Mount_Batur_Flyby.mp4", true, now - (dayMs * 4.5).toLong()),
            Triple("/Video Drone/Coastline_Aerial.mp4", true, now - (dayMs * 4.6).toLong()),
            Triple("/Arsip 2025/Desain_Arsitektur_Cloud.png", false, now - (dayMs * 5.0).toLong())
        )

        return items.map { (relPath, isVideo, time) ->
            val name = relPath.substringAfterLast('/')
            val sha1 = ThumbHashUtil.calculateSha1Hash(relPath)
            MediaItem(
                id = sha1,
                name = name,
                relativePath = relPath,
                isVideo = isVideo,
                sizeBytes = if (isVideo) 48_500_000L else 3_200_000L,
                mtimeMs = time,
                sha1Hash = sha1,
                formattedDate = dateFormat.format(Date(time)),
                formattedSize = if (isVideo) "46.2 MB" else "3.1 MB"
            )
        }
    }

    private fun getDemoDirectoryNodes(relativePath: String): List<FileNode> {
        val now = System.currentTimeMillis()
        if (relativePath == "/" || relativePath.isEmpty()) {
            return listOf(
                FileNode("Foto Keluarga", "/Foto Keluarga", isDirectory = true, sizeBytes = 0, mtimeMs = now),
                FileNode("Proyek Barra", "/Proyek Barra", isDirectory = true, sizeBytes = 0, mtimeMs = now),
                FileNode("Dokumen Visual", "/Dokumen Visual", isDirectory = true, sizeBytes = 0, mtimeMs = now),
                FileNode("Video Drone", "/Video Drone", isDirectory = true, sizeBytes = 0, mtimeMs = now),
                FileNode("Arsip 2025", "/Arsip 2025", isDirectory = true, sizeBytes = 0, mtimeMs = now),
                FileNode("tailscale_config.yaml", "/tailscale_config.yaml", isDirectory = false, sizeBytes = 4096, mtimeMs = now, permissions = "rw-r--r--"),
                FileNode("server_status.json", "/server_status.json", isDirectory = false, sizeBytes = 1024, mtimeMs = now, permissions = "rw-r--r--")
            )
        }

        if (relativePath == "/Foto Keluarga") {
            return listOf(
                FileNode("Liburan_Bali_01.jpg", "/Foto Keluarga/Liburan_Bali_01.jpg", isDirectory = false, sizeBytes = 3_200_000, mtimeMs = now),
                FileNode("Sunset_Kuta.mp4", "/Foto Keluarga/Sunset_Kuta.mp4", isDirectory = false, sizeBytes = 48_500_000, mtimeMs = now),
                FileNode("Panorama_Bedugul.jpg", "/Foto Keluarga/Panorama_Bedugul.jpg", isDirectory = false, sizeBytes = 4_100_000, mtimeMs = now),
                FileNode("Kuliner_Malam.jpg", "/Foto Keluarga/Kuliner_Malam.jpg", isDirectory = false, sizeBytes = 2_800_000, mtimeMs = now),
                FileNode("Pantai_Pandawa.jpg", "/Foto Keluarga/Pantai_Pandawa.jpg", isDirectory = false, sizeBytes = 3_900_000, mtimeMs = now)
            )
        }

        return listOf(
            FileNode("Sample_Media.jpg", "$relativePath/Sample_Media.jpg", isDirectory = false, sizeBytes = 2_400_000, mtimeMs = now),
            FileNode("Sample_Video.mp4", "$relativePath/Sample_Video.mp4", isDirectory = false, sizeBytes = 25_000_000, mtimeMs = now)
        )
    }
}
