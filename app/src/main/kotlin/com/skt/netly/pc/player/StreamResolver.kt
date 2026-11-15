package com.skt.netly.pc.player

import com.skt.netly.pc.data.engine.EngineOptions
import com.skt.netly.pc.data.engine.EngineRepository
import com.skt.netly.pc.data.engine.ProcessRunner
import java.util.concurrent.ConcurrentHashMap
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Direct stream addresses. videoUrl may equal audioUrl (one combined stream or one local file). */
data class StreamUrls(
    val videoUrl: String?,
    val audioUrl: String?,
    val durationSec: Long,
    val userAgent: String?,
)

object StreamParser {
    /** Picks the best audio-only stream (up to about 160 kbps) and a video-only stream at or below [targetHeight]. */
    fun parse(json: String, targetHeight: Int): StreamUrls {
        val root = Json.parseToJsonElement(json).jsonObject
        val formats = (root["formats"] as? JsonArray).orEmpty().map { it.jsonObject }.filter { isHttp(it) && it.str("url") != null }

        fun vcodec(f: JsonObject) = f.str("vcodec") ?: "none"
        fun acodec(f: JsonObject) = f.str("acodec") ?: "none"
        fun height(f: JsonObject) = f["height"]?.jsonPrimitive?.intOrNull ?: 0
        fun abr(f: JsonObject) = f["abr"]?.jsonPrimitive?.doubleOrNull ?: f["tbr"]?.jsonPrimitive?.doubleOrNull ?: 0.0

        val audioOnly = formats.filter { vcodec(it) == "none" && acodec(it) != "none" }
        val videoOnly = formats.filter { vcodec(it) != "none" && acodec(it) == "none" && height(it) > 0 }
        val muxed = formats.filter { vcodec(it) != "none" && acodec(it) != "none" && height(it) > 0 }

        val audio = audioOnly.filter { abr(it) <= 160.0 }.maxByOrNull { abr(it) } ?: audioOnly.minByOrNull { abr(it) }

        fun codecRank(f: JsonObject): Int = when {
            vcodec(f).startsWith("avc1") -> 0
            vcodec(f).startsWith("vp9") || vcodec(f).startsWith("vp09") -> 1
            vcodec(f).startsWith("av01") -> 3
            else -> 2
        }

        fun pickVideo(list: List<JsonObject>): JsonObject? {
            if (list.isEmpty()) return null
            val fitting = list.filter { height(it) <= targetHeight }
            val h = if (fitting.isNotEmpty()) fitting.maxOf { height(it) } else list.minOf { height(it) }
            return list.filter { height(it) == h }.minByOrNull { codecRank(it) }
        }

        val video = pickVideo(videoOnly)
        val combined = pickVideo(muxed)
        val ua = (root["http_headers"] as? JsonObject)?.str("User-Agent")
        val duration = root["duration"]?.jsonPrimitive?.doubleOrNull?.toLong() ?: 0L

        return when {
            video != null && audio != null -> StreamUrls(video.str("url"), audio.str("url"), duration, ua)
            combined != null -> StreamUrls(combined.str("url"), combined.str("url"), duration, ua)
            audio != null -> StreamUrls(null, audio.str("url"), duration, ua)
            else -> StreamUrls(null, null, duration, ua)
        }
    }

    private fun isHttp(f: JsonObject): Boolean {
        val p = f.str("protocol") ?: return true
        return p == "https" || p == "http"
    }

    private fun JsonObject.str(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull
}

/** Asks yt-dlp for stream addresses. The JSON answer is kept for a while so changing quality is quick. */
class StreamResolver(
    private val engine: EngineRepository,
    private val optionsProvider: () -> EngineOptions,
) {
    private class Cached(val at: Long, val json: String)

    private val cache = ConcurrentHashMap<String, Cached>()

    suspend fun resolve(videoUrl: String, targetHeight: Int): StreamUrls {
        val now = System.currentTimeMillis()
        val hit = cache[videoUrl]?.takeIf { now - it.at < 2 * 60 * 60 * 1000 }
        val json = hit?.json ?: fetch(videoUrl).also {
            cache[videoUrl] = Cached(now, it)
            if (cache.size > 8) cache.entries.minByOrNull { e -> e.value.at }?.let { old -> cache.remove(old.key) }
        }
        return StreamParser.parse(json, targetHeight)
    }

    private suspend fun fetch(videoUrl: String): String {
        val exe = engine.awaitReady()
        val command = buildList {
            add(exe.absolutePath)
            addAll(listOf("--ignore-config", "--no-warnings", "--no-colors", "--no-playlist", "-J"))
            addAll(optionsProvider().networkArgs(includeRate = false))
            add("--")
            add(videoUrl)
        }
        val result = ProcessRunner.run(command, 60_000)
        val text = result.stdout.trim()
        if (result.exitCode != 0 || !text.startsWith("{")) {
            error(result.stderr.lineSequence().lastOrNull { it.isNotBlank() }?.take(160) ?: "Could not load this video.")
        }
        return text
    }

    fun forget(videoUrl: String) {
        cache.remove(videoUrl)
    }
}
