package com.skt.netly.pc.player

import com.skt.netly.pc.data.engine.EngineOptions
import com.skt.netly.pc.data.engine.EngineRepository
import com.skt.netly.pc.data.engine.ProcessRunner
import com.skt.netly.pc.data.engine.Innertube
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    /** True when the addresses came straight from YouTube's API (not yt-dlp). */
    val native: Boolean = false,
)

/** One stream the server offers. */
data class StreamCandidate(val url: String, val vcodec: String, val acodec: String, val height: Int, val abr: Double)

object StreamParser {
    /** Picks the best audio-only stream (up to about 160 kbps) and a video-only stream at or below [targetHeight]. */
    fun parse(json: String, targetHeight: Int): StreamUrls {
        val root = Json.parseToJsonElement(json).jsonObject
        val formats = (root["formats"] as? JsonArray).orEmpty().map { it.jsonObject }.filter { isHttp(it) && it.str("url") != null }
        val candidates = formats.map { f ->
            StreamCandidate(
                url = f.str("url")!!,
                vcodec = f.str("vcodec") ?: "none",
                acodec = f.str("acodec") ?: "none",
                height = f["height"]?.jsonPrimitive?.intOrNull ?: 0,
                abr = f["abr"]?.jsonPrimitive?.doubleOrNull ?: f["tbr"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
            )
        }
        val ua = (root["http_headers"] as? JsonObject)?.str("User-Agent")
        val duration = root["duration"]?.jsonPrimitive?.doubleOrNull?.toLong() ?: 0L
        return pick(candidates, targetHeight, duration, ua, native = false)
    }

    /** Reads the answer of YouTube's player API (adaptiveFormats and formats). Returns null if nothing playable is in it. */
    fun parseNative(json: String, targetHeight: Int, userAgent: String): StreamUrls? {
        val root = Json.parseToJsonElement(json).jsonObject
        if ((root["playabilityStatus"] as? JsonObject)?.str("status") != "OK") return null
        val streaming = root["streamingData"] as? JsonObject ?: return null
        val all = ((streaming["adaptiveFormats"] as? JsonArray).orEmpty() + (streaming["formats"] as? JsonArray).orEmpty()).map { it.jsonObject }
        val candidates = all.mapNotNull { f ->
            val url = f.str("url") ?: return@mapNotNull null
            val mime = f.str("mimeType") ?: return@mapNotNull null
            val codecs = Regex("codecs=\"([^\"]*)\"").find(mime)?.groupValues?.get(1)?.split(",")?.map { it.trim() }.orEmpty()
            val isVideo = mime.startsWith("video/")
            val isAudio = mime.startsWith("audio/")
            if (!isVideo && !isAudio) return@mapNotNull null
            val bitrate = (f["averageBitrate"] ?: f["bitrate"])?.jsonPrimitive?.doubleOrNull?.div(1000.0) ?: 0.0
            if (isAudio) {
                StreamCandidate(url, "none", codecs.firstOrNull() ?: "audio", 0, bitrate)
            } else {
                StreamCandidate(
                    url,
                    codecs.firstOrNull() ?: "video",
                    codecs.getOrNull(1) ?: "none",
                    f["height"]?.jsonPrimitive?.intOrNull ?: 0,
                    bitrate,
                )
            }
        }
        val duration = (root["videoDetails"] as? JsonObject)?.str("lengthSeconds")?.toLongOrNull() ?: 0L
        val result = pick(candidates, targetHeight, duration, userAgent, native = true)
        return if (result.audioUrl == null) null else result
    }

    private fun pick(list: List<StreamCandidate>, targetHeight: Int, duration: Long, ua: String?, native: Boolean): StreamUrls {
        val audioOnly = list.filter { it.vcodec == "none" && it.acodec != "none" }
        val videoOnly = list.filter { it.vcodec != "none" && it.acodec == "none" && it.height > 0 }
        val muxed = list.filter { it.vcodec != "none" && it.acodec != "none" && it.height > 0 }

        val audio = audioOnly.filter { it.abr <= 160.0 }.maxByOrNull { it.abr } ?: audioOnly.minByOrNull { it.abr }

        fun codecRank(c: StreamCandidate): Int = when {
            c.vcodec.startsWith("avc1") -> 0
            c.vcodec.startsWith("vp9") || c.vcodec.startsWith("vp09") -> 1
            c.vcodec.startsWith("av01") -> 3
            else -> 2
        }

        fun pickVideo(l: List<StreamCandidate>): StreamCandidate? {
            if (l.isEmpty()) return null
            val fitting = l.filter { it.height <= targetHeight }
            val h = if (fitting.isNotEmpty()) fitting.maxOf { it.height } else l.minOf { it.height }
            return l.filter { it.height == h }.minByOrNull { codecRank(it) }
        }

        val video = pickVideo(videoOnly)
        val combined = pickVideo(muxed)
        return when {
            video != null && audio != null -> StreamUrls(video.url, audio.url, duration, ua, native)
            combined != null -> StreamUrls(combined.url, combined.url, duration, ua, native)
            audio != null -> StreamUrls(null, audio.url, duration, ua, native)
            else -> StreamUrls(null, null, duration, ua, native)
        }
    }

    private fun isHttp(f: JsonObject): Boolean {
        val p = f.str("protocol") ?: return true
        return p == "https" || p == "http"
    }

    private fun JsonObject.str(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull
}

/** Finds stream addresses: first straight from YouTube's API (fast), otherwise with yt-dlp (slower, more robust). */
class StreamResolver(
    private val engine: EngineRepository,
    private val optionsProvider: () -> EngineOptions,
) {
    private class Cached(val at: Long, val json: String)

    private val cache = ConcurrentHashMap<String, Cached>()
    private val nativeOff = ConcurrentHashMap.newKeySet<String>()

    suspend fun resolve(videoUrl: String, targetHeight: Int): StreamUrls {
        val id = videoIdOf(videoUrl)
        if (id != null && videoUrl !in nativeOff) {
            val fast = runCatching { resolveNative(id, targetHeight) }.getOrNull()
            if (fast != null) return fast
        }
        val now = System.currentTimeMillis()
        val hit = cache[videoUrl]?.takeIf { now - it.at < 2 * 60 * 60 * 1000 }
        val json = hit?.json ?: fetch(videoUrl).also { remember(videoUrl, it) }
        return StreamParser.parse(json, targetHeight)
    }

    private suspend fun resolveNative(id: String, targetHeight: Int): StreamUrls? {
        val key = "n:$id"
        val now = System.currentTimeMillis()
        val hit = cache[key]?.takeIf { now - it.at < 2 * 60 * 60 * 1000 }
        val json = hit?.json ?: withContext(Dispatchers.IO) { Innertube.player(id, optionsProvider().proxy) }.also { remember(key, it) }
        val parsed = StreamParser.parseNative(json, targetHeight, Innertube.VR_USER_AGENT)
        if (parsed == null) cache.remove(key)
        return parsed
    }

    private fun remember(key: String, json: String) {
        cache[key] = Cached(System.currentTimeMillis(), json)
        if (cache.size > 12) cache.entries.minByOrNull { e -> e.value.at }?.let { old -> cache.remove(old.key) }
    }

    /** The direct addresses did not play: use yt-dlp for this video from now on. */
    fun disableNative(videoUrl: String) {
        nativeOff.add(videoUrl)
        videoIdOf(videoUrl)?.let { cache.remove("n:$it") }
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
        val result = ProcessRunner.run(command, 90_000)
        val text = result.stdout.trim()
        if (result.exitCode != 0 || !text.startsWith("{")) {
            error(result.stderr.lineSequence().lastOrNull { it.isNotBlank() }?.take(160) ?: "Could not load this video.")
        }
        return text
    }

    fun forget(videoUrl: String) {
        cache.remove(videoUrl)
    }

    companion object {
        private val idRegex = Regex("""(?:v=|youtu\.be/|/shorts/|/embed/)([A-Za-z0-9_-]{11})""")
        fun videoIdOf(url: String): String? = idRegex.find(url)?.groupValues?.get(1)
    }
}
