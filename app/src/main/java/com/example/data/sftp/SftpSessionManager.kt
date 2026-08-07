package com.example.data.sftp

import com.example.data.db.SshServerEntity
import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import com.jcraft.jsch.SftpATTRS
import com.jcraft.jsch.SftpProgressMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.Vector

class SftpSessionManager private constructor() {

    private val jSch = JSch()
    private var session: Session? = null
    private var channelSftp: ChannelSftp? = null

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _activeServer = MutableStateFlow<SshServerEntity?>(null)
    val activeServer: StateFlow<SshServerEntity?> = _activeServer.asStateFlow()

    private val _currentPath = MutableStateFlow("/")
    val currentPath: StateFlow<String> = _currentPath.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private val sftpMutex = Mutex()

    companion object {
        @Volatile
        private var instance: SftpSessionManager? = null

        fun getInstance(): SftpSessionManager {
            return instance ?: synchronized(this) {
                instance ?: SftpSessionManager().also { instance = it }
            }
        }
    }

    /**
     * Establish SSH/SFTP session using a single session instance.
     */
    suspend fun connect(server: SshServerEntity): Result<Unit> = withContext(Dispatchers.IO) {
        sftpMutex.withLock {
            try {
                _connectionState.value = ConnectionState.CONNECTING
                _lastError.value = null
                
                // Disconnect existing session if any
                disconnectInternal()

                _activeServer.value = server

                // Setup private key if provided
                if (server.authType == "PRIVATE_KEY" && server.privateKey.isNotBlank()) {
                    jSch.removeAllIdentity()
                    val passphraseBytes = if (server.passphrase.isNotBlank()) server.passphrase.toByteArray() else null
                    jSch.addIdentity("custom_id", server.privateKey.toByteArray(), null, passphraseBytes)
                }

                val newSession = jSch.getSession(server.username, server.host, server.port)
                if (server.authType == "PASSWORD" && server.password.isNotBlank()) {
                    newSession.setPassword(server.password)
                }

                val config = java.util.Properties()
                config["StrictHostKeyChecking"] = "no"
                // Preferred auth methods
                config["PreferredAuthentications"] = "publickey,password,keyboard-interactive"
                newSession.setConfig(config)
                newSession.timeout = 15000

                newSession.connect()

                val sftp = newSession.openChannel("sftp") as ChannelSftp
                sftp.connect()

                session = newSession
                channelSftp = sftp

                val initPath = if (server.defaultPath.isNotBlank()) server.defaultPath else "/"
                _currentPath.value = try {
                    sftp.cd(initPath)
                    sftp.pwd()
                } catch (e: Exception) {
                    sftp.pwd()
                }

                _connectionState.value = ConnectionState.CONNECTED
                Result.success(Unit)
            } catch (e: Exception) {
                disconnectInternal()
                _connectionState.value = ConnectionState.ERROR
                val errMsg = e.localizedMessage ?: "Gagal terhubung ke server SSH"
                _lastError.value = errMsg
                Result.failure(Exception(errMsg))
            }
        }
    }

    suspend fun disconnect() = withContext(Dispatchers.IO) {
        sftpMutex.withLock {
            disconnectInternal()
        }
    }

