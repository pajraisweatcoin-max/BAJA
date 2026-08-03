package com.example.data.remote

import com.example.data.local.SecureStorage
import com.example.data.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
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
            val jsonBody = JSONObject().apply {
                put("password", password)
            }.toString()

            val request = Request.Builder()
                .url("$serverUrl/api/login")
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful || response.code == 200) {
                val bodyStr = response.body?.string() ?: ""
                // Check if cookie was set or response indicates success
                true
            } else {
                throw Exception("HTTP Error ${response.code}: ${response.message}")
            }
        }
    }

    suspend fun listDirectory(baseUrl: String, path: String = "/"): Result<List<MediaItem>> = withContext(Dispatchers.IO) {
        runCatching {
            val serverUrl = normalizeUrl(baseUrl)
            val encodedPath = URLEncoder.encode(path, "UTF-8")
            val request = Request.Builder()
                .url("$serverUrl/api/list?path=$encodedPath")
                .get()
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                throw Exception("Gagal mengambil data folder (${response.code})")
            }

            val bodyStr = response.body?.string() ?: "[]"
            parseMediaItems(bodyStr, path)
        }
    }

    suspend fun listAllMediaRecursively(baseUrl: String, rootPath: String = "/"): Result<List<MediaItem>> = withContext(Dispatchers.IO) {
        runCatching {
            val resultList = mutableListOf<MediaItem>()
            val queue = ArrayDeque<String>()
            queue.add(rootPath)

            var depthCount = 0
            while (queue.isNotEmpty() && depthCount < 50) {
                val currentPath = queue.removeFirst()
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
            resultList.sortedByDescending { it.mtime }
        }
    }

    fun getStreamUrl(baseUrl: String, path: String, isThumb: Boolean = false): String {
        val serverUrl = normalizeUrl(baseUrl)
        val encodedPath = URLEncoder.encode(path, "UTF-8")
        return if (isThumb) {
            "$serverUrl/api/stream?path=$encodedPath&thumb=1"
        } else {
            "$serverUrl/api/stream?path=$encodedPath"
        }
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
                else -> JSONArray()
            }
        } else {
            JSONArray()
        }

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.optJSONObject(i) ?: continue
            val name = obj.optString("name", "Unknown")
            var path = obj.optString("path", "$parentPath/$name".replace("//", "/"))
            val isDir = obj.optBoolean("isDir", obj.optString("mime") == "directory" || obj.optBoolean("is_directory", false))
            val size = obj.optLong("size", 0L)
            val mtime = obj.optLong("mtime", System.currentTimeMillis())
            val mime = obj.optString("mime", if (isDir) "directory" else guessMimeType(name))
            val sha1 = if (obj.has("sha1")) obj.optString("sha1") else null

            // Skip .thumbs hidden directory in standard listing if returned
            if (name == ".thumbs" || name.startsWith(".")) {
                if (isDir && name == ".thumbs") {
                    // Filtered out by HTTP rule
                    continue
                }
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
