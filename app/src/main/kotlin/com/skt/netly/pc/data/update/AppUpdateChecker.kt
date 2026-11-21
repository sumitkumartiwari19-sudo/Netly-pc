package com.skt.netly.pc.data.update

import com.skt.netly.pc.AppInfo
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URI
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class AppUpdate(val version: String, val pageUrl: String, val notes: String)

sealed interface UpdateCheck {
    data class Available(val update: AppUpdate) : UpdateCheck
    data object UpToDate : UpdateCheck
    data class Failed(val message: String) : UpdateCheck
}

object Versions {
    private fun parts(v: String): List<Int> =
        v.trim().removePrefix("v").removePrefix("V").substringBefore('-').substringBefore('+').split('.').map { it.toIntOrNull() ?: 0 }

    /** True when [latest] is a higher version than [current]. A development build ("dev") is never told to update. */
    fun isNewer(latest: String, current: String): Boolean {
        if (current.equals("dev", ignoreCase = true)) return false
        val a = parts(latest)
        val b = parts(current)
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }
}

/**
 * Asks GitHub for the latest Netly release (public information, https only). It never downloads or installs anything:
 * the person opens the release page and installs the new version themselves.
 */
object AppUpdateChecker {
    private const val API = "https://api.github.com/repos/${AppInfo.REPO}/releases/latest"

    fun parse(json: String, current: String): UpdateCheck {
        val o = Json.parseToJsonElement(json).jsonObject
        val tag = o["tag_name"]?.jsonPrimitive?.contentOrNull ?: return UpdateCheck.Failed("No release found yet.")
        if (!Versions.isNewer(tag, current)) return UpdateCheck.UpToDate
        val page = o["html_url"]?.jsonPrimitive?.contentOrNull
            ?.takeIf { it.startsWith("${AppInfo.REPO_URL}/") } ?: AppInfo.RELEASES_URL
        val notes = o["body"]?.jsonPrimitive?.contentOrNull.orEmpty().trim().take(600)
        return UpdateCheck.Available(AppUpdate(tag.removePrefix("v"), page, notes))
    }

    suspend fun check(current: String = AppInfo.VERSION, proxy: String = ""): UpdateCheck = withContext(Dispatchers.IO) {
        try {
            val p = proxy.trim()
            val url = URL(API)
            val connection = (
                if (p.startsWith("http")) {
                    val uri = URI(p)
                    url.openConnection(Proxy(Proxy.Type.HTTP, InetSocketAddress(uri.host, if (uri.port > 0) uri.port else 8080)))
                } else {
                    url.openConnection()
                }
                ) as HttpURLConnection
            connection.connectTimeout = 8_000
            connection.readTimeout = 8_000
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("User-Agent", "Netly/${AppInfo.VERSION}")
            try {
                when (connection.responseCode) {
                    200 -> parse(connection.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }, current)
                    404 -> UpdateCheck.Failed("No release is published yet.")
                    else -> UpdateCheck.Failed("GitHub answered ${connection.responseCode}. Try again later.")
                }
            } finally {
                connection.disconnect()
            }
        } catch (e: Exception) {
            UpdateCheck.Failed("Could not reach GitHub. Check your internet connection.")
        }
    }
}
