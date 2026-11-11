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
}