    private fun disconnectInternal() {
        try {
            channelSftp?.disconnect()
        } catch (_: Exception) {}
        try {
            session?.disconnect()
        } catch (_: Exception) {}

        channelSftp = null
        session = null
        _activeServer.value = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    private fun ensureChannel(): ChannelSftp {
        var sftp = channelSftp
        var sess = session
        if (sess == null || !sess.isConnected) {
            val server = _activeServer.value
            if (server != null) {
                try {
                    disconnectInternal()
                    _activeServer.value = server
                    if (server.authType == "PRIVATE_KEY" && server.privateKey.isNotBlank()) {
                        try {
                            jSch.removeAllIdentity()
                            val passphraseBytes = if (server.passphrase.isNotBlank()) server.passphrase.toByteArray() else null
                            jSch.addIdentity("custom_id", server.privateKey.toByteArray(), null, passphraseBytes)
                        } catch (_: Exception) {}
                    }
                    val newSession = jSch.getSession(server.username, server.host, server.port)
                    if (server.authType == "PASSWORD" && server.password.isNotBlank()) {
                        newSession.setPassword(server.password)
                    }
                    val config = java.util.Properties()
                    config["StrictHostKeyChecking"] = "no"
                    config["PreferredAuthentications"] = "publickey,password,keyboard-interactive"
                    newSession.setConfig(config)
                    newSession.timeout = 15000
                    newSession.connect()

                    val newChannel = newSession.openChannel("sftp") as ChannelSftp
                    newChannel.connect()

                    session = newSession
                    channelSftp = newChannel
                    _connectionState.value = ConnectionState.CONNECTED
                    return newChannel
                } catch (_: Exception) {
                    throw IllegalStateException("Sesi SSH terputus. Silakan hubungkan kembali ke server.")
                }
            }
            throw IllegalStateException("Sesi SSH terputus. Silakan hubungkan kembali ke server.")
        }
        if (sftp == null || sftp.isClosed || !sftp.isConnected) {
            val newChannel = sess.openChannel("sftp") as ChannelSftp
            newChannel.connect()
            channelSftp = newChannel
            return newChannel
        }
        return sftp
    }

    /**
     * Escape shell argument safely using single quotes for bash/sh compatibility.
     */
    private fun escapeShellArg(arg: String): String {
        return "'" + arg.replace("'", "'\\''") + "'"
    }

    /**
     * List files in given path using the single active SFTP channel.
     */
    suspend fun listFiles(path: String = _currentPath.value): Result<List<SftpFileItem>> = withContext(Dispatchers.IO) {
        sftpMutex.withLock {
            try {
                val sftp = ensureChannel()
                sftp.cd(path)
                val targetPath = sftp.pwd()
                _currentPath.value = targetPath

                val vector: Vector<*> = sftp.ls(targetPath) ?: Vector<Any>()
                val items = mutableListOf<SftpFileItem>()

                for (i in 0 until vector.size) {
                    val entry = vector[i] as? ChannelSftp.LsEntry ?: continue
                    val filename = entry.filename ?: continue
                    if (filename == "." || filename == "..") continue

                    val attrs = entry.attrs
                    val isDir = attrs?.isDir == true
                    val isLink = attrs?.isLink == true
                    val fullPath = if (targetPath.endsWith("/")) "$targetPath$filename" else "$targetPath/$filename"
                    var linkTarget: String? = null
                    if (isLink) {
                        try {
                            linkTarget = sftp.readlink(fullPath)
                        } catch (_: Exception) {}
                    }

                    items.add(
                        SftpFileItem(
                            name = filename,
                            path = fullPath,
                            isDirectory = isDir,
                            size = attrs?.size ?: 0L,
                            permissions = attrs?.permissionsString ?: "-",
                            mtime = attrs?.mTime?.toLong() ?: 0L,
                            isSymlink = isLink,
                            linkTarget = linkTarget
                        )
                    )
                }

                // Sort: Folders first, then files alphabetically
                val sorted = items.sortedWith(
                    compareByDescending<SftpFileItem> { it.isDirectory }
                        .thenBy { it.name.lowercase() }
                )

                Result.success(sorted)
            } catch (e: Exception) {
                Result.failure(Exception(e.localizedMessage ?: "Gagal memuat daftar berkas"))
            }
        }
    }

    /**
     * Change directory to target path
     */
    suspend fun changeDirectory(targetPath: String): Result<List<SftpFileItem>> {
        return listFiles(targetPath)
    }

    /**
     * Upload a local file stream to remote SFTP directory using the single session.
     */
    suspend fun uploadFile(
        inputStream: InputStream,
        remoteFileName: String,
        targetDir: String = _currentPath.value,
        totalBytes: Long,
        onProgress: (transferred: Long) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        sftpMutex.withLock {
            try {
                val sftp = ensureChannel()
                val remotePath = if (targetDir.endsWith("/")) "$targetDir$remoteFileName" else "$targetDir/$remoteFileName"

                val monitor = object : SftpProgressMonitor {
                    private var count: Long = 0
                    private var lastReportTime: Long = 0
                    override fun init(op: Int, src: String?, dest: String?, max: Long) {}
                    override fun count(c: Long): Boolean {
                        count += c
                        val now = System.currentTimeMillis()
                        if (now - lastReportTime >= 100 || (totalBytes > 0 && count >= totalBytes)) {
                            lastReportTime = now
                            onProgress(count)
                        }
                        return true
                    }
                    override fun end() {
                        onProgress(count)
                    }
                }

                sftp.put(inputStream, remotePath, monitor, ChannelSftp.OVERWRITE)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(Exception("Gagal mengunggah berkas: ${e.localizedMessage}"))
            } finally {
                try { inputStream.close() } catch (_: Exception) {}
            }
        }
    }

    /**
     * Download a remote file to an OutputStream using the single session.
     */
    suspend fun downloadFile(
        remotePath: String,
        outputStream: OutputStream,
        onProgress: (transferred: Long) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        sftpMutex.withLock {
            try {
                val sftp = ensureChannel()

                val monitor = object : SftpProgressMonitor {
                    private var count: Long = 0
                    private var lastReportTime: Long = 0
                    override fun init(op: Int, src: String?, dest: String?, max: Long) {}
                    override fun count(c: Long): Boolean {
                        count += c
                        val now = System.currentTimeMillis()
                        if (now - lastReportTime >= 100) {
                            lastReportTime = now
                            onProgress(count)
                        }
                        return true
                    }
                    override fun end() {
                        onProgress(count)
                    }
                }

                sftp.get(remotePath, outputStream, monitor)
                try { outputStream.flush() } catch (_: Exception) {}
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(Exception("Gagal mengunduh berkas: ${e.localizedMessage}"))
            } finally {
                try { outputStream.close() } catch (_: Exception) {}
            }
        }
    }

    /**
     * Copy remote item (file or folder) to destination directory using the single SSH session.
     */
    suspend fun copyItem(srcPath: String, dstDir: String, fileName: String, isDirectory: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        sftpMutex.withLock {
            try {
                val sess = session ?: throw IllegalStateException("Terputus dari server SSH")
                val dstPath = if (dstDir.endsWith("/")) "$dstDir$fileName" else "$dstDir/$fileName"

                // First try fast remote SSH exec `cp -r src dst` on the same SSH session
                val execResult = runSshExecCommand(sess, "cp -r ${escapeShellArg(srcPath)} ${escapeShellArg(dstPath)}")
                if (execResult.isSuccess) {
                    return@withContext Result.success(Unit)
                }

                // Fallback stream copy if exec fails or disabled
                val sftp = ensureChannel()
                if (isDirectory) {
                    copyDirectoryStream(sftp, srcPath, dstPath)
                } else {
                    val baos = ByteArrayOutputStream()
                    sftp.get(srcPath, baos)
                    sftp.put(ByteArrayInputStream(baos.toByteArray()), dstPath)
                }

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(Exception("Gagal menyalin item: ${e.localizedMessage}"))
            }
        }
    }

    private fun copyDirectoryStream(sftp: ChannelSftp, srcDir: String, dstDir: String) {
        try {
            sftp.mkdir(dstDir)
        } catch (_: Exception) {}

        val vector: Vector<*> = sftp.ls(srcDir) ?: return
        for (i in 0 until vector.size) {
            val entry = vector[i] as? ChannelSftp.LsEntry ?: continue
            val name = entry.filename ?: continue
            if (name == "." || name == "..") continue

            val srcChild = if (srcDir.endsWith("/")) "$srcDir$name" else "$srcDir/$name"
            val dstChild = if (dstDir.endsWith("/")) "$dstDir$name" else "$dstDir/$name"

            if (entry.attrs?.isDir == true) {
                copyDirectoryStream(sftp, srcChild, dstChild)
            } else {
                val baos = ByteArrayOutputStream()
                sftp.get(srcChild, baos)
                sftp.put(ByteArrayInputStream(baos.toByteArray()), dstChild)
            }
        }
    }

    /**
     * Move (Cut/Rename) remote file or directory using the single SFTP channel.
     */
    suspend fun moveItem(srcPath: String, dstDir: String, fileName: String): Result<Unit> = withContext(Dispatchers.IO) {
        sftpMutex.withLock {
            try {
                val sftp = ensureChannel()
                val dstPath = if (dstDir.endsWith("/")) "$dstDir$fileName" else "$dstDir/$fileName"

                try {
                    sftp.rename(srcPath, dstPath)
                } catch (renameErr: Exception) {
                    // If cross-filesystem rename fails, try SSH exec mv command
                    val sess = session
                    if (sess != null) {
                        val execRes = runSshExecCommand(sess, "mv ${escapeShellArg(srcPath)} ${escapeShellArg(dstPath)}")
                        if (execRes.isFailure) {
                            throw renameErr
                        }
                    } else {
                        throw renameErr
                    }
                }

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(Exception("Gagal memindahkan item: ${e.localizedMessage}"))
            }
        }
    }

    /**
     * Rename file or directory.
     */
    suspend fun renameItem(oldPath: String, newName: String): Result<Unit> = withContext(Dispatchers.IO) {
        sftpMutex.withLock {
            try {
                val sftp = ensureChannel()
                val parentPath = oldPath.substringBeforeLast('/', "/")
                val newPath = if (parentPath.endsWith("/")) "$parentPath$newName" else "$parentPath/$newName"

                sftp.rename(oldPath, newPath)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(Exception("Gagal mengubah nama: ${e.localizedMessage}"))
            }
        }
    }

    /**
     * Delete file or directory recursively.
     */
    suspend fun deleteItem(path: String, isDirectory: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        if (path.trim() == "/" || path.isBlank()) {
            return@withContext Result.failure(Exception("Tidak dapat menghapus direktori root"))
        }
        sftpMutex.withLock {
            try {
                val sess = session
                // Try SSH exec `rm -rf path` first for fast recursive deletion
                if (sess != null) {
                    val execRes = runSshExecCommand(sess, "rm -rf ${escapeShellArg(path)}")
                    if (execRes.isSuccess) {
                        return@withContext Result.success(Unit)
                    }
                }

                val sftp = ensureChannel()
                if (isDirectory) {
                    deleteDirectoryRecursive(sftp, path)
                } else {
                    sftp.rm(path)
                }

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(Exception("Gagal menghapus item: ${e.localizedMessage}"))
            }
        }
    }

    private fun deleteDirectoryRecursive(sftp: ChannelSftp, dirPath: String) {
        val vector: Vector<*> = sftp.ls(dirPath) ?: return
        for (i in 0 until vector.size) {
            val entry = vector[i] as? ChannelSftp.LsEntry ?: continue
            val name = entry.filename ?: continue
            if (name == "." || name == "..") continue

            val childPath = if (dirPath.endsWith("/")) "$dirPath$name" else "$dirPath/$name"
            if (entry.attrs?.isDir == true) {
                deleteDirectoryRecursive(sftp, childPath)
            } else {
                sftp.rm(childPath)
            }
        }
        sftp.rmdir(dirPath)
    }

    /**
     * Create new folder in current path.
     */
    suspend fun createFolder(parentPath: String, folderName: String): Result<Unit> = withContext(Dispatchers.IO) {
        sftpMutex.withLock {
            try {
                val sftp = ensureChannel()
                val targetPath = if (parentPath.endsWith("/")) "$parentPath$folderName" else "$parentPath/$folderName"
                sftp.mkdir(targetPath)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(Exception("Gagal membuat folder: ${e.localizedMessage}"))
            }
        }
    }

    /**
     * Create new empty or populated file in current path.
     */
    suspend fun createFile(parentPath: String, fileName: String, content: String = ""): Result<Unit> = withContext(Dispatchers.IO) {
        sftpMutex.withLock {
            try {
                val sftp = ensureChannel()
                val targetPath = if (parentPath.endsWith("/")) "$parentPath$fileName" else "$parentPath/$fileName"
                sftp.put(ByteArrayInputStream(content.toByteArray(Charsets.UTF_8)), targetPath)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(Exception("Gagal membuat berkas: ${e.localizedMessage}"))
            }
        }
    }

    /**
     * Read text file content for preview/editing.
     */
    suspend fun readTextFile(remotePath: String): Result<String> = withContext(Dispatchers.IO) {
        sftpMutex.withLock {
            try {
                val sftp = ensureChannel()
                val baos = ByteArrayOutputStream()
                sftp.get(remotePath, baos)
                val content = baos.toString("UTF-8")
                Result.success(content)
            } catch (e: Exception) {
                Result.failure(Exception("Gagal membaca berkas: ${e.localizedMessage}"))
            }
        }
    }

    /**
     * Save updated text content back to remote file.
     */
    suspend fun saveTextFile(remotePath: String, newContent: String): Result<Unit> = withContext(Dispatchers.IO) {
        sftpMutex.withLock {
            try {
                val sftp = ensureChannel()
                sftp.put(ByteArrayInputStream(newContent.toByteArray(Charsets.UTF_8)), remotePath)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(Exception("Gagal menyimpan berkas: ${e.localizedMessage}"))
            }
        }
    }

    /**
     * Helper to run SSH command using exec channel on the SAME active SSH Session.
     */
    private fun runSshExecCommand(sess: Session, command: String): Result<Unit> {
        return try {
            val channelExec = sess.openChannel("exec") as ChannelExec
            channelExec.setCommand(command)
            channelExec.inputStream = null
            channelExec.setErrStream(System.err)
            channelExec.connect(5000)

            val startTime = System.currentTimeMillis()
            val timeoutMs = 10000L
            while (!channelExec.isClosed) {
                if (System.currentTimeMillis() - startTime > timeoutMs) {
                    channelExec.disconnect()
                    return Result.failure(Exception("Satu eksekusi SSH melebihi batas waktu (timeout)"))
                }
                Thread.sleep(50)
            }
            val status = channelExec.exitStatus
            channelExec.disconnect()

            if (status == 0) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Exec status $status"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
