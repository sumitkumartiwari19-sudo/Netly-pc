package com.skt.netly.pc.domain.model

data class VideoQuality(
    val height: Int,
    val label: String,
    /** Estimated final size (video plus audio) in bytes, or null when YouTube does not report it. */
    val estimatedBytes: Long?,
)

data class AudioFormat(
    val key: String,
    val label: String,
    val ext: String,
    val bitrateKbps: Int,
    val estimatedBytes: Long?,
)

data class MediaInfo(
    val id: String,
    val url: String,
    val title: String,
    val channel: String,
    val durationSec: Long,
    val viewCount: Long?,
    /** yt-dlp format: yyyyMMdd. */
    val uploadDate: String?,
    val thumbnailUrl: String,
    val videoQualities: List<VideoQuality>,
    val audioFormats: List<AudioFormat>,
    val subtitleLanguages: Int,
    val chapterCount: Int,
    val isLive: Boolean,
) {
    val maxHeight: Int get() = videoQualities.maxOfOrNull { it.height } ?: 0
}
