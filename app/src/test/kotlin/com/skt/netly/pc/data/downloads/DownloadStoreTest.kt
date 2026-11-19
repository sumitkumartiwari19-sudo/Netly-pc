package com.skt.netly.pc.data.downloads

import com.skt.netly.pc.domain.model.DownloadJob
import com.skt.netly.pc.domain.model.DownloadRequest
import com.skt.netly.pc.domain.model.JobStatus
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DownloadStoreTest {
    private fun tempFile(): File = File(Files.createTempDirectory("netly-store").toFile(), "downloads.json")

    private val request = DownloadRequest(
        videoId = "abcdefghijk", url = "https://www.youtube.com/watch?v=abcdefghijk", title = "Title \"quoted\" ✓",
        channel = "Chan", thumbnailUrl = "https://i.ytimg.com/vi/abcdefghijk/hqdefault.jpg", durationSec = 212,
        audioOnly = false, height = 720, audioKey = "mp3-320", container = "MKV",
        embedThumbnail = false, embedSubtitles = true, outputDir = "C:\\Users\\a\\Downloads\\Netly",
    )

    @Test
    fun roundTripsJobs() {
        val file = tempFile()
        val store = DownloadStore(file)
        val job = DownloadJob(
            id = 7, request = request, status = JobStatus.Failed, progress = 0.42f,
            downloadedBytes = 42, totalBytes = 100, filePath = "C:\\x\\a.mp4", errorMessage = "Nope", createdAt = 123L,
        )
        store.save(listOf(job, job.copy(id = 8, request = request.copy(audioOnly = true, height = null))))
        val loaded = DownloadStore(file).load()
        assertEquals(2, loaded.size)
        assertEquals(job, loaded[0])
        assertEquals(null, loaded[1].request.height)
        assertTrue(loaded[1].request.audioOnly)
    }

    @Test
    fun missingOrBrokenFileLoadsEmpty() {
        val file = tempFile()
        assertEquals(emptyList(), DownloadStore(file).load())
        file.writeText("{ not json")
        assertEquals(emptyList(), DownloadStore(file).load())
    }
}
