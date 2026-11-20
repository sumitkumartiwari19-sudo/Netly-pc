package com.skt.netly.pc.data.engine

import com.skt.netly.pc.domain.model.MediaInfo
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

/** Turns YouTube's player answer into [MediaInfo] (qualities and sizes) by reusing the yt-dlp parser. */
object NativeInfo {
    fun toInfo(playerJson: String): MediaInfo? = runCatching {
        val root = Json.parseToJsonElement(playerJson).jsonObject
        if ((root["playabilityStatus"] as? JsonObject)?.str("status") != "OK") return null
        val details = root["videoDetails"] as? JsonObject ?: return null
        val id = details.str("videoId") ?: return null
        val streaming = root["streamingData"] as? JsonObject ?: return null
        val all = ((streaming["adaptiveFormats"] as? JsonArray).orEmpty() + (streaming["formats"] as? JsonArray).orEmpty())
            .mapNotNull { it as? JsonObject }
        val upload = ((root["microformat"] as? JsonObject)?.get("playerMicroformatRenderer") as? JsonObject)
            ?.str("uploadDate")?.take(10)?.replace("-", "")

        val formats = buildJsonArray {
            all.forEach { f ->
                val mime = f.str("mimeType") ?: return@forEach
                val codecs = Regex("codecs=\"([^\"]*)\"").find(mime)?.groupValues?.get(1)?.split(",")?.map { it.trim() }.orEmpty()
                val isVideo = mime.startsWith("video/")
                val isAudio = mime.startsWith("audio/")
                if (!isVideo && !isAudio) return@forEach
                val muxed = isVideo && codecs.size > 1
                val container = mime.substringAfter("/").substringBefore(";")
                add(
                    buildJsonObject {
                        put("vcodec", if (isVideo) (codecs.firstOrNull() ?: "video") else "none")
                        put("acodec", if (isAudio) (codecs.firstOrNull() ?: "audio") else if (muxed) codecs[1] else "none")
                        put("ext", if (isAudio && container == "mp4") "m4a" else container)
                        f["height"]?.let { put("height", it) }
                        f.str("contentLength")?.toLongOrNull()?.let { put("filesize", it) }
                        (f["averageBitrate"] ?: f["bitrate"])?.let { b ->
                            (b as? JsonPrimitive)?.doubleOrNull?.let { put("abr", it / 1000.0) }
                        }
                    },
                )
            }
        }
        val out = buildJsonObject {
            put("id", id)
            put("title", details.str("title") ?: "Untitled video")
            put("channel", details.str("author") ?: "")
            details.str("lengthSeconds")?.toLongOrNull()?.let { put("duration", it) }
            details.str("viewCount")?.toLongOrNull()?.let { put("view_count", it) }
            upload?.let { put("upload_date", it) }
            put("is_live", details["isLiveContent"]?.let { (it as? JsonPrimitive)?.contentOrNull == "true" && details.str("lengthSeconds") == "0" } ?: false)
            put("formats", formats)
        }
        val info = MediaInfoParser.parse(Json.encodeToString(JsonObject.serializer(), out))
        if (info.videoQualities.isEmpty()) null else info
    }.getOrNull()

    private fun JsonObject.str(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull
}
