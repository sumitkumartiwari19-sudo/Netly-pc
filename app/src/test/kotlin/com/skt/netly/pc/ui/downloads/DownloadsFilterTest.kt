package com.skt.netly.pc.ui.downloads

import com.skt.netly.pc.domain.model.DownloadJob
import com.skt.netly.pc.domain.model.DownloadRequest
import com.skt.netly.pc.domain.model.JobStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class DownloadsFilterTest {
    private val req = DownloadRequest(
        videoId = "abcdefghijk", url = "u", title = "t", channel = "c", thumbnailUrl = "", durationSec = 1,
        audioOnly = false, height = 720, audioKey = "mp3-320", container = "MP4",
        embedThumbnail = false, embedSubtitles = false, outputDir = "out",
    )
    private fun job(id: Long, status: JobStatus) = DownloadJob(id = id, request = req, status = status)

    private val jobs = listOf(
        job(1, JobStatus.Downloading), job(2, JobStatus.Merging), job(3, JobStatus.Queued),
        job(4, JobStatus.Paused), job(5, JobStatus.Completed), job(6, JobStatus.Failed),
    )

    @Test
    fun filtersByTab() {
        assertEquals(6, jobs.filteredBy(DownloadFilter.All).size)
        assertEquals(listOf(2L, 1L), jobs.filteredBy(DownloadFilter.Active).map { it.id })
        assertEquals(listOf(4L, 3L), jobs.filteredBy(DownloadFilter.Queued).map { it.id })
        assertEquals(listOf(5L), jobs.filteredBy(DownloadFilter.Completed).map { it.id })
        assertEquals(listOf(6L), jobs.filteredBy(DownloadFilter.Failed).map { it.id })
    }
}
