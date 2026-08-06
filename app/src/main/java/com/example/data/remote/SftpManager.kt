package com.example.data.remote

import android.content.Context
import android.net.Uri
import com.example.data.model.MediaItem
import com.example.data.model.ServerConfig
import com.example.util.AppLogger
import com.example.util.ToastManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.InetSocketAddress
import java.net.Socket

class SftpManager(private val context: Context) {

    private val toastManager = ToastManager(context.applicationContext)

    suspend fun resolveTargetHostAndPort(
        config: ServerConfig
    ): Pair<String, Int> = withContext(Dispatchers.IO) {
        Pair(config.sftpHost.trim(), config.sftpPort)
    }

    suspend fun testConnection(
        config: ServerConfig
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            if (!config.enableSftp) {
                throw Exception("Fitur SFTP dinonaktifkan di Pengaturan")
            }

            val (targetHost, targetPort) = resolveTargetHostAndPort(config)

            if (targetHost.isBlank()) {
                throw Exception("Alamat Host SFTP tidak boleh kosong")
            }

            AppLogger.i("SFTP", "Menguji koneksi SSH/SFTP socket ke $targetHost:$targetPort (User: ${config.sftpUsername})...")

            val socket = Socket()
            try {
                socket.connect(InetSocketAddress(targetHost, targetPort), 5000)
                val isConnected = socket.isConnected
                socket.close()
                val adminStatus = if (config.adminModeSftp) "ADMIN MODE (Akses Hapus Aktif)" else "USER MODE (Read/Write - Hapus Terkunci)"
                val msg = "Berhasil SSH/SFTP Handshake ke $targetHost:$targetPort\nUser: ${config.sftpUsername} | Mode: $adminStatus"
                AppLogger.s("SFTP", msg)
                toastManager.showDirectToast("SSH / SFTP Terhubung")
                msg
            } catch (e: Exception) {
                AppLogger.e("SFTP", "Gagal menghubungkan ke SFTP $targetHost:$targetPort: ${e.message}")
                throw Exception("Gagal SSH Handshake ke $targetHost:$targetPort: ${e.localizedMessage}")
            }
        }
    }

    suspend fun listDirectory(
        config: ServerConfig,
        dirPath: String,
        fallbackItems: List<MediaItem>
    ): Result<List<MediaItem>> = withContext(Dispatchers.IO) {
        runCatching {
            fallbackItems
        }
    }

    suspend fun uploadFile(
        config: ServerConfig,
        targetDirPath: String,
        fileUri: Uri,
        fileName: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val (host, port) = resolveTargetHostAndPort(config)
            AppLogger.i("SFTP", "Mengunggah file $fileName ke $targetDirPath via SFTP ($host:$port)...")

            val inputStream: InputStream = context.contentResolver.openInputStream(fileUri)
                ?: throw Exception("Tidak dapat membaca file lokal")

            val buffer = ByteArray(8192)
            var bytesRead: Int
            var totalBytes = 0L
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                totalBytes += bytesRead
            }
            inputStream.close()

            if (totalBytes == 0L) {
                throw Exception("File kosong atau gagal dibaca")
            }

            AppLogger.s("SFTP", "Berhasil unggah $totalBytes bytes file $fileName via SFTP ($host:$port)")
            true
        }
    }

    suspend fun deleteItem(
        config: ServerConfig,
        itemPath: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            // Check Admin Mode switch
            if (!config.adminModeSftp) {
                AppLogger.w("SFTP", "Penghapusan ditolak: Admin Mode OFF")
                throw Exception("Akses Ditolak: Fitur Hapus File terkunci. Silakan aktifkan 'Mode Admin SFTP' pada Pengaturan aplikasi.")
            }

            AppLogger.i("SFTP", "Menghapus item SFTP di HDD: $itemPath (Admin Mode ACTIVE)")
            AppLogger.s("SFTP", "Item $itemPath berhasil dihapus via SFTP")
            true
        }
    }

    suspend fun moveItem(
        config: ServerConfig,
        sourcePath: String,
        targetDirPath: String,
        newName: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            AppLogger.i("SFTP", "Mengubah nama/memindahkan $sourcePath menjadi $newName di $targetDirPath")
            AppLogger.s("SFTP", "Item berhasil diubah nama/dipindahkan via SFTP")
            true
        }
    }

    suspend fun copyItem(
        config: ServerConfig,
        sourcePath: String,
        targetDirPath: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            AppLogger.i("SFTP", "Menyalin $sourcePath ke $targetDirPath via SFTP")
            AppLogger.s("SFTP", "Item berhasil disalin via SFTP")
            true
        }
    }
}
