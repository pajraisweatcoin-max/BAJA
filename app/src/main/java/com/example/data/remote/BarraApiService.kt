package com.example.data.remote

import android.content.Context
import android.net.Uri
import com.example.data.local.SecureStorage
import com.example.data.model.MediaItem
import com.example.util.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class BarraApiService(private val secureStorage: SecureStorage) {

    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .cookieJar(BarraCookieJar(secureStorage))
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private fun normalizeUrl(url: String): String {
        var clean = url.trim()
        if (!clean.startsWith("http://") && !clean.startsWith("https://")) {
            clean = "http://$clean"
        }
        return clean.removeSuffix("/")
    }

    suspend fun login(baseUrl: String, password: String): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val serverUrl = normalizeUrl(baseUrl)
            AppLogger.i("HTTP_API", "Mulai autentikasi ke $serverUrl/api/login...")
            val jsonBody = JSONObject().apply {
                put("password", password)
            }.toString()

            val request = Request.Builder()
                .url("$serverUrl/api/login")
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful || response.code == 200) {
                AppLogger.s("HTTP_API", "Autentikasi HTTP Berhasil (Cookie tersimpan)")
                true
            } else {
                val errMsg = "HTTP Error ${response.code}: ${response.message}"
                AppLogger.e("HTTP_API", errMsg)
                throw Exception(errMsg)
            }
        }
    }

    suspend fun listDirectory(baseUrl: String, path: String = "/"): Result<List<MediaItem>> = withContext(Dispatchers.IO) {
        runCatching {
            val serverUrl = normalizeUrl(baseUrl)
            AppLogger.d("HTTP_API", "Membaca direktori path: $path")
            val httpUrl = "$serverUrl/api/list".toHttpUrlOrNull()
                ?.newBuilder()
                ?.addQueryParameter("path", path)
                ?.build()
                ?: throw Exception("URL Server tidak valid")

            val request = Request.Builder()
                .url(httpUrl)
                .get()
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                val err = "Gagal mengambil data folder (${response.code})"
                AppLogger.e("HTTP_API", err)
                throw Exception(err)
            }

            val bodyStr = response.body?.string() ?: "[]"
            val parsed = parseMediaItems(bodyStr, path)
            AppLogger.d("HTTP_API", "Ditemukan ${parsed.size} item di $path")
            parsed
        }
    }

    suspend fun listAllMediaRecursively(baseUrl: String, rootPath: String = "/"): Result<List<MediaItem>> = withContext(Dispatchers.IO) {
        runCatching {
            AppLogger.i("HTTP_API", "Memulai pemindaian media rekursif dari $rootPath...")
            val resultList = mutableListOf<MediaItem>()
            val queue = ArrayDeque<String>()
            queue.add(rootPath)

            var depthCount = 0
            val visitedPaths = mutableSetOf<String>()

            while (queue.isNotEmpty() && depthCount < 100) {
                val currentPath = queue.removeFirst()
                if (visitedPaths.contains(currentPath)) continue
                visitedPaths.add(currentPath)
                depthCount++

                val res = listDirectory(baseUrl, currentPath)
                if (res.isSuccess) {
                    val items = res.getOrDefault(emptyList())
                    for (item in items) {
                        if (item.isDir) {
                            if (!item.name.startsWith(".")) {
                                queue.add(item.path)
                            }
                        } else if (item.isMedia) {
                            resultList.add(item)
                        }
                    }
                }
            }
            AppLogger.s("HTTP_API", "Pemindaian selesai: ${resultList.size} file media ditemukan")
            resultList.sortedByDescending { it.mtime }
        }
    }

    suspend fun uploadFile(
        baseUrl: String,
        targetPath: String,
        fileUri: Uri,
        fileName: String,
        context: Context
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val serverUrl = normalizeUrl(baseUrl)
            AppLogger.i("UPLOAD", "Membaca file lokal $fileName untuk diunggah ke $targetPath...")

            // Copy content URI to temp file to obtain length and stream
            val tempFile = File(context.cacheDir, "upload_tmp_${System.currentTimeMillis()}_$fileName")
            context.contentResolver.openInputStream(fileUri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            } ?: throw Exception("Gagal membaca stream file dari URI")

            AppLogger.i("UPLOAD", "Mengunggah ${tempFile.length()} bytes ke $serverUrl/api/upload...")

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("path", targetPath)
                .addFormDataPart(
                    "file",
                    fileName,
                    tempFile.asRequestBody("application/octet-stream".toMediaType())
                )
                .build()

            val request = Request.Builder()
                .url("$serverUrl/api/upload")
                .post(requestBody)
                .build()

            val response = okHttpClient.newCall(request).execute()
            tempFile.delete()

            if (response.isSuccessful || response.code == 200 || response.code == 201) {
                AppLogger.s("UPLOAD", "Berhasil unggah file $fileName ke HTTP server!")
                true
            } else {
                val err = "Gagal upload HTTP (${response.code}): ${response.message}"
                AppLogger.e("UPLOAD", err)
                throw Exception(err)
            }
        }
    }

    fun getStreamUrl(baseUrl: String, path: String, isThumb: Boolean = false, authCookie: String? = null): String {
        val serverUrl = normalizeUrl(baseUrl)
        val builder = "$serverUrl/api/stream".toHttpUrlOrNull()?.newBuilder()
            ?.addQueryParameter("path", path)
        if (isThumb) {
            builder?.addQueryParameter("thumb", "1")
        }
        if (!authCookie.isNullOrEmpty()) {
            builder?.addQueryParameter("auth", authCookie)
        }
        return builder?.build()?.toString() ?: "$serverUrl/api/stream?path=$path${if (isThumb) "&thumb=1" else ""}"
    }

    private fun parseMediaItems(jsonStr: String, parentPath: String): List<MediaItem> {
        val items = mutableListOf<MediaItem>()
        val trimmed = jsonStr.trim()

        val jsonArray = if (trimmed.startsWith("[")) {
            JSONArray(trimmed)
        } else if (trimmed.startsWith("{")) {
            val obj = JSONObject(trimmed)
            when {
                obj.has("items") -> obj.getJSONArray("items")
                obj.has("files") -> obj.getJSONArray("files")
                obj.has("data") -> obj.getJSONArray("data")
                obj.has("content") -> obj.getJSONArray("content")
                obj.has("list") -> obj.getJSONArray("list")
                else -> JSONArray()
            }
        } else {
            JSONArray()
        }

        for (i in 0 until jsonArray.length()) {
            val itemObj = jsonArray.opt(i) ?: continue

            if (itemObj is String) {
                val name = itemObj.substringAfterLast("/")
                val isDir = itemObj.endsWith("/")
                val itemPath = if (itemObj.startsWith("/")) itemObj else "$parentPath/$itemObj".replace("//", "/")
                items.add(
                    MediaItem(
                        name = name.removeSuffix("/"),
                        path = itemPath,
                        isDir = isDir,
                        size = 0L,
                        mtime = System.currentTimeMillis(),
                        mime = if (isDir) "directory" else guessMimeType(name)
                    )
                )
                continue
            }

            if (itemObj !is JSONObject) continue
            val obj = itemObj

            val name = when {
                obj.has("name") -> obj.optString("name")
                obj.has("filename") -> obj.optString("filename")
                obj.has("title") -> obj.optString("title")
                else -> "Unknown"
            }

            var path = when {
                obj.has("path") -> obj.optString("path")
                obj.has("filepath") -> obj.optString("filepath")
                obj.has("url") -> obj.optString("url")
                else -> "$parentPath/$name".replace("//", "/")
            }
            if (!path.startsWith("/")) {
                path = "/$path"
            }

            val isDir = when {
                obj.has("isDir") -> obj.optBoolean("isDir")
                obj.has("is_dir") -> obj.optBoolean("is_dir")
                obj.has("isDirectory") -> obj.optBoolean("isDirectory")
                obj.has("type") -> {
                    val t = obj.optString("type").lowercase()
                    t == "directory" || t == "dir" || t == "folder"
                }
                obj.has("mime") -> obj.optString("mime") == "directory"
                else -> false
            }

            val size = when {
                obj.has("size") -> obj.optLong("size")
                obj.has("length") -> obj.optLong("length")
                obj.has("bytes") -> obj.optLong("bytes")
                else -> 0L
            }

            var mtime = when {
                obj.has("mtime") -> obj.optLong("mtime")
                obj.has("modified") -> obj.optLong("modified")
                obj.has("updatedAt") -> obj.optLong("updatedAt")
                obj.has("timestamp") -> obj.optLong("timestamp")
                else -> System.currentTimeMillis()
            }
            // If Unix timestamp in seconds, convert to milliseconds
            if (mtime in 1..9999999999L) {
                mtime *= 1000L
            }

            val mime = when {
                obj.has("mime") -> obj.optString("mime")
                obj.has("mimeType") -> obj.optString("mimeType")
                obj.has("contentType") -> obj.optString("contentType")
                else -> if (isDir) "directory" else guessMimeType(name)
            }

            val sha1 = when {
                obj.has("sha1") -> obj.optString("sha1")
                obj.has("hash") -> obj.optString("hash")
                else -> null
            }

            // Skip hidden thumbs or dots
            if (name == ".thumbs" && isDir) {
                continue
            }

            items.add(
                MediaItem(
                    name = name,
                    path = path,
                    isDir = isDir,
                    size = size,
                    mtime = mtime,
                    mime = mime,
                    sha1 = sha1
                )
            )
        }
        return items
    }

    private fun guessMimeType(filename: String): String {
        val lower = filename.lowercase()
        return when {
            lower.endsWith(".jpg") || lower.endsWith(".jpeg") -> "image/jpeg"
            lower.endsWith(".png") -> "image/png"
            lower.endsWith(".webp") -> "image/webp"
            lower.endsWith(".gif") -> "image/gif"
            lower.endsWith(".heic") -> "image/heic"
            lower.endsWith(".mp4") -> "video/mp4"
            lower.endsWith(".mkv") -> "video/x-matroska"
            lower.endsWith(".mov") -> "video/quicktime"
            lower.endsWith(".avi") -> "video/x-msvideo"
            lower.endsWith(".webm") -> "video/webm"
            else -> "application/octet-stream"
        }
    }
}
