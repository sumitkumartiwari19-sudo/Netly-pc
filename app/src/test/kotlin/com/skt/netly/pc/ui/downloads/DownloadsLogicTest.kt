package com.skt.netly.pc.ui.downloads

import com.skt.netly.pc.domain.model.DownloadJob
import com.skt.netly.pc.domain.model.DownloadRequest
import com.skt.netly.pc.domain.model.JobStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class DownloadsLogicTest {
    private val req = DownloadRequest(
        videoId = "abcdefghijk", url = "u", title = "t", channel = "c", thumbnailUrl = "", durationSec = 10,
        audioOnly = false, height = 720, audioKey = "mp3-320", container = "MP4",
        embedThumbnail = false, embedSubtitles = false, outputDir = "out",
    )

    private fun job(id: Long, status: JobStatus, speed: Long? = null, progress: Float = 0f, eta: Long? = null) =
        DownloadJob(id = id, request = req, status = status, speedBytesPerSec = speed, progress = progress, etaSec = eta)

    private val all = listOf(
        job(1, JobStatus.Downloading, speed = 1000),
        job(2, JobStatus.Downloading, speed = 500),
        job(3, JobStatus.Queued),
        job(4, JobStatus.Paused),
        job(5, JobStatus.Completed),
        job(6, JobStatus.Failed),
    )

    @Test
    fun tabsFilterJobs() {
        assertEquals(6, DownloadsLogic.visible(all, DownloadTab.All).size)
        assertEquals(listOf(2L, 1L), DownloadsLogic.visible(all, DownloadTab.Active).map { it.id })
        assertEquals(setOf(3L, 4L), DownloadsLogic.visible(all, DownloadTab.Queued).map { it.id }.toSet())
        assertEquals(listOf(5L), DownloadsLogic.visible(all, DownloadTab.Completed).map { it.id })
        assertEquals(listOf(6L), DownloadsLogic.visible(all, DownloadTab.Failed).map { it.id })
    }

    @Test
    fun summaryAddsSpeedOfRunningJobs() {
        val s = DownloadsLogic.summary(all)
        assertEquals(1500L, s.totalSpeed)
        assertEquals(2, s.active)
        assertEquals(1, s.completed)
    }

    @Test
    fun statusTexts() {
        assertEquals("42% · 1 KB/s · 0:35 left", DownloadsLogic.statusText(job(1, JobStatus.Downloading, speed = 1000, progress = 0.42f, eta = 35)))
        assertEquals("Paused · 50%", DownloadsLogic.statusText(job(2, JobStatus.Paused, progress = 0.5f)))
        assertEquals("Waiting in the queue", DownloadsLogic.statusText(job(3, JobStatus.Queued)))
        assertEquals("The download failed.", DownloadsLogic.statusText(job(4, JobStatus.Failed)))
    }
}
