package com.skt.netly.pc.player

import com.skt.netly.pc.data.engine.EngineOptions
import com.skt.netly.pc.data.engine.EngineRepository
import com.skt.netly.pc.data.engine.DownloadOutcome
import com.skt.netly.pc.data.engine.MediaDownloader
import com.skt.netly.pc.data.engine.ProcessRunner
import com.skt.netly.pc.domain.model.DownloadRequest
import com.skt.netly.pc.domain.model.PlayRequest
import com.skt.netly.pc.platform.AppPaths
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.swing.SwingUtilities

/** Finds playable addresses for YouTube videos and opens the player window. */
class PlayerService(
    private val engine: EngineRepository,
    private val downloader: MediaDownloader,
    private val optionsProvider: () -> EngineOptions = { EngineOptions() },
) {
    private val cacheDir = File(AppPaths.dataDir(), "cache")
    private val hdFiles = ConcurrentHashMap<String, String>()

    fun open(request: PlayRequest) {
        SwingUtilities.invokeLater { PlayerFrame(this, request).show() }
    }

    /** Direct address of a 360p stream (video and sound together); null if YouTube gave none. */
    suspend fun resolveStream(videoUrl: String): String? {
        val exe = engine.awaitReady()
        val command = buildList {
            add(exe.absolutePath)
            addAll(listOf("--ignore-config", "--no-warnings", "--no-colors", "--no-playlist"))
            addAll(listOf("-g", "-f", "18/b[ext=mp4][vcodec^=avc1][acodec!=none]/b[ext=mp4]"))
            addAll(optionsProvider().networkArgs(includeRate = false))
            add("--")
            add(videoUrl)
        }
        val result = ProcessRunner.run(command, 60_000)
        return result.stdout.lineSequence().map { it.trim() }.firstOrNull { it.startsWith("http") }
    }

    /** Downloads an H.264 copy at [height] into the cache (YouTube has no single-file HD streams) and returns its path. */
    suspend fun prepareHd(request: PlayRequest, height: Int, onPercent: (Int) -> Unit): String? {
        val key = "${request.videoId ?: request.videoUrl}-$height"
        hdFiles[key]?.let { if (File(it).isFile) return it }
        val url = request.videoUrl ?: return null
        cacheDir.mkdirs()
        val download = DownloadRequest(
            videoId = request.videoId ?: "play", url = url, title = request.title, channel = "", thumbnailUrl = "",
            durationSec = 0, audioOnly = false, height = height, audioKey = "mp3-192", container = "MP4",
            embedThumbnail = false, embedSubtitles = false, outputDir = cacheDir.absolutePath,
        )
        val outcome = downloader.download(download) { line ->
            if (line is com.skt.netly.pc.data.engine.ParsedLine.Progress && line.total != null && line.total > 0) {
                onPercent(((line.downloaded * 100) / line.total).toInt().coerceIn(0, 100))
            }
        }
        val path = (outcome as? DownloadOutcome.Completed)?.filePath ?: return null
        hdFiles[key] = path
        return path
    }

    /** Removes old preview files so the cache does not fill the disk. */
    fun cleanCache() {
        val limit = System.currentTimeMillis() - 24L * 60 * 60 * 1000
        cacheDir.listFiles()?.forEach { if (it.isFile && it.lastModified() < limit) it.delete() }
    }
}
