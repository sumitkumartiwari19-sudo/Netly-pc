package com.skt.netly.pc

import com.skt.netly.pc.data.downloads.DownloadManager
import com.skt.netly.pc.data.downloads.DownloadStore
import com.skt.netly.pc.data.engine.EngineOptions
import com.skt.netly.pc.data.engine.EngineRepository
import com.skt.netly.pc.data.engine.EngineStatus
import com.skt.netly.pc.data.engine.EngineUpdater
import com.skt.netly.pc.data.engine.YtDlpClient
import com.skt.netly.pc.data.engine.YtDlpDownloader
import com.skt.netly.pc.data.engine.YtDlpSearch
import com.skt.netly.pc.domain.model.PlayRequest
import com.skt.netly.pc.platform.FileActions
import com.skt.netly.pc.player.PlayerService
import com.skt.netly.pc.ui.search.SearchViewModel
import com.skt.netly.pc.ui.shell.Screen
import java.io.File
import kotlinx.coroutines.delay
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
    private val downloader = YtDlpDownloader(engine) { options() }
    val manager = DownloadManager(
        downloader,
        DownloadStore(AppPaths.downloadsFile()),
        maxConcurrent = settings.state.value.maxConcurrent,
    )
    val updater = EngineUpdater(engine)
    val home = HomeViewModel(engine, client, manager, settings)
    val player = PlayerService(engine, downloader) { options() }

    /** Screens other parts of the app ask to open (for example after a playlist link is pasted). */
    val requestScreen = MutableStateFlow<Screen?>(null)

    val search = SearchViewModel(
        search = YtDlpSearch(engine) { options() },
        manager = manager,
        settings = settings,
        onPlay = { player.open(it) },
        onOpenInHome = { url ->
            home.pasteAndFetch(url)
            requestScreen.value = Screen.Home
        },
    )

    /** Plays a downloaded file in the built-in player when it can (MP4/MP3/M4A); other formats open in the Windows player. */
    fun playFile(path: String, title: String) {
        val ext = File(path).extension.lowercase()
        if (ext in setOf("mp4", "m4v", "mp3", "m4a", "aac", "wav")) {
            player.open(PlayRequest(title = title, filePath = path))
        } else {
            FileActions.open(path)
        }
    }

    /** A YouTube link found on the clipboard by the clipboard watcher, waiting for the user's decision. */
    val clipboardLink = MutableStateFlow<String?>(null)

    init {
        home.onPlaylist = { id ->
            search.openPlaylist(id)
            requestScreen.value = Screen.Search
        }
        scope.launch { player.cleanCache() }
        scope.launch {
            settings.state.collect { manager.setMaxConcurrent(it.maxConcurrent) }
        }
        scope.launch {
            // HomeViewModel prepares the engine; once it is ready, refresh yt-dlp if the user allows it.
            engine.status.first { it is EngineStatus.Ready }
            delay(60_000) // do not compete with the user's first actions on slow PCs
            if (settings.state.value.autoUpdateEngine) updater.update()
        }
    }

    fun close() {
        home.close()
        search.close()
        manager.close()
        scope.cancel()
    }
}
