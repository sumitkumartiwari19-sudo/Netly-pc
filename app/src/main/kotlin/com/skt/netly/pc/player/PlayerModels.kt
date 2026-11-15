package com.skt.netly.pc.player

import com.skt.netly.pc.data.library.LibraryRepository
import java.io.File

/** One thing in the play queue: a YouTube video (url) or a downloaded file (filePath). */
data class PlayItem(
    val key: String,
    val title: String,
    val channel: String,
    val thumbnailUrl: String,
    val durationSec: Long,
    val url: String? = null,
    val filePath: String? = null,
) {
    /** Audio files have no picture to show. */
    val isAudioFile: Boolean
        get() = filePath != null && File(filePath).extension.lowercase() in LibraryRepository.AUDIO_EXT
}

enum class PlayStatus { Idle, Loading, Playing, Paused, Ended, Error }

data class PlayerState(
    val queue: List<PlayItem> = emptyList(),
    val index: Int = 0,
    val status: PlayStatus = PlayStatus.Idle,
    val positionSec: Double = 0.0,
    val durationSec: Long = 0,
    val speed: Float = 1f,
    val quality: Int = 360,
    val audioOnly: Boolean = false,
    val volume: Float = 0.8f,
    val autoplay: Boolean = true,
    val error: String? = null,
) {
    val item: PlayItem? get() = queue.getOrNull(index)
    val hasNext: Boolean get() = index < queue.lastIndex
    val hasPrevious: Boolean get() = index > 0
    val effectiveAudioOnly: Boolean get() = audioOnly || item?.isAudioFile == true
}

object PlayerChoices {
    val speeds = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f, 3f, 4f)
    val qualities = listOf(144, 240, 360, 480, 720)

    fun speedLabel(s: Float): String = (if (s == s.toInt().toFloat()) s.toInt().toString() else s.toString()) + "x"
}
