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
