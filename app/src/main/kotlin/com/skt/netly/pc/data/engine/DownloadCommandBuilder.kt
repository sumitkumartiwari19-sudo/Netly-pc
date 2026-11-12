package com.skt.netly.pc.data.engine

import com.skt.netly.pc.domain.model.DownloadRequest

/** Builds the yt-dlp argument list for one download. Arguments are passed as a list, never as a shell string. */
object DownloadCommandBuilder {
    const val OUTPUT_TEMPLATE = "%(title).150B.%(ext)s"

    fun build(
        exe: String,
        ffmpegDir: String?,
        request: DownloadRequest,
        options: EngineOptions = EngineOptions(),
    ): List<String> = buildList {
        add(exe)
        addAll(listOf("--ignore-config", "--no-playlist", "--no-warnings", "--no-colors"))
        addAll(listOf("--newline", "--progress", "--no-mtime", "--windows-filenames"))
        addAll(listOf("--retries", "10", "--fragment-retries", "10", "--concurrent-fragments", "4"))
        if (ffmpegDir != null) {
            add("--ffmpeg-location")
            add(ffmpegDir)
        }
        addAll(options.networkArgs())
        add("--progress-template")
        add("download:${ProgressParser.DL_PREFIX}%(progress.status)s|%(progress.downloaded_bytes)s|%(progress.total_bytes)s|" +
            "%(progress.total_bytes_estimate)s|%(progress.speed)s|%(progress.eta)s|%(progress.filename)s")
        add("--progress-template")
        add("postprocess:${ProgressParser.PP_PREFIX}%(progress.status)s|%(progress.postprocessor)s")
        addAll(listOf("--no-simulate", "--print", "after_move:${ProgressParser.FILE_PREFIX}%(filepath)s"))
        addAll(listOf("-P", request.outputDir, "-o", options.template))

        if (request.audioOnly) addAudio(request) else addVideo(request)

        add("--")
        add(request.url)
    }

    private fun MutableList<String>.addVideo(request: DownloadRequest) {
        val container = request.container.lowercase()
        val h = request.height?.let { "[height<=$it]" }.orEmpty()
        val format = when (container) {
            // H.264 (avc1) plays on every Windows PC. AV1 needs a paid codec extension on many laptops, so it is avoided here.
            "mp4" -> "bv*[vcodec^=avc1]$h+ba[ext=m4a]/b[vcodec^=avc1]$h/bv*$h[vcodec!^=av01]+ba/b$h[vcodec!^=av01]/bv*$h+ba/b$h"
            "webm" -> "bv*$h[ext=webm]+ba[ext=webm]/b$h[ext=webm]"
            else -> "bv*$h+ba/b$h"
        }
        addAll(listOf("-f", format, "--merge-output-format", container))
        if (request.embedSubtitles) addAll(listOf("--embed-subs", "--sub-langs", "en.*,hi.*"))
        if (request.embedThumbnail && container != "webm") addAll(listOf("--embed-thumbnail", "--embed-metadata"))
    }

    private fun MutableList<String>.addAudio(request: DownloadRequest) {
        when (request.audioKey) {
            "mp3-192" -> addAll(listOf("-f", "ba/b", "-x", "--audio-format", "mp3", "--audio-quality", "192K"))
            "m4a" -> addAll(listOf("-f", "ba[ext=m4a]/ba/b", "-x", "--audio-format", "m4a"))
            "opus" -> addAll(listOf("-f", "ba[ext=webm]/ba/b", "-x", "--audio-format", "opus"))
            else -> addAll(listOf("-f", "ba/b", "-x", "--audio-format", "mp3", "--audio-quality", "320K"))
        }
        if (request.embedThumbnail) addAll(listOf("--embed-thumbnail", "--embed-metadata"))
    }
}
