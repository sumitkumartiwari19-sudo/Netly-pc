package com.skt.netly.pc

import java.io.File

object AppInfo {
    const val NAME = "Netly"

    /** The installed version, e.g. 1.0.0 ("dev" when run from an IDE). */
    val VERSION: String by lazy {
        runCatching {
            AppInfo::class.java.getResourceAsStream("/netly-version.txt")?.use { it.readBytes().toString(Charsets.UTF_8).trim() }
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: "dev"
    }

    const val REPO = "sumitkumartiwari19-sudo/Netly-pc"
    const val REPO_URL = "https://github.com/$REPO"
    const val ISSUES_URL = "$REPO_URL/issues/new"
    const val RELEASES_URL = "$REPO_URL/releases"
    const val PRIVACY_URL = "$REPO_URL/blob/main/PRIVACY.md"
    const val LICENSES_URL = "$REPO_URL/blob/main/THIRD_PARTY_NOTICES.md"
    const val SECURITY_URL = "$REPO_URL/blob/main/SECURITY.md"

    const val DEV_NAME = "Sumit Kumar Tiwari"
    const val DEV_ROLE = "Android and web developer · motion graphics editor"
    const val DEV_EMAIL = "mr.sumitkumartiwari19@gmail.com"
    const val DEV_GITHUB = "https://github.com/sumitkumartiwari19-sudo"
    const val DEV_INSTAGRAM = "https://www.instagram.com/sumitkumartiwari19"
    const val DEV_PORTFOLIO = "https://developer-sumit.netlify.app"

    /** Folder with bundled yt-dlp.exe and ffmpeg.exe (set by the Compose packaging). Null when run from an IDE without resources. */
    fun resourcesDir(): File? =
        System.getProperty("compose.application.resources.dir")?.let { File(it) }

    fun hasBundledTool(fileName: String): Boolean =
        resourcesDir()?.resolve(fileName)?.isFile == true
}
