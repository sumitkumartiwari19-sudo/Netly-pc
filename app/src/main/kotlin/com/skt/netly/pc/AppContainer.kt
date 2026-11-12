package com.skt.netly.pc

import com.skt.netly.pc.data.downloads.DownloadManager
import com.skt.netly.pc.data.downloads.DownloadStore
import com.skt.netly.pc.data.engine.EngineOptions
import com.skt.netly.pc.data.engine.EngineRepository
import com.skt.netly.pc.data.engine.EngineStatus
import com.skt.netly.pc.data.engine.EngineUpdater
import com.skt.netly.pc.data.engine.YtDlpClient
import com.skt.netly.pc.data.engine.YtDlpDownloader
import com.skt.netly.pc.data.settings.SettingsRepository
import com.skt.netly.pc.platform.AppPaths
import com.skt.netly.pc.ui.home.HomeViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Creates the app's long-lived objects once, so the window, tray and screens share them. */
class AppContainer {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val settings = SettingsRepository(AppPaths.settingsFile())
    val engine = EngineRepository()

    private fun options() = EngineOptions.from(settings.state.value)

    val client = YtDlpClient(engine) { options() }
    val manager = DownloadManager(
        YtDlpDownloader(engine) { options() },
        DownloadStore(AppPaths.downloadsFile()),
        maxConcurrent = settings.state.value.maxConcurrent,
    )
    val updater = EngineUpdater(engine)
    val home = HomeViewModel(engine, client, manager, settings)

    /** A YouTube link found on the clipboard by the clipboard watcher, waiting for the user's decision. */
    val clipboardLink = MutableStateFlow<String?>(null)

    init {
        scope.launch {
            settings.state.collect { manager.setMaxConcurrent(it.maxConcurrent) }
        }
        scope.launch {
            // HomeViewModel prepares the engine; once it is ready, refresh yt-dlp if the user allows it.
            engine.status.first { it is EngineStatus.Ready }
            if (settings.state.value.autoUpdateEngine) updater.update()
        }
    }

    fun close() {
        home.close()
        manager.close()
        scope.cancel()
    }
}
