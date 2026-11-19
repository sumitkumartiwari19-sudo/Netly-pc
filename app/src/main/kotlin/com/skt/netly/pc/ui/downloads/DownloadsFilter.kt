package com.skt.netly.pc.ui.downloads

import com.skt.netly.pc.domain.model.DownloadJob
import com.skt.netly.pc.domain.model.JobStatus

enum class DownloadFilter(val label: String) {
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

fun List<DownloadJob>.filteredBy(filter: DownloadFilter): List<DownloadJob> =
    filter { filter.matches(it) }.sortedByDescending { it.id }
