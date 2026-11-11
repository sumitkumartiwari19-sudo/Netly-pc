package com.skt.netly.pc.data.engine

import com.skt.netly.pc.AppInfo
import com.skt.netly.pc.platform.AppPaths
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

sealed interface EngineStatus {
    data object Preparing : EngineStatus
    data class Ready(val version: String) : EngineStatus
    data class Failed(val message: String) : EngineStatus
}

/**
 * Owns the working copy of yt-dlp and ffmpeg in %LOCALAPPDATA%\Netly\engine.
 * The installer folder is read-only, so bundled files are copied once and the copy is what runs (and what updates replace later).
 */
class EngineRepository(
    val engineDir: File = AppPaths.engineDir(),
    private val bundledDir: File? = AppInfo.resourcesDir(),
) {
    val ytDlpFile: File get() = File(engineDir, "yt-dlp" + AppPaths.exeSuffix)
    val ffmpegFile: File get() = File(engineDir, "ffmpeg" + AppPaths.exeSuffix)

    private val _status = MutableStateFlow<EngineStatus>(EngineStatus.Preparing)
    val status: StateFlow<EngineStatus> = _status.asStateFlow()

    suspend fun prepare() {
        _status.value = EngineStatus.Preparing
        _status.value = withContext(Dispatchers.IO) {
            try {
                engineDir.mkdirs()
                copyIfMissing("yt-dlp" + AppPaths.exeSuffix)
                copyIfMissing("ffmpeg" + AppPaths.exeSuffix)
                if (!ytDlpFile.isFile) {
                    EngineStatus.Failed("yt-dlp was not found. Reinstall Netly.")
                } else {
                    val result = ProcessRunner.run(listOf(ytDlpFile.absolutePath, "--version"), 30_000)
                    if (result.exitCode == 0) {
                        EngineStatus.Ready(result.stdout.trim())
                    } else {
                        EngineStatus.Failed("yt-dlp could not start.")
                    }
                }
            } catch (e: ProcessTimeoutException) {
                EngineStatus.Failed("yt-dlp did not respond.")
            } catch (e: java.io.IOException) {
                EngineStatus.Failed("Could not prepare the download engine: ${e.message}")
            }
        }
    }

    /** Waits until preparation finished; throws if the engine is unusable. */
    suspend fun awaitReady(): File {
        val finished = status.first { it !is EngineStatus.Preparing }
        if (finished is EngineStatus.Failed) {
            throw FetchException(FetchError(ErrorKind.EngineMissing, finished.message))
        }
        return ytDlpFile
    }

    private fun copyIfMissing(name: String) {
        val target = File(engineDir, name)
        if (target.isFile) return
        val source = bundledDir?.resolve(name) ?: return
        if (!source.isFile) return
        Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        target.setExecutable(true)
    }
}
