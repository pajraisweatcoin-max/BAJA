package com.example.barracloud.smb

import android.util.Log
import fi.iki.elonen.NanoHTTPD
import java.io.InputStream
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class SmbProxyServer(
    port: Int = 8080,
    private val smbManager: SmbConnectionManager
) : NanoHTTPD("127.0.0.1", port) {

    init {
        Log.d(TAG, "SmbProxyServer initialized on port $port")
    }

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        if (!uri.startsWith("/stream")) {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "404 Not Found")
        }

        val parms = session.parameters
        val encodedPath = parms["path"]?.firstOrNull()
            ?: return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Missing path parameter")

        val path = try {
            URLDecoder.decode(encodedPath, StandardCharsets.UTF_8.name())
        } catch (e: Exception) {
            encodedPath
        }

        val fileLength = smbManager.getFileLength(path)
        if (fileLength <= 0) {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "File not found or empty")
        }

        val mimeType = getMimeType(path)
        val headers = session.headers
        val rangeHeader = headers["range"] ?: headers["Range"]

        return if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            servePartialContent(path, fileLength, mimeType, rangeHeader)
        } else {
            serveFullContent(path, fileLength, mimeType)
        }
    }

    private fun servePartialContent(
        path: String,
        fileLength: Long,
        mimeType: String,
        rangeHeader: String
    ): Response {
        try {
            val rangeValue = rangeHeader.substring("bytes=".length)
            val parts = rangeValue.split("-")
            val start = parts[0].toLongOrNull() ?: 0L
            var end = if (parts.size > 1 && parts[1].isNotBlank()) parts[1].toLongOrNull() else null

            if (end == null || end >= fileLength) {
                end = fileLength - 1
            }

            val contentLength = end - start + 1
            val inputStream = smbManager.openInputStreamAtOffset(path, start)
                ?: return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Cannot open stream")

            val boundedStream = BoundedInputStream(inputStream, contentLength)
            val response = newFixedLengthResponse(
                Response.Status.PARTIAL_CONTENT,
                mimeType,
                boundedStream,
                contentLength
            )

            response.addHeader("Accept-Ranges", "bytes")
            response.addHeader("Content-Range", "bytes $start-$end/$fileLength")
            response.addHeader("Content-Length", contentLength.toString())
            return response
        } catch (e: Exception) {
            Log.e(TAG, "Error handling partial content request for $path", e)
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, e.localizedMessage)
        }
    }

    private fun serveFullContent(path: String, fileLength: Long, mimeType: String): Response {
        val inputStream = smbManager.openInputStreamAtOffset(path, 0L)
            ?: return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Cannot open stream")

        val response = newFixedLengthResponse(
            Response.Status.OK,
            mimeType,
            inputStream,
            fileLength
        )
        response.addHeader("Accept-Ranges", "bytes")
        response.addHeader("Content-Length", fileLength.toString())
        return response
    }

    private fun getMimeType(filename: String): String {
        val ext = filename.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            "gif" -> "image/gif"
            "heic" -> "image/heic"
            "mp4", "m4v" -> "video/mp4"
            "mkv" -> "video/x-matroska"
            "avi" -> "video/x-msvideo"
            "mov" -> "video/quicktime"
            "3gp" -> "video/3gpp"
            "webm" -> "video/webm"
            "ts" -> "video/mp2t"
            "srt" -> "text/plain"
            "vtt" -> "text/vtt"
            else -> "application/octet-stream"
        }
    }

    fun getStreamUrl(path: String): String {
        val encodedPath = java.net.URLEncoder.encode(path, StandardCharsets.UTF_8.name())
        return "http://127.0.0.1:$listeningPort/stream?path=$encodedPath"
    }

    companion object {
        private const val TAG = "SmbProxyServer"
    }

    private class BoundedInputStream(
        private val delegate: InputStream,
        private val maxBytes: Long
    ) : InputStream() {
        private var bytesRead: Long = 0

        override fun read(): Int {
            if (bytesRead >= maxBytes) return -1
            val result = delegate.read()
            if (result != -1) bytesRead++
            return result
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            if (bytesRead >= maxBytes) return -1
            val maxToRead = (maxBytes - bytesRead).coerceAtMost(len.toLong()).toInt()
            val result = delegate.read(b, off, maxToRead)
            if (result > 0) bytesRead += result
            return result
        }

        override fun close() {
            delegate.close()
        }
    }
}
