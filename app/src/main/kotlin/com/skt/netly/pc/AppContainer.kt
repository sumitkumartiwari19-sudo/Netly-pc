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
import com.skt.netly.pc.data.library.LibraryItem
import com.skt.netly.pc.data.library.LibraryRepository
import com.skt.netly.pc.domain.model.DownloadRequest
import com.skt.netly.pc.domain.model.SearchItem
import com.skt.netly.pc.domain.model.JobStatus
import com.skt.netly.pc.player.PlayItem
import com.skt.netly.pc.player.PlayerEngine
import com.skt.netly.pc.player.PlayerState
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
    /** Finished downloads. Kept separately from the Downloads list, so clearing that list never empties the Library. */
    val library = LibraryRepository(AppPaths.libraryFile()) { File(settings.state.value.downloadDir) }
    val manager = DownloadManager(
        downloader,
        DownloadStore(AppPaths.downloadsFile()),
        maxConcurrent = settings.state.value.maxConcurrent,
        onCompleted = { job -> library.addFromJob(job) },
    )
    val updater = EngineUpdater(engine)
    val home = HomeViewModel(engine, client, manager, settings)
    val player = PlayerEngine(
        engine,
        { options() },
        initial = settings.state.value.let {
            PlayerState(quality = it.playerQuality, audioOnly = it.playerAudioOnly, autoplay = it.autoplayNext)
        },
        onPrefsChanged = { q, a, p -> settings.update { it.copy(playerQuality = q, playerAudioOnly = a, autoplayNext = p) } },
    )

    /** True while the big player is shown instead of the current screen. */
    val playerOpen = MutableStateFlow(false)

    /** Screens other parts of the app ask to open (for example after a playlist link is pasted). */
    val requestScreen = MutableStateFlow<Screen?>(null)

    val search = SearchViewModel(
        search = YtDlpSearch(engine) { options() },
        manager = manager,
        settings = settings,
        onPlay = { items, index -> playSearch(items, index) },
        onOpenInHome = { url ->
            home.pasteAndFetch(url)
            requestScreen.value = Screen.Home
        },
    )

    fun playSearch(items: List<SearchItem>, index: Int) {
        val queue = items.filter { !it.isPlaylist }
        val picked = items.getOrNull(index) ?: return
        val start = queue.indexOfFirst { it.id == picked.id }.coerceAtLeast(0)
        player.playQueue(
            queue.map { PlayItem(it.id, it.title, it.channel, it.thumbnailUrl, it.durationSec ?: 0L, url = it.url) },
            start,
        )
        playerOpen.value = true
    }

    fun playLibrary(items: List<LibraryItem>, index: Int) {
        player.playQueue(
            items.map { PlayItem(it.path, it.title.ifBlank { File(it.path).nameWithoutExtension }, it.channel, it.thumbnailUrl, it.durationSec, filePath = it.path) },
            index,
        )
        playerOpen.value = true
    }

    /** Downloads the video that is playing now, using the player's current video or music mode. */
    fun downloadCurrent() {
        val s = player.state.value
        val item = s.item ?: return
        val url = item.url ?: return
        val cfg = settings.state.value
        manager.enqueue(
            DownloadRequest(
                videoId = item.key,
                url = url,
                title = item.title,
                channel = item.channel,
                thumbnailUrl = item.thumbnailUrl,
                durationSec = item.durationSec,
                audioOnly = s.audioOnly,
                height = if (s.audioOnly) null else (if (cfg.defaultHeight <= 0) 1080 else cfg.defaultHeight).coerceAtMost(1080),
                audioKey = "mp3-192",
                container = "MP4",
                embedThumbnail = true,
                embedSubtitles = false,
                outputDir = cfg.downloadDir,
            ),
            priority = true,
        )
    }

    /** A YouTube link found on the clipboard by the clipboard watcher, waiting for the user's decision. */
    val clipboardLink = MutableStateFlow<String?>(null)

    init {
        home.onPlaylist = { id ->
            search.openPlaylist(id)
            requestScreen.value = Screen.Search
        }
        scope.launch {
            // Downloads finished before the Library had its own list are added once; then new files in the folder are picked up.
            manager.jobs.value.filter { it.status == JobStatus.Completed }.forEach { library.addFromJob(it) }
            library.scan()
        }
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
        player.close()
        home.close()
        search.close()
        manager.close()
        scope.cancel()
    }
}
