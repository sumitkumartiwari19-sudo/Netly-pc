package com.skt.netly.pc.data.engine

import com.skt.netly.pc.data.settings.AppSettings

/** Settings that change how yt-dlp is started. */
data class EngineOptions(
    /** Maximum speed in KB/s; 0 means unlimited. */
    val speedLimitKb: Int = 0,
    /** none, chrome, edge, firefox or brave. */
    val cookiesBrowser: String = "none",
    val proxy: String = "",
    val template: String = DownloadCommandBuilder.OUTPUT_TEMPLATE,
) {
    fun networkArgs(includeRate: Boolean = true): List<String> = buildList {
        if (cookiesBrowser != "none" && cookiesBrowser.isNotBlank()) addAll(listOf("--cookies-from-browser", cookiesBrowser))
        if (proxy.isNotBlank()) addAll(listOf("--proxy", proxy.trim()))
        if (includeRate && speedLimitKb > 0) addAll(listOf("--limit-rate", "${speedLimitKb}K"))
    }

    companion object {
        fun templateFor(key: String): String = when (key) {
            "channel_title" -> "%(channel).60B - %(title).120B.%(ext)s"
            "title_id" -> "%(title).120B [%(id)s].%(ext)s"
            else -> DownloadCommandBuilder.OUTPUT_TEMPLATE
        }

        fun from(s: AppSettings) = EngineOptions(
            speedLimitKb = s.speedLimitKb,
            cookiesBrowser = s.cookiesBrowser,
            proxy = s.proxy,
            template = templateFor(s.nameTemplate),
        )
    }
}
