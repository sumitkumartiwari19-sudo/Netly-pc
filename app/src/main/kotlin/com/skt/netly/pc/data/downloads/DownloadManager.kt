package com.skt.netly.pc.data.downloads

import com.skt.netly.pc.data.engine.DownloadOutcome
import com.skt.netly.pc.data.engine.ErrorKind
import com.skt.netly.pc.data.engine.FetchError
import com.skt.netly.pc.data.engine.MediaDownloader
import com.skt.netly.pc.data.engine.ParsedLine
import com.skt.netly.pc.domain.model.DownloadJob
import com.skt.netly.pc.domain.model.DownloadRequest
import com.skt.netly.pc.domain.model.JobStatus
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Owns the download queue. Jobs move Queued -> Downloading -> Merging -> Completed (or Failed).
 * Pause stops the yt-dlp process and keeps its partial file; Resume starts it again and yt-dlp continues from there.
 */
class DownloadManager(
    private val downloader: MediaDownloader,
    private val store: DownloadStore,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    private val maxConcurrent: Int = 2,
) {
    private enum class StopReason { Pause, Remove }

    private val _jobs = MutableStateFlow<List<DownloadJob>>(emptyList())
    val jobs: StateFlow<List<DownloadJob>> = _jobs.asStateFlow()

    private val lock = Any()
    private val running = ConcurrentHashMap<Long, Job>()
    private val stopReasons = ConcurrentHashMap<Long, StopReason>()
    private val lastProgressAt = ConcurrentHashMap<Long, Long>()
    private var nextId = 1L

    init {
        // Restart safety: anything that was unfinished when the app closed comes back Paused.
        val saved = store.load().map { job ->
            if (job.status == JobStatus.Completed || job.status == JobStatus.Failed) {
                job
            } else {
                job.copy(status = JobStatus.Paused, speedBytesPerSec = null, etaSec = null)
            }
        }
        nextId = (saved.maxOfOrNull { it.id } ?: 0L) + 1
        _jobs.value = saved
    }

    /** Adds a download. [priority] is true for "Download" (runs before queued items), false for "Add to queue". */
    fun enqueue(request: DownloadRequest, priority: Boolean): Long {
        val id: Long
        synchronized(lock) {
            val duplicate = _jobs.value.firstOrNull { it.request == request && !it.isFinished }
            if (duplicate != null) {
                if (priority && duplicate.status == JobStatus.Paused) resume(duplicate.id)
                return duplicate.id
            }
            id = nextId++
            _jobs.update { it + DownloadJob(id = id, request = request, priority = priority) }
        }
        persist()
        schedule()
        return id
    }

    fun pause(id: Long) {
        val job = find(id) ?: return
        when {
            running.containsKey(id) -> {
                stopReasons[id] = StopReason.Pause
                running[id]?.cancel()
            }
            job.status == JobStatus.Queued -> {
                update(id) { it.copy(status = JobStatus.Paused) }
                persist()
            }
        }
    }

    /** Resumes a paused download or retries a failed one. */
    fun resume(id: Long) {
        val job = find(id) ?: return
        if (job.status != JobStatus.Paused && job.status != JobStatus.Failed) return
        update(id) { it.copy(status = JobStatus.Queued, errorMessage = null, priority = true) }
        persist()
        schedule()
    }

    /** Stops the download if it is running and removes it from the list. Partial files are left in the folder. */
    fun remove(id: Long) {
        if (running.containsKey(id)) {
            stopReasons[id] = StopReason.Remove
            running[id]?.cancel()
        } else {
            _jobs.update { list -> list.filterNot { it.id == id } }
            persist()
        }
    }

    fun pauseAll() {
        _jobs.value.filter { it.status == JobStatus.Queued || it.isRunning }.forEach { pause(it.id) }
    }

    fun resumeAll() {
        _jobs.value.filter { it.status == JobStatus.Paused }.forEach { resume(it.id) }
    }

    fun clearCompleted() {
        _jobs.update { list -> list.filterNot { it.status == JobStatus.Completed } }
        persist()
    }

    fun close() {
        scope.cancel()
    }

    // ---- internals ----

    private fun find(id: Long): DownloadJob? = _jobs.value.firstOrNull { it.id == id }

    private fun update(id: Long, change: (DownloadJob) -> DownloadJob) {
        _jobs.update { list -> list.map { if (it.id == id) change(it) else it } }
    }

    private fun persist() {
        store.save(_jobs.value)
    }

    private fun schedule() {
        synchronized(lock) {
            val free = maxConcurrent - running.size
            if (free <= 0) return
            val next = _jobs.value
                .filter { it.status == JobStatus.Queued && !running.containsKey(it.id) }
                .sortedWith(compareByDescending<DownloadJob> { it.priority }.thenBy { it.id })
                .take(free)
            next.forEach { start(it) }
        }
    }

    private fun start(job: DownloadJob) {
        val id = job.id
        // Created lazily and registered first, so a Pause arriving right after the status change always finds it.
        val coroutine = scope.launch(start = CoroutineStart.LAZY) {
            val outcome: DownloadOutcome = try {
                downloader.download(job.request) { line -> onLine(id, line) }
            } catch (e: CancellationException) {
                withContext(NonCancellable) { onStopped(id) }
                throw e
            } catch (e: Exception) {
                DownloadOutcome.Failed(FetchError(ErrorKind.Unknown, "The download failed. Try again.", e.message))
            }
            onFinished(id, outcome)
        }
        running[id] = coroutine
        coroutine.invokeOnCompletion {
            running.remove(id)
            stopReasons.remove(id)
            lastProgressAt.remove(id)
            schedule()
        }
        update(id) { it.copy(status = JobStatus.Downloading, errorMessage = null, speedBytesPerSec = null, etaSec = null) }
        persist()
        coroutine.start()
    }

    private fun onLine(id: Long, line: ParsedLine) {
        when (line) {
            is ParsedLine.Progress -> {
                val now = System.currentTimeMillis()
                val last = lastProgressAt[id] ?: 0L
                if (now - last < 250) return
                lastProgressAt[id] = now
                update(id) { job ->
                    if (job.status == JobStatus.Merging) return@update job
                    val ratio = line.total?.let { (line.downloaded.toDouble() / it).toFloat().coerceIn(0f, 1f) }
                    job.copy(
                        status = JobStatus.Downloading,
                        downloadedBytes = line.downloaded,
                        totalBytes = line.total ?: job.totalBytes,
                        // Video and audio come as two streams; never let the bar move backwards.
                        progress = if (ratio != null) maxOf(job.progress, ratio) else job.progress,
                        speedBytesPerSec = line.speedBytesPerSec,
                        etaSec = line.etaSec,
                    )
                }
            }
            is ParsedLine.Finishing ->
                update(id) { it.copy(status = JobStatus.Merging, progress = 1f, speedBytesPerSec = null, etaSec = null) }
            is ParsedLine.FilePath -> update(id) { it.copy(filePath = line.path) }
            ParsedLine.Other -> Unit
        }
    }

    private fun onFinished(id: Long, outcome: DownloadOutcome) {
        when (outcome) {
            is DownloadOutcome.Completed -> update(id) {
                it.copy(
                    status = JobStatus.Completed,
                    progress = 1f,
                    speedBytesPerSec = null,
                    etaSec = null,
                    filePath = outcome.filePath ?: it.filePath,
                    errorMessage = null,
                )
            }
            is DownloadOutcome.Failed -> update(id) {
                it.copy(
                    status = JobStatus.Failed,
                    speedBytesPerSec = null,
                    etaSec = null,
                    errorMessage = outcome.error.message,
                )
            }
        }
        persist()
    }

    private fun onStopped(id: Long) {
        when (stopReasons[id]) {
            StopReason.Remove -> _jobs.update { list -> list.filterNot { it.id == id } }
            else -> update(id) { it.copy(status = JobStatus.Paused, speedBytesPerSec = null, etaSec = null) }
        }
        persist()
    }
}
