package com.skt.netly.pc.platform

import java.io.File

object AppPaths {
    val isWindows: Boolean = System.getProperty("os.name").orEmpty().lowercase().contains("windows")
    val exeSuffix: String = if (isWindows) ".exe" else ""

    /** %LOCALAPPDATA%\Netly on Windows; ~/.netly elsewhere (development only). */
    fun dataDir(): File {
        val localAppData = System.getenv("LOCALAPPDATA")?.takeIf { it.isNotBlank() }
        return if (isWindows && localAppData != null) {
            File(localAppData, "Netly")
        } else {
            File(System.getProperty("user.home") ?: ".", ".netly")
        }
    }

    fun engineDir(): File = File(dataDir(), "engine")

    fun settingsFile(): File = File(dataDir(), "settings.json")

    fun downloadsFile(): File = File(dataDir(), "downloads.json")

    /** Where finished files go until the Settings screen (M4) lets the user change it: Downloads\Netly. */
    fun defaultDownloadDir(): File = File(File(System.getProperty("user.home") ?: ".", "Downloads"), "Netly")
}
