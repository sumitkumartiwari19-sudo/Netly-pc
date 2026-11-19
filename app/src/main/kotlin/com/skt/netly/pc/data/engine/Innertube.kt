package com.skt.netly.pc.data.engine

import com.skt.netly.pc.domain.model.SearchItem
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URI
import java.net.URL
import java.util.zip.GZIPInputStream
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Talks to YouTube's own web API directly. It answers in well under a second, while starting yt-dlp.exe
 * takes several seconds on older PCs. Anything unexpected makes the caller fall back to yt-dlp.
 */
object Innertube {
    const val WEB_USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36"
    const val VR_USER_AGENT =
        "com.google.android.apps.youtube.vr.oculus/1.60.19 (Linux; U; Android 12L; eureka-user Build/SQ3A.220605.009.A1) gzip"

    private const val BASE = "https://www.youtube.com/youtubei/v1"
    private const val WEB_KEY = "AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8"
    private const val VR_KEY = "AIzaSyA8eiZmM1FaDVjRy-df2KTyQ_vz_yYM39w"

    fun search(query: String, proxy: String): String {
        val body = """{"context":{"client":{"clientName":"WEB","clientVersion":"2.20251020.01.00","hl":"en","gl":"IN"}},"query":${Json.encodeToString(String.serializer(), query)}}"""
        return post("$BASE/search?prettyPrint=false&key=$WEB_KEY", body, WEB_USER_AGENT, proxy, 9_000, emptyMap())
    }

    fun player(videoId: String, proxy: String): String {
        val body = """{"context":{"client":{"clientName":"ANDROID_VR","clientVersion":"1.60.19","deviceMake":"Oculus","deviceModel":"Quest 3","androidSdkVersion":32,"osName":"Android","osVersion":"12L","hl":"en","gl":"IN"}},"videoId":"$videoId","contentCheckOk":true,"racyCheckOk":true}"""
        return post(
            "$BASE/player?prettyPrint=false&key=$VR_KEY", body, VR_USER_AGENT, proxy, 9_000,
            mapOf("X-YouTube-Client-Name" to "28", "X-YouTube-Client-Version" to "1.60.19"),
        )
    }

    private fun post(url: String, body: String, userAgent: String, proxy: String, timeoutMs: Int, extra: Map<String, String>): String {
        val connection = (openConnection(url, proxy) as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = timeoutMs
            readTimeout = timeoutMs
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("User-Agent", userAgent)
            setRequestProperty("Origin", "https://www.youtube.com")
            setRequestProperty("Accept-Encoding", "gzip")
            extra.forEach { (k, v) -> setRequestProperty(k, v) }
        }
        try {
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            if (connection.responseCode != 200) error("HTTP ${connection.responseCode}")
            val raw = connection.inputStream
            val stream = if (connection.contentEncoding.equals("gzip", ignoreCase = true)) GZIPInputStream(raw) else raw
            return stream.use { it.readBytes().toString(Charsets.UTF_8) }
        } finally {
            connection.disconnect()
        }
    }

    private fun openConnection(url: String, proxy: String) = runCatching {
        val p = proxy.trim()
        if (p.startsWith("http")) {
            val uri = URI(p)
            URL(url).openConnection(Proxy(Proxy.Type.HTTP, InetSocketAddress(uri.host, if (uri.port > 0) uri.port else 8080)))
        } else {
            URL(url).openConnection()
        }
    }.getOrElse { URL(url).openConnection() }
}

/** Reads the search answer. Videos and playlists keep YouTube's own order. */
object InnertubeSearchParser {
    fun parse(json: String): List<SearchItem> {
        val found = LinkedHashMap<String, SearchItem>()
        walk(Json.parseToJsonElement(json)) { name, o ->
            val item = when (name) {
                "videoRenderer" -> video(o)
                "playlistRenderer" -> playlist(o)
                "lockupViewModel" -> lockup(o)
                else -> null
            }
            if (item != null) found.putIfAbsent((if (item.isPlaylist) "p" else "v") + item.id, item)
        }
        return found.values.toList()
    }

