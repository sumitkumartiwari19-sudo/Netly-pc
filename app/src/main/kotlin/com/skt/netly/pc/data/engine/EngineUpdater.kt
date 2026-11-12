package com.skt.netly.pc.data.engine

import java.io.IOException
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class UpdateResult(val ok: Boolean, val message: String)

/** Updates the working copy of yt-dlp with its built-in updater (it verifies the download itself). */
class EngineUpdater(private val engine: EngineRepository) {
    suspend fun update(): UpdateResult {
        val exe = try {
            engine.awaitReady()
        } catch (e: FetchException) {
            return UpdateResult(false, e.error.message)
        }
        return try {
            val result = ProcessRunner.run(listOf(exe.absolutePath, "-U", "--no-colors"), 180_000)
            val last = (result.stdout + "\n" + result.stderr).lineSequence().map { it.trim() }.lastOrNull { it.isNotEmpty() }.orEmpty()
            engine.prepare()
            if (result.exitCode == 0) {
                val version = (engine.status.value as? EngineStatus.Ready)?.version
                val message = when {
                    "up to date" in last.lowercase() -> "yt-dlp is up to date" + (version?.let { " ($it)" } ?: "")
                    else -> "yt-dlp updated" + (version?.let { " to $it" } ?: "")
                }
                UpdateResult(true, message)
            } else {
                UpdateResult(false, "Update failed. " + last.take(120))
            }
        } catch (e: ProcessTimeoutException) {
            UpdateResult(false, "The update took too long. Check your connection.")
        } catch (e: IOException) {
            UpdateResult(false, "Could not start the updater.")
        }
    }
}

object ConnectionTester {
    suspend fun test(proxy: String): String = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://www.youtube.com/generate_204")
            val hostPort = proxy.trim().removePrefix("http://").removePrefix("https://").substringBefore('/')
            val host = hostPort.substringBeforeLast(':', "")
            val port = hostPort.substringAfterLast(':', "").toIntOrNull()
            val conn = if (host.isNotEmpty() && port != null) {
                url.openConnection(Proxy(Proxy.Type.HTTP, InetSocketAddress(host, port)))
            } else {
                url.openConnection()
            } as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            val start = System.currentTimeMillis()
            val code = conn.responseCode
            conn.disconnect()
            if (code in 200..399) "Connected to YouTube (${System.currentTimeMillis() - start} ms)"
            else "YouTube answered with code $code"
        } catch (e: Exception) {
            "Could not reach YouTube. Check your internet or proxy."
        }
    }
}
