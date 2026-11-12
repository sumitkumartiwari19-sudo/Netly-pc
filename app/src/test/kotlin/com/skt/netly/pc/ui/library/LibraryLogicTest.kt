package com.skt.netly.pc.ui.library

import com.skt.netly.pc.domain.model.DownloadJob
import com.skt.netly.pc.domain.model.DownloadRequest
import com.skt.netly.pc.domain.model.JobStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class LibraryLogicTest {
    private fun job(id: Long, title: String, audio: Boolean = false, status: JobStatus = JobStatus.Completed, at: Long = id) =
        DownloadJob(
            id = id, status = status, createdAt = at,
            request = DownloadRequest(
                videoId = "v$id", url = "u", title = title, channel = "Chan$id", thumbnailUrl = "", durationSec = 1,
                audioOnly = audio, height = 720, audioKey = "mp3-320", container = "MP4",
                embedThumbnail = false, embedSubtitles = false, outputDir = "out",
            ),
        )

    private val jobs = listOf(job(1, "Banana song", audio = true), job(2, "apple video"), job(3, "Cherry"), job(4, "Hidden", status = JobStatus.Failed))

    @Test
    fun onlyCompletedAndFiltered() {
        assertEquals(listOf(3L, 2L, 1L), LibraryLogic.items(jobs, "", LibraryFilter.All, LibrarySort.Newest).map { it.id })
        assertEquals(listOf(1L), LibraryLogic.items(jobs, "", LibraryFilter.Audio, LibrarySort.Newest).map { it.id })
        assertEquals(listOf(3L, 2L), LibraryLogic.items(jobs, "", LibraryFilter.Video, LibrarySort.Newest).map { it.id })
    }

    @Test
    fun searchAndSort() {
        assertEquals(listOf(2L), LibraryLogic.items(jobs, "APPLE", LibraryFilter.All, LibrarySort.Newest).map { it.id })
        assertEquals(listOf(2L, 1L, 3L), LibraryLogic.items(jobs, "", LibraryFilter.All, LibrarySort.Name).map { it.id })
        assertEquals(listOf(3L), LibraryLogic.items(jobs, "chan3", LibraryFilter.All, LibrarySort.Name).map { it.id })
    }
}
