package com.skt.netly.pc.domain.model

/** One search result or playlist entry. Playlists have [isPlaylist] set and [url] pointing at the playlist. */
data class SearchItem(
    val id: String,
    val title: String,
    val channel: String,
    val durationSec: Long?,
    val thumbnailUrl: String,
    val isPlaylist: Boolean,
    val url: String,
    val count: Int? = null,
)

/** What the player window should open: a downloaded file, or a YouTube video to stream. */
data class PlayRequest(
    val title: String,
    val videoUrl: String? = null,
    val videoId: String? = null,
    val filePath: String? = null,
)
