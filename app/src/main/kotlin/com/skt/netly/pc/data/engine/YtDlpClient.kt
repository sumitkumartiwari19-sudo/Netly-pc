package com.skt.netly.pc.data.engine

import com.skt.netly.pc.domain.YoutubeLink
import com.skt.netly.pc.domain.model.MediaInfo
import java.io.IOException

/** Fetches video details by running the bundled yt-dlp. All failures are reported as FetchException. */
class YtDlpClient(private val engine: EngineRepository) {

    suspend fun fetch(link: YoutubeLink.Video): MediaInfo {
        val exe = engine.awaitReady()

        val command = buildList {
            add(exe.absolutePath)
            addAll(listOf("-J", "--no-playlist", "--no-warnings", "--no-colors", "--ignore-config"))
            if (engine.ffmpegFile.isFile) {
                add("--ffmpeg-location")
                add(engine.engineDir.absolutePath)
            }
            add("--")
            add(link.canonicalUrl)
        }

        val result = try {
            ProcessRunner.run(command, timeoutMs = 60_000)
        } catch (e: ProcessTimeoutException) {
            throw FetchException(
                FetchError(ErrorKind.Timeout, "YouTube took too long to respond. Check your connection and try again."),
            )
        } catch (e: IOException) {
            throw FetchException(
                FetchError(ErrorKind.EngineMissing, "Netly's download engine could not start.", e.message),
            )
        }

        if (result.exitCode != 0 || result.stdout.isBlank()) {
            throw FetchException(ErrorMapper.map(result.stderr))
        }

        val info = try {
            MediaInfoParser.parse(result.stdout)
        } catch (e: Exception) {
            throw FetchException(FetchError(ErrorKind.Unknown, "Could not read the video details.", e.message))
        }

        if (info.isLive) {
            throw FetchException(FetchError(ErrorKind.Unavailable, "Live streams cannot be downloaded yet."))
        }
        return info
    }
}
