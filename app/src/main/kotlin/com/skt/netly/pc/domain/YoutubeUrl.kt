package com.skt.netly.pc.domain

import java.net.URI
import java.net.URLDecoder

sealed interface YoutubeLink {
    data class Video(val id: String, val canonicalUrl: String) : YoutubeLink
    data object PlaylistOnly : YoutubeLink
    data object Invalid : YoutubeLink
}

/** Accepts only YouTube hosts and rebuilds a clean watch URL, so nothing else is ever passed to yt-dlp. */
object YoutubeUrl {
    private val hosts = setOf("youtube.com", "www.youtube.com", "m.youtube.com", "music.youtube.com", "youtu.be")
    private val idRegex = Regex("^[A-Za-z0-9_-]{11}$")
    private val listRegex = Regex("[?&]list=([A-Za-z0-9_-]+)")

    /** The playlist id inside a link like youtube.com/playlist?list=PL..., or null. */
    fun playlistId(input: String): String? = listRegex.find(input.trim())?.groupValues?.get(1)

    fun parse(raw: String): YoutubeLink {
        val text = raw.trim()
        if (text.isEmpty() || text.any { it.isWhitespace() }) return YoutubeLink.Invalid

        val withScheme = if (text.startsWith("http://") || text.startsWith("https://")) text else "https://$text"
        val uri = runCatching { URI(withScheme) }.getOrNull() ?: return YoutubeLink.Invalid
        val host = uri.host?.lowercase() ?: return YoutubeLink.Invalid
        if (host !in hosts) return YoutubeLink.Invalid

        val query = parseQuery(uri.rawQuery)
        val segments = uri.path.orEmpty().split('/').filter { it.isNotEmpty() }
        val first = segments.firstOrNull()

        val id: String? = when {
            host == "youtu.be" -> first
            first == "watch" -> query["v"]
            first == "shorts" || first == "live" || first == "embed" || first == "v" -> segments.getOrNull(1)
            else -> null
        }
        if (id != null && idRegex.matches(id)) {
            return YoutubeLink.Video(id, "https://www.youtube.com/watch?v=$id")
        }
        if (first == "playlist" && !query["list"].isNullOrBlank()) return YoutubeLink.PlaylistOnly
        return YoutubeLink.Invalid
    }

    private fun parseQuery(rawQuery: String?): Map<String, String> =
        rawQuery.orEmpty().split('&').mapNotNull { part ->
            val index = part.indexOf('=')
            if (index <= 0) null else part.substring(0, index) to URLDecoder.decode(part.substring(index + 1), "UTF-8")
        }.toMap()
}
