package com.skt.netly.pc.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToInt

object Formatters {
    private const val KIB = 1024L
    private const val MIB = 1024L * 1024L
    private const val GIB = 1024L * 1024L * 1024L

    fun duration(totalSeconds: Long): String {
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        val s = totalSeconds % 60
        return if (h > 0) "%d:%02d:%02d".format(Locale.US, h, m, s) else "%d:%02d".format(Locale.US, m, s)
    }

    fun bytes(value: Long?): String = when {
        value == null || value <= 0L -> "—"
        value >= GIB -> "%.1f GB".format(Locale.US, value / GIB.toDouble())
        value >= MIB -> "${(value / MIB.toDouble()).roundToInt()} MB"
        else -> "${maxOf(1L, value / KIB)} KB"
    }

    /** Same as [bytes] with a "~" prefix, for sizes calculated from bitrate rather than reported by YouTube. */
    fun approxBytes(value: Long?): String = bytes(value).let { if (it == "—") it else "~$it" }

    fun views(count: Long?): String {
        if (count == null) return ""
        val (scaled, suffix) = when {
            count >= 1_000_000_000L -> count / 1e9 to "B"
            count >= 1_000_000L -> count / 1e6 to "M"
            count >= 1_000L -> count / 1e3 to "K"
            else -> return "$count views"
        }
        val text = "%.1f".format(Locale.US, scaled).removeSuffix(".0")
        return "$text$suffix views"
    }

    /** uploadDate is yt-dlp's yyyyMMdd. Returns "" when unknown. */
    fun age(uploadDate: String?, today: LocalDate = LocalDate.now()): String {
        if (uploadDate == null) return ""
        val date = runCatching { LocalDate.parse(uploadDate, DateTimeFormatter.BASIC_ISO_DATE) }.getOrNull() ?: return ""
        val days = ChronoUnit.DAYS.between(date, today)
        return when {
            days < 1 -> "today"
            days == 1L -> "yesterday"
            days < 30 -> "$days days ago"
            days < 365 -> (days / 30).let { if (it == 1L) "1 month ago" else "$it months ago" }
            else -> (days / 365).let { if (it == 1L) "1 year ago" else "$it years ago" }
        }
    }
}
