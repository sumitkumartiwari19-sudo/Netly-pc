package com.skt.netly.pc.data.downloads

import com.skt.netly.pc.data.engine.DownloadOutcome
import com.skt.netly.pc.data.engine.ErrorKind
import com.skt.netly.pc.data.engine.FetchError
import com.skt.netly.pc.data.engine.MediaDownloader
import com.skt.netly.pc.data.engine.ParsedLine
import com.skt.netly.pc.domain.model.DownloadJob
import com.skt.netly.pc.domain.model.DownloadRequest
import com.skt.netly.pc.domain.model.JobStatus
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

class DownloadManagerTest {
    private fun request(id: String) = DownloadRequest(
        videoId = id, url = "https://www.youtube.com/watch?v=$id", title = id, channel = "c", thumbnailUrl = "",
        durationSec = 10, audioOnly = false, height = 720, audioKey = "mp3-320", container = "MP4",
        embedThumbnail = false, embedSubtitles = false, outputDir = "out",
    )

    private fun store() = DownloadStore(File(Files.createTempDirectory("netly-mgr").toFile(), "downloads.json"))

    private fun manager(store: DownloadStore, max: Int = 2, downloader: MediaDownloader) =
        DownloadManager(downloader, store, CoroutineScope(SupervisorJob() + Dispatchers.Default), max)

    private suspend fun DownloadManager.await(id: Long, status: JobStatus): DownloadJob =
        withTimeout(10_000) { jobs.first { list -> list.any { it.id == id && it.status == status } } }.first { it.id == id }

    @Test
    fun completesAJob() = runBlocking {
        val m = manager(store()) { _, onLine ->
            onLine(ParsedLine.Progress(50, 100, 10, 5))
            DownloadOutcome.Completed("out/a.mp4")
        }
        val id = m.enqueue(request("aaaaaaaaaaa"), priority = true)
        val job = m.await(id, JobStatus.Completed)
        assertEquals(1f, job.progress)
        assertEquals("out/a.mp4", job.filePath)
        m.close()
    }

    @Test
    fun marksFailure() = runBlocking {
        val m = manager(store()) { _, _ -> DownloadOutcome.Failed(FetchError(ErrorKind.Network, "No internet")) }
        val id = m.enqueue(request("bbbbbbbbbbb"), priority = false)
        assertEquals("No internet", m.await(id, JobStatus.Failed).errorMessage)
        m.close()
    }

    @Test
    fun pauseKeepsJobAndResumeFinishesIt() = runBlocking {
        val gate = CompletableDeferred<Unit>()
        var calls = 0
        val m = manager(store()) { _, _ ->
            calls++
            if (calls == 1) gate.await() // first run blocks until paused (cancelled)
            DownloadOutcome.Completed(null)
        }
        val id = m.enqueue(request("ccccccccccc"), priority = true)
        m.await(id, JobStatus.Downloading)
        m.pause(id)
        m.await(id, JobStatus.Paused)
        m.resume(id)
        m.await(id, JobStatus.Completed)
        assertEquals(2, calls)
        m.close()
    }

    @Test
    fun respectsConcurrencyLimit() = runBlocking {
        val gate = CompletableDeferred<Unit>()
        val m = manager(store(), max = 1) { _, _ ->
            gate.await()
            DownloadOutcome.Completed(null)
        }
        val a = m.enqueue(request("ddddddddddd"), priority = false)
        val b = m.enqueue(request("eeeeeeeeeee"), priority = false)
        m.await(a, JobStatus.Downloading)
        assertEquals(JobStatus.Queued, m.jobs.value.first { it.id == b }.status)
        gate.complete(Unit)
        m.await(b, JobStatus.Completed)
        m.close()
    }

    @Test
    fun ignoresDuplicateRequests() = runBlocking {
        val gate = CompletableDeferred<Unit>()
        val m = manager(store()) { _, _ ->
            gate.await()
            DownloadOutcome.Completed(null)
        }
        val a = m.enqueue(request("fffffffffff"), priority = true)
        val b = m.enqueue(request("fffffffffff"), priority = false)
        assertEquals(a, b)
        assertEquals(1, m.jobs.value.size)
        gate.complete(Unit)
        m.close()
    }

    @Test
    fun unfinishedJobsComeBackPausedAfterRestart() = runBlocking {
        val store = store()
        val gate = CompletableDeferred<Unit>()
        val first = manager(store) { _, _ ->
            gate.await()
            DownloadOutcome.Completed(null)
        }
        val id = first.enqueue(request("ggggggggggg"), priority = true)
        first.await(id, JobStatus.Downloading)

        // Simulate an app restart: a new manager reads the saved list.
        val second = manager(store) { _, _ -> DownloadOutcome.Completed(null) }
        val restored = second.jobs.value.single()
        assertEquals(JobStatus.Paused, restored.status)
        assertTrue(restored.id == id)
        gate.complete(Unit)
        first.close()
        second.close()
    }
}
