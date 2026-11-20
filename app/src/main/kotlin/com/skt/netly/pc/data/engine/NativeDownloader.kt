package com.skt.netly.pc.data.engine

import com.skt.netly.pc.domain.model.DownloadRequest
import com.skt.netly.pc.player.StreamParser
import com.skt.netly.pc.player.StreamResolver
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext

/**
 * Backup download route used only when yt-dlp is refused ("confirm you're not a bot") on every try.
 * It asks YouTube's player API for the stream addresses and lets ffmpeg save them, so yt-dlp is not involved at all.
 */
class NativeDownloader(
    private val engine: EngineRepository,
    private val optionsProvider: () -> EngineOptions,
) {
    /** Returns null when this route cannot help either (the caller then reports the original error). */
    suspend fun download(request: DownloadRequest, onLine: (ParsedLine) -> Unit): DownloadOutcome? = withContext(Dispatchers.IO) {
        val ffmpeg = engine.ffmpegFile
        if (!ffmpeg.isFile) return@withContext null
        val id = StreamResolver.videoIdOf(request.url) ?: return@withContext null
        val proxy = optionsProvider().proxy.trim()
        val streams = runCatching {
            StreamParser.parseNative(Innertube.player(id, proxy), request.height ?: 4320, Innertube.VR_USER_AGENT)
        }.getOrNull() ?: return@withContext null
        val audioUrl = streams.audioUrl ?: return@withContext null
        val videoUrl = if (request.audioOnly) null else streams.videoUrl

        val dir = File(request.outputDir).apply { mkdirs() }
        val ext = if (request.audioOnly) audioExt(request.audioKey) else request.container.lowercase().let { if (it == "webm" || it == "mkv") it else "mp4" }
        val base = request.title.replace(Regex("""[\\/:*?"<>|\u0000-\u001f]"""), "_").trim().trimEnd('.').take(120).ifBlank { id }
        var final = File(dir, "$base.$ext")
        var n = 1
        while (final.exists()) final = File(dir, "$base ($n).$ext").also { n++ }
        val temp = File(dir, "${final.nameWithoutExtension}.temp.$ext")
        temp.delete()

        val total = (streams.durationSec.takeIf { it > 0 } ?: request.durationSec).coerceAtLeast(1L)
        val cmd = buildList {
            add(ffmpeg.absolutePath)
            addAll(listOf("-hide_banner", "-loglevel", "error", "-nostdin", "-y", "-nostats", "-progress", "pipe:1"))
            fun input(url: String) {
                addAll(listOf("-user_agent", Innertube.VR_USER_AGENT, "-reconnect", "1", "-reconnect_streamed", "1", "-reconnect_delay_max", "5"))
                if (proxy.startsWith("http")) addAll(listOf("-http_proxy", proxy))
                addAll(listOf("-i", url))
            }
            if (videoUrl != null) {
                input(videoUrl)
                input(audioUrl)
                addAll(listOf("-map", "0:v:0", "-map", "1:a:0", "-c:v", "copy"))
                addAll(if (ext == "webm") listOf("-c:a", "libopus", "-b:a", "160k") else listOf("-c:a", "aac", "-b:a", "192k"))
            } else {
                input(audioUrl)
                addAll(listOf("-vn"))
                addAll(
                    when (request.audioKey) {
                        "m4a" -> listOf("-c:a", "aac", "-b:a", "192k")
                        "opus" -> listOf("-c:a", "libopus", "-b:a", "160k")
                        "mp3-192" -> listOf("-c:a", "libmp3lame", "-b:a", "192k")
                        else -> listOf("-c:a", "libmp3lame", "-b:a", "320k")
                    },
                )
            }
            add(temp.absolutePath)
        }

        val process = try {
            ProcessBuilder(cmd).start()
        } catch (e: IOException) {
            return@withContext null
        }
        runCatching { process.outputStream.close() }
        val err = StringBuilder()
        val errThread = kotlin.concurrent.thread(isDaemon = true) {
            runCatching { process.errorStream.bufferedReader().use { r -> r.forEachLine { if (err.length < 4000) err.appendLine(it) } } }
        }
        try {
            runCatching {
                process.inputStream.bufferedReader().forEachLine { line ->
                    val us = when {
                        line.startsWith("out_time_us=") -> line.substringAfter('=').trim().toLongOrNull()
                        line.startsWith("out_time_ms=") -> line.substringAfter('=').trim().toLongOrNull()
                        else -> null
                    }
                    if (us != null && us >= 0) onLine(ParsedLine.Percent((us / 1_000_000f / total).coerceIn(0f, 0.99f)))
                }
            }
            val exit = runInterruptible { process.waitFor() }
            errThread.join(2000)
            if (exit == 0 && temp.isFile && temp.length() > 0) {
                temp.renameTo(final)
                onLine(ParsedLine.Percent(1f))
                onLine(ParsedLine.FilePath(final.absolutePath))
                DownloadOutcome.Completed(final.absolutePath)
            } else {
                temp.delete()
                null
            }
        } finally {
            if (process.isAlive) {
                runCatching { process.descendants().forEach { it.destroyForcibly() } }
                process.destroyForcibly()
                temp.delete()
            }
        }
    }

    private fun audioExt(key: String) = when (key) {
        "m4a" -> "m4a"
        "opus" -> "opus"
        else -> "mp3"
    }
}
