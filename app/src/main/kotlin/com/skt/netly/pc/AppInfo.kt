package com.skt.netly.pc

import java.io.File

object AppInfo {
    const val NAME = "Netly"

    /** Folder with bundled yt-dlp.exe and ffmpeg.exe (set by the Compose packaging). Null when run from an IDE without resources. */
    fun resourcesDir(): File? =
        System.getProperty("compose.application.resources.dir")?.let { File(it) }

    fun hasBundledTool(fileName: String): Boolean =
        resourcesDir()?.resolve(fileName)?.isFile == true
}
