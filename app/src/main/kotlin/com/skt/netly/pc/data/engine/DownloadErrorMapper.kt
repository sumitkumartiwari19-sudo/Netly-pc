package com.skt.netly.pc.data.engine

/** Turns yt-dlp's stderr from a failed download into a short message for the Downloads screen. */
object DownloadErrorMapper {
    fun map(stderr: String): FetchError {
        val lower = stderr.lowercase()
        val detail = stderr.lineSequence().map { it.trim() }.lastOrNull { it.isNotEmpty() }
        return when {
            "no space left" in lower || "not enough space" in lower ->
                FetchError(ErrorKind.Unknown, "Not enough free space on the disk.", detail)

            "permission denied" in lower || "access is denied" in lower ->
                FetchError(ErrorKind.Unknown, "Netly cannot write to the download folder.", detail)

            "ffmpeg not found" in lower || "ffprobe and ffmpeg not found" in lower || "ffmpeg is not installed" in lower ->
                FetchError(ErrorKind.EngineMissing, "FFmpeg is missing. Reinstall Netly.", detail)

            "requested format is not available" in lower ->
                FetchError(ErrorKind.Unavailable, "This quality is not available for this video. Pick another one.", detail)

            else -> {
                val mapped = ErrorMapper.map(stderr)
                if (mapped.kind == ErrorKind.Unknown) {
                    FetchError(ErrorKind.Unknown, "The download failed. Try again.", detail)
                } else {
                    mapped
                }
            }
        }
    }
}
