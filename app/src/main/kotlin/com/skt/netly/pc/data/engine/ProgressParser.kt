package com.skt.netly.pc.data.engine

/** One meaningful line printed by yt-dlp while downloading. */
sealed interface ParsedLine {
    data class Progress(
        val downloaded: Long,
        val total: Long?,
        val speedBytesPerSec: Long?,
        val etaSec: Long?,
    ) : ParsedLine

    /** A post-processing step (merging, converting, embedding) has started. */
    data class Finishing(val step: String) : ParsedLine

    data class FilePath(val path: String) : ParsedLine

    data object Other : ParsedLine
}

/**
 * Reads the lines produced by the templates in [DownloadCommandBuilder]:
 *   NETLY_DL|status|downloaded|total|totalEstimate|speed|eta|filename
 *   NETLY_PP|status|postprocessor
 *   NETLY_FILE|path
 */
object ProgressParser {
    const val DL_PREFIX = "NETLY_DL|"
    const val PP_PREFIX = "NETLY_PP|"
    const val FILE_PREFIX = "NETLY_FILE|"

    // Subtitle, thumbnail and metadata files are downloaded too; their progress must not move the main bar.
    private val sideFileExtensions = setOf("vtt", "srt", "ttml", "srv1", "srv2", "srv3", "json3", "jpg", "jpeg", "png", "webp", "json")

    fun parse(rawLine: String): ParsedLine {
        val line = rawLine.trim()
        return when {
            line.startsWith(DL_PREFIX) -> parseDownload(line)
            line.startsWith(PP_PREFIX) -> parsePostProcess(line)
            line.startsWith(FILE_PREFIX) -> {
                val path = line.removePrefix(FILE_PREFIX).trim()
                if (path.isEmpty() || path == "NA") ParsedLine.Other else ParsedLine.FilePath(path)
            }
            else -> ParsedLine.Other
        }
    }

    private fun parseDownload(line: String): ParsedLine {
        val parts = line.removePrefix(DL_PREFIX).split("|", limit = 7)
        if (parts.size < 6) return ParsedLine.Other
        val filename = parts.getOrNull(6).orEmpty()
        val ext = filename.substringAfterLast('.', "").lowercase()
        if (ext in sideFileExtensions) return ParsedLine.Other

        val downloaded = number(parts[1]) ?: return ParsedLine.Other
        val total = number(parts[2]) ?: number(parts[3])
        return ParsedLine.Progress(
            downloaded = downloaded,
            total = total?.takeIf { it > 0 },
            speedBytesPerSec = number(parts[4]),
            etaSec = number(parts[5]),
        )
    }

    private fun parsePostProcess(line: String): ParsedLine {
        val parts = line.removePrefix(PP_PREFIX).split("|")
        val status = parts.getOrNull(0).orEmpty()
        return if (status == "started") ParsedLine.Finishing(parts.getOrNull(1).orEmpty()) else ParsedLine.Other
    }

    /** yt-dlp prints "NA" for unknown values and may print integers as 1234.0. */
    private fun number(text: String): Long? = text.trim().toDoubleOrNull()?.takeIf { it >= 0 }?.toLong()
}
