package com.skt.netly.pc.data.engine

import com.skt.netly.pc.data.settings.AppSettings

/** Settings that change how yt-dlp is started. */
data class EngineOptions(
    /** Maximum speed in KB/s; 0 means unlimited. */
    val speedLimitKb: Int = 0,
    /** none, chrome, edge, firefox or brave. */
    val cookiesBrowser: String = "none",
    /** Optional cookies.txt exported from a browser; used before [cookiesBrowser]. */
    val cookiesFile: String = "",
    /** YouTube client names for yt-dlp (see BotGuard); empty means yt-dlp's own choice. */
    val clients: String = "",
    val proxy: String = "",
    val template: String = DownloadCommandBuilder.OUTPUT_TEMPLATE,
) {
    fun networkArgs(includeRate: Boolean = true): List<String> = buildList {
        if (cookiesFile.isNotBlank() && java.io.File(cookiesFile).isFile) {
            addAll(listOf("--cookies", cookiesFile))
        } else if (cookiesBrowser in BotGuard.BROWSERS) {
            addAll(listOf("--cookies-from-browser", cookiesBrowser))
        }
        if (clients.isNotBlank()) addAll(listOf("--extractor-args", "youtube:player_client=$clients"))
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
            cookiesFile = s.cookiesFile,
            proxy = s.proxy,
            template = templateFor(s.nameTemplate),
        )
    }
}
