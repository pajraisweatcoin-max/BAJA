package com.example.data.remote

import com.example.data.local.SecureStorage
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

class BarraCookieJar(private val secureStorage: SecureStorage) : CookieJar {
    private val cookieMap = mutableMapOf<String, List<Cookie>>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        cookieMap[url.host] = cookies
        for (cookie in cookies) {
            if (cookie.name == "barra_auth") {
                secureStorage.saveAuthCookie(cookie.value)
            }
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val storedAuth = secureStorage.getAuthCookie()
        if (!storedAuth.isNullOrEmpty()) {
            val authCookie = Cookie.Builder()
                .domain(url.host)
                .path("/")
                .name("barra_auth")
                .value(storedAuth)
                .httpOnly()
                .build()
            return listOf(authCookie)
        }
        return cookieMap[url.host] ?: emptyList()
    }
}