    private fun walk(e: JsonElement, visit: (String, JsonObject) -> Unit) {
        when (e) {
            is JsonObject -> e.forEach { (k, v) ->
                if (v is JsonObject && (k == "videoRenderer" || k == "playlistRenderer" || k == "lockupViewModel")) visit(k, v) else walk(v, visit)
            }
            is JsonArray -> e.forEach { walk(it, visit) }
            else -> Unit
        }
    }

    private fun video(o: JsonObject): SearchItem? {
        val id = o.str("videoId") ?: return null
        val title = text(o["title"]) ?: return null
        val channel = text(o["ownerText"]) ?: text(o["longBylineText"]) ?: text(o["shortBylineText"]).orEmpty()
        return SearchItem(
            id = id,
            title = title,
            channel = channel,
            durationSec = parseClock(text(o["lengthText"])),
            thumbnailUrl = "https://i.ytimg.com/vi/$id/mqdefault.jpg",
            isPlaylist = false,
            url = "https://www.youtube.com/watch?v=$id",
        )
    }

    private fun playlist(o: JsonObject): SearchItem? {
        val id = o.str("playlistId") ?: return null
        val title = text(o["title"]) ?: return null
        val firstVideo = o["navigationEndpoint"]?.at("watchEndpoint")?.str("videoId")
        val thumb = if (firstVideo != null) "https://i.ytimg.com/vi/$firstVideo/mqdefault.jpg" else ""
        return SearchItem(
            id = id,
            title = title,
            channel = text(o["shortBylineText"]).orEmpty(),
            durationSec = null,
            thumbnailUrl = thumb,
            isPlaylist = true,
            url = "https://www.youtube.com/playlist?list=$id",
            count = o.str("videoCount")?.filter { it.isDigit() }?.toIntOrNull(),
        )
    }

    private fun lockup(o: JsonObject): SearchItem? {
        val id = o.str("contentId") ?: return null
        val type = o.str("contentType") ?: return null
        val meta = o["metadata"]?.at("lockupMetadataViewModel")
        val title = meta?.at("title")?.str("content") ?: return null
        val channel = runCatching {
            meta.at("metadata")?.at("contentMetadataViewModel")?.get("metadataRows")?.jsonArray?.firstOrNull()
                ?.jsonObject?.get("metadataParts")?.jsonArray?.firstOrNull()?.jsonObject?.at("text")?.str("content")
        }.getOrNull().orEmpty()
        return when {
            type.endsWith("PLAYLIST") -> {
                val thumb = runCatching {
                    o["contentImage"]?.at("collectionThumbnailViewModel")?.at("primaryThumbnail")?.at("thumbnailViewModel")
                        ?.at("image")?.get("sources")?.jsonArray?.firstOrNull()?.jsonObject?.str("url")
                }.getOrNull().orEmpty()
                SearchItem(id, title, channel, null, thumb, true, "https://www.youtube.com/playlist?list=$id")
            }
            type.endsWith("VIDEO") && id.length == 11 ->
                SearchItem(id, title, channel, null, "https://i.ytimg.com/vi/$id/mqdefault.jpg", false, "https://www.youtube.com/watch?v=$id")
            else -> null
        }
    }

    /** "simpleText" or the joined "runs" of a text object. */
    private fun text(e: JsonElement?): String? {
        val o = e as? JsonObject ?: return null
        o.str("simpleText")?.let { return it }
        val runs = (o["runs"] as? JsonArray) ?: return null
        return runs.joinToString("") { it.jsonObject.str("text").orEmpty() }.ifBlank { null }
    }

    private fun JsonObject.str(key: String): String? = (this[key] as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull
    private fun JsonElement.at(key: String): JsonObject? = (this as? JsonObject)?.get(key) as? JsonObject

    fun parseClock(text: String?): Long? {
        if (text.isNullOrBlank()) return null
        val parts = text.trim().split(":").map { it.toLongOrNull() ?: return null }
        return parts.fold(0L) { acc, p -> acc * 60 + p }
    }
}
