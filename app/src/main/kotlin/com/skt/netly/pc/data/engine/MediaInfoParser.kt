package com.skt.netly.pc.data.engine

import com.skt.netly.pc.domain.model.AudioFormat
import com.skt.netly.pc.domain.model.MediaInfo
import com.skt.netly.pc.domain.model.VideoQuality
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject

/** Reads the JSON printed by `yt-dlp -J` and builds the quality and size options shown on the Home screen. */
object MediaInfoParser {
    private val standardHeights = listOf(2160, 1440, 1080, 720, 480, 360)

    private data class Stream(
        val height: Int?,
        val hasVideo: Boolean,
        val hasAudio: Boolean,
        val bytes: Long?,
        val abr: Double?,
        val ext: String?,
    )

    fun parse(jsonText: String): MediaInfo {
        val root = Json.parseToJsonElement(jsonText).jsonObject
        val id = root.str("id") ?: throw IllegalArgumentException("Missing video id")
        val durationSec = root.num("duration")?.toLong() ?: 0L

        val streams = ((root["formats"] as? JsonArray) ?: JsonArray(emptyList()))
            .mapNotNull { it as? JsonObject }
            .map { toStream(it) }

        return MediaInfo(
            id = id,
            url = "https://www.youtube.com/watch?v=$id",
            title = root.str("title") ?: "Untitled video",
            channel = root.str("channel") ?: root.str("uploader") ?: "Unknown channel",
            durationSec = durationSec,
            viewCount = root.num("view_count")?.toLong(),
            uploadDate = root.str("upload_date"),
            thumbnailUrl = "https://i.ytimg.com/vi/$id/hqdefault.jpg",
            videoQualities = videoQualities(streams),
            audioFormats = audioFormats(streams, durationSec),
            subtitleLanguages = (root["subtitles"] as? JsonObject)?.size ?: 0,
            chapterCount = (root["chapters"] as? JsonArray)?.size ?: 0,
            isLive = (root["is_live"] as? JsonPrimitive)?.booleanOrNull == true,
        )
    }

    private fun toStream(o: JsonObject): Stream {
        val vcodec = o.str("vcodec")
        val acodec = o.str("acodec")
        return Stream(
            height = o.num("height")?.toInt(),
            hasVideo = vcodec != null && vcodec != "none",
            hasAudio = acodec != null && acodec != "none",
            bytes = o.num("filesize")?.toLong() ?: o.num("filesize_approx")?.toLong(),
            abr = o.num("abr"),
            ext = o.str("ext"),
        )
    }

    private fun videoQualities(streams: List<Stream>): List<VideoQuality> {
        val video = streams.filter { it.hasVideo && it.height != null }
        val bestAudioBytes = streams.filter { it.hasAudio && !it.hasVideo }.mapNotNull { it.bytes }.maxOrNull()

        val heights = video.mapNotNull { it.height }.distinct().sortedDescending()
        val chosen = heights.filter { it in standardHeights }.ifEmpty { heights.take(6) }

        return chosen.map { height ->
            val atHeight = video.filter { it.height == height }
            val preferred = atHeight.filter { it.ext == "mp4" }.ifEmpty { atHeight }
            val videoBytes = preferred.mapNotNull { it.bytes }.maxOrNull()
            val needsAudio = preferred.any { !it.hasAudio }
            val total = videoBytes?.let { it + if (needsAudio) (bestAudioBytes ?: 0L) else 0L }
            VideoQuality(height, qualityLabel(height), total)
        }
    }

    private fun audioFormats(streams: List<Stream>, durationSec: Long): List<AudioFormat> {
        fun estimate(kbps: Int): Long? = if (durationSec > 0) durationSec * kbps * 1000L / 8 else null
        fun bestKbps(ext: String): Int? =
            streams.filter { it.hasAudio && !it.hasVideo && it.ext == ext }.mapNotNull { it.abr }.maxOrNull()?.toInt()

        val m4a = bestKbps("m4a") ?: 128
        val opus = bestKbps("webm") ?: 128
        return listOf(
            AudioFormat("mp3-320", "MP3 · 320k", "mp3", 320, estimate(320)),
            AudioFormat("mp3-192", "MP3 · 192k", "mp3", 192, estimate(192)),
            AudioFormat("m4a", "M4A · AAC", "m4a", m4a, estimate(m4a)),
            AudioFormat("opus", "Opus", "opus", opus, estimate(opus)),
        )
    }

    private fun qualityLabel(height: Int): String = if (height >= 2160) "${height}p · 4K" else "${height}p"

    private fun JsonObject.str(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull

    private fun JsonObject.num(key: String): Double? = (this[key] as? JsonPrimitive)?.doubleOrNull
}
