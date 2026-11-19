package com.skt.netly.pc.ui.downloads

import com.skt.netly.pc.domain.model.DownloadJob
import com.skt.netly.pc.domain.model.JobStatus
import com.skt.netly.pc.util.Formatters

enum class DownloadTab(val label: String) {
    All("All"),
    Active("Active"),
    Queued("Queued"),
    Completed("Completed"),
    Failed("Failed"),
    ;

    fun matches(job: DownloadJob): Boolean = when (this) {
        All -> true
        Active -> job.isRunning
        Queued -> job.status == JobStatus.Queued || job.status == JobStatus.Paused
        Completed -> job.status == JobStatus.Completed
        Failed -> job.status == JobStatus.Failed
    }
}

data class DownloadSummary(val totalSpeed: Long, val active: Int, val completed: Int)

object DownloadsLogic {
    fun summary(jobs: List<DownloadJob>) = DownloadSummary(
        totalSpeed = jobs.filter { it.isRunning }.sumOf { it.speedBytesPerSec ?: 0L },
        active = jobs.count { it.isRunning },
        completed = jobs.count { it.status == JobStatus.Completed },
    )

    /** Newest first, but running items stay on top. */
    fun visible(jobs: List<DownloadJob>, tab: DownloadTab): List<DownloadJob> =
        jobs.filter { tab.matches(it) }
            .sortedWith(compareByDescending<DownloadJob> { it.isRunning }.thenByDescending { it.id })

    /** Text under the progress bar, e.g. "42% · 3.2 MB/s · 0:35 left". */
    fun statusText(job: DownloadJob): String = when (job.status) {
        JobStatus.Queued -> "Waiting in the queue"
        JobStatus.Paused -> "Paused · ${percent(job)}%"
        JobStatus.Merging -> "Merging and finishing…"
        JobStatus.Completed -> "Completed" + (job.totalBytes?.let { " · " + Formatters.bytes(it) } ?: "")
        JobStatus.Failed -> job.errorMessage ?: "The download failed."
        JobStatus.Downloading -> buildString {
            append(percent(job)).append('%')
            job.speedBytesPerSec?.let { append(" · ").append(Formatters.bytes(it)).append("/s") }
            job.etaSec?.let { append(" · ").append(Formatters.duration(it)).append(" left") }
        }
    }

    private fun percent(job: DownloadJob): Int = (job.progress * 100).toInt().coerceIn(0, 100)
}
