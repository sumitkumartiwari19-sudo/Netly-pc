package com.skt.netly.pc.data.engine

import com.skt.netly.pc.domain.model.DownloadRequest
import java.io.File
import java.io.IOException
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext

sealed interface DownloadOutcome {
    data class Completed(val filePath: String?) : DownloadOutcome
    data class Failed(val error: FetchError) : DownloadOutcome
}

fun interface MediaDownloader {
    /** Runs one download. Cancelling the calling coroutine stops the download and keeps partial files for resuming. */
    suspend fun download(request: DownloadRequest, onLine: (ParsedLine) -> Unit): DownloadOutcome
}

class YtDlpDownloader(
    private val engine: EngineRepository,
    private val optionsProvider: () -> EngineOptions = { EngineOptions() },
) : MediaDownloader {

    override suspend fun download(request: DownloadRequest, onLine: (ParsedLine) -> Unit): DownloadOutcome {
        val exe = try {
            engine.awaitReady()
        } catch (e: FetchException) {
            return DownloadOutcome.Failed(e.error)
        }
        val ffmpegDir = if (engine.ffmpegFile.isFile) engine.engineDir.absolutePath else null
        val command = DownloadCommandBuilder.build(exe.absolutePath, ffmpegDir, request, optionsProvider())
        return withContext(Dispatchers.IO) {
            File(request.outputDir).mkdirs()
            runProcess(command, onLine)
        }
    }

    private suspend fun runProcess(command: List<String>, onLine: (ParsedLine) -> Unit): DownloadOutcome =
        coroutineScope<DownloadOutcome> {
            val builder = ProcessBuilder(command)
            builder.environment()["PYTHONIOENCODING"] = "utf-8"
            val process = try {
                builder.start()
            } catch (e: IOException) {
                return@coroutineScope DownloadOutcome.Failed(
                    FetchError(ErrorKind.EngineMissing, "Netly's download engine could not start.", e.message),
                )
            }
            runCatching { process.outputStream.close() }

            val errText = StringBuilder()
            val errThread = thread(isDaemon = true) {
                try {
                    process.errorStream.bufferedReader(Charsets.UTF_8).use { reader ->
                        val buffer = CharArray(4096)
                        while (true) {
                            val read = reader.read(buffer)
                            if (read < 0) break
                            synchronized(errText) { if (errText.length < 200_000) errText.append(buffer, 0, read) }
                        }
                    }
                } catch (_: IOException) {
                    // Process ended or was stopped.
                }
            }

            val filePath = AtomicReference<String?>(null)
            val reader = launch(Dispatchers.IO) {
                try {
                    process.inputStream.bufferedReader(Charsets.UTF_8).forEachLine { line ->
                        val parsed = ProgressParser.parse(line)
                        if (parsed is ParsedLine.FilePath) filePath.set(parsed.path)
                        onLine(parsed)
                    }
                } catch (_: IOException) {
                    // Stream closed because the process was stopped.
                }
            }

            try {
                reader.join()
                val exit = runInterruptible { process.waitFor() }
                errThread.join(3000)
                if (exit == 0) {
                    DownloadOutcome.Completed(filePath.get())
                } else {
                    DownloadOutcome.Failed(DownloadErrorMapper.map(synchronized(errText) { errText.toString() }))
                }
            } finally {
                // Runs on cancel (pause or remove) too. Closing the process ends the reader's blocking read.
                if (process.isAlive) killTree(process)
            }
        }

    /** yt-dlp.exe starts child processes (its own runtime, ffmpeg); stop them all. */
    private fun killTree(process: Process) {
        runCatching { process.descendants().forEach { it.destroyForcibly() } }
        process.destroyForcibly()
    }
}
