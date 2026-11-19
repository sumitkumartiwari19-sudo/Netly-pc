package com.skt.netly.pc.domain.model

enum class JobStatus { Queued, Downloading, Merging, Paused, Completed, Failed }

/** Everything needed to run one download. Stored with the job so it can be resumed after a restart. */
data class DownloadRequest(
    val videoId: String,
    val url: String,
    val title: String,
    val channel: String,
    val thumbnailUrl: String,
    val durationSec: Long,
    val audioOnly: Boolean,
    /** Maximum video height, or null for the best available. Ignored in audio mode. */
    val height: Int?,
    /** Audio choice key: mp3-320, mp3-192, m4a or opus. Used in audio mode. */
    val audioKey: String,
    /** MP4, WEBM or MKV. Used in video mode. */
    val container: String,
    val embedThumbnail: Boolean,
    val embedSubtitles: Boolean,
    val outputDir: String,
) {
    /** Short text for the quality shown on a download row, e.g. "1080p · MP4" or "MP3 320 kbps". */
    val qualityLabel: String
        get() = if (audioOnly) {
            when (audioKey) {
                "mp3-320" -> "MP3 320 kbps"
                "mp3-192" -> "MP3 192 kbps"
                "m4a" -> "M4A"
                "opus" -> "OPUS"
                else -> audioKey.uppercase()
            }
        } else {
            (if (height != null) "${height}p" else "Best") + " · " + container.uppercase()
        }
}

data class DownloadJob(
    val id: Long,
    val request: DownloadRequest,
    val status: JobStatus = JobStatus.Queued,
    /** 0.0 to 1.0. */
    val progress: Float = 0f,
    val speedBytesPerSec: Long? = null,
    val etaSec: Long? = null,
    val downloadedBytes: Long = 0,
    val totalBytes: Long? = null,
    val filePath: String? = null,
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    /** True for "Download" (goes before queued items); false for "Add to queue". Not saved. */
    val priority: Boolean = false,
) {
    val isRunning: Boolean get() = status == JobStatus.Downloading || status == JobStatus.Merging
    val isFinished: Boolean get() = status == JobStatus.Completed || status == JobStatus.Failed
}
