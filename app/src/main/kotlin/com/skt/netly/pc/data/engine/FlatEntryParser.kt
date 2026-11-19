package com.skt.netly.pc.data.engine

import com.skt.netly.pc.domain.model.SearchItem
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class FlatResult(val title: String?, val items: List<SearchItem>)

/** Reads the output of `yt-dlp --flat-playlist -J` for searches and playlists. */
object FlatEntryParser {
    private val videoId = Regex("^[A-Za-z0-9_-]{11}$")

    fun parse(json: String): FlatResult {
        val root = Json.parseToJsonElement(json).jsonObject
        val entries = root["entries"] as? JsonArray ?: return FlatResult(str(root, "title"), emptyList())
        val items = entries.mapNotNull { element -> (element as? JsonObject)?.let { toItem(it) } }
        return FlatResult(str(root, "title"), items)
    }

    private fun str(o: JsonObject, key: String): String? =
        (o[key] as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull?.takeIf { it != "null" }

    private fun toItem(o: JsonObject): SearchItem? {
        val id = str(o, "id") ?: return null
        val title = str(o, "title") ?: return null
        if (title == "[Private video]" || title == "[Deleted video]") return null
        val url = str(o, "url").orEmpty()
        val channel = str(o, "channel") ?: str(o, "uploader") ?: ""
        val duration = (o["duration"] as? kotlinx.serialization.json.JsonPrimitive)?.doubleOrNull?.toLong()
        val isPlaylist = str(o, "ie_key") == "YoutubeTab" || "list=" in url || (!videoId.matches(id) && "playlist" in url)

        if (isPlaylist) {
            val thumbs = o["thumbnails"] as? JsonArray
            val thumb = thumbs?.lastOrNull()?.let { (it as? JsonObject)?.let { t -> str(t, "url") } }.orEmpty()
            val count = (o["playlist_count"] as? kotlinx.serialization.json.JsonPrimitive)?.intOrNull
                ?: (o["n_entries"] as? kotlinx.serialization.json.JsonPrimitive)?.intOrNull
            val playlistUrl = if (url.startsWith("http")) url else "https://www.youtube.com/playlist?list=$id"
            return SearchItem(id, title, channel, null, thumb, true, playlistUrl, count)
        }
        if (!videoId.matches(id)) return null
        return SearchItem(
            id = id,
            title = title,
            channel = channel,
            durationSec = duration,
            thumbnailUrl = "https://i.ytimg.com/vi/$id/mqdefault.jpg",
            isPlaylist = false,
            url = "https://www.youtube.com/watch?v=$id",
        )
    }
}
