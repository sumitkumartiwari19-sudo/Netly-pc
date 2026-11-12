package com.skt.netly.pc.ui.search

import com.skt.netly.pc.data.downloads.DownloadManager
import com.skt.netly.pc.data.engine.FetchException
import com.skt.netly.pc.data.engine.YtDlpSearch
import com.skt.netly.pc.data.settings.SettingsRepository
import com.skt.netly.pc.domain.YoutubeLink
import com.skt.netly.pc.domain.YoutubeUrl
import com.skt.netly.pc.domain.model.DownloadRequest
import com.skt.netly.pc.domain.model.PlayRequest
import com.skt.netly.pc.domain.model.SearchItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlaylistState(
    val id: String,
    val title: String = "",
    val loading: Boolean = true,
    val error: String? = null,
    val items: List<SearchItem> = emptyList(),
    val selected: Set<String> = emptySet(),
    val audio: Boolean = false,
    val height: Int = 720,
    val audioKey: String = "mp3-192",
)

data class SearchUiState(
    val query: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val searched: Boolean = false,
    val videos: List<SearchItem> = emptyList(),
    val playlists: List<SearchItem> = emptyList(),
    val playlist: PlaylistState? = null,
    val notice: String? = null,
)

class SearchViewModel(
    private val search: YtDlpSearch,
    private val manager: DownloadManager,
    private val settings: SettingsRepository,
    private val onPlay: (PlayRequest) -> Unit,
    private val onOpenInHome: (String) -> Unit,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()
    private var job: Job? = null

    fun onQuery(value: String) = _state.update { it.copy(query = value) }

    /** Runs a search; a pasted video link goes to Home and a playlist link opens the playlist. */
    fun submit() {
        val text = _state.value.query.trim()
        if (text.isEmpty()) return
        when (val link = YoutubeUrl.parse(text)) {
            is YoutubeLink.Video -> {
                if (YoutubeUrl.playlistId(text) != null && text.contains("list=") && !text.contains("watch?v=")) {
                    openPlaylist(YoutubeUrl.playlistId(text)!!)
                } else {
                    onOpenInHome(link.canonicalUrl)
                }
                return
            }
            YoutubeLink.PlaylistOnly -> {
                YoutubeUrl.playlistId(text)?.let { openPlaylist(it) }
                return
            }
            YoutubeLink.Invalid -> Unit
        }
        job?.cancel()
        _state.update { it.copy(loading = true, error = null, searched = true, playlist = null, notice = null) }
        job = scope.launch {
            try {
                val results = search.search(text)
                _state.update { it.copy(loading = false, videos = results.videos, playlists = results.playlists) }
            } catch (e: FetchException) {
                _state.update { it.copy(loading = false, error = e.error.message, videos = emptyList(), playlists = emptyList()) }
            }
        }
    }

    fun openPlaylist(id: String) {
        job?.cancel()
        _state.update { it.copy(playlist = PlaylistState(id = id), error = null, notice = null) }
        job = scope.launch {
            try {
                val result = search.playlist(id)
                val items = result.items.filter { !it.isPlaylist }
                _state.update {
                    it.copy(playlist = it.playlist?.copy(loading = false, title = result.title.orEmpty(), items = items, selected = items.map { v -> v.id }.toSet()))
                }
            } catch (e: FetchException) {
                _state.update { it.copy(playlist = it.playlist?.copy(loading = false, error = e.error.message)) }
            }
        }
    }

    fun closePlaylist() = _state.update { it.copy(playlist = null, notice = null) }

    private fun editPlaylist(change: (PlaylistState) -> PlaylistState) =
        _state.update { s -> s.copy(playlist = s.playlist?.let(change)) }

    fun toggle(id: String) = editPlaylist { p -> p.copy(selected = if (id in p.selected) p.selected - id else p.selected + id) }
    fun selectAll() = editPlaylist { p -> p.copy(selected = p.items.map { it.id }.toSet()) }
    fun selectNone() = editPlaylist { p -> p.copy(selected = emptySet()) }
    fun setPlaylistAudio(audio: Boolean) = editPlaylist { it.copy(audio = audio) }
    fun setPlaylistHeight(height: Int) = editPlaylist { it.copy(height = height) }
    fun setPlaylistAudioKey(key: String) = editPlaylist { it.copy(audioKey = key) }

    fun downloadSelected() {
        val p = _state.value.playlist ?: return
        val chosen = p.items.filter { it.id in p.selected }
        if (chosen.isEmpty()) {
            _state.update { it.copy(notice = "Nothing selected") }
            return
        }
        chosen.forEach { manager.enqueue(requestFor(it, p.audio, p.height, p.audioKey), priority = false) }
        _state.update { it.copy(notice = "Added ${chosen.size} to the queue. See the Downloads tab.") }
    }

    fun quickDownload(item: SearchItem, audio: Boolean) {
        val height = settings.state.value.defaultHeight.let { if (it <= 0) 1080 else it }
        manager.enqueue(requestFor(item, audio, height, "mp3-192"), priority = true)
        _state.update { it.copy(notice = "Download started. Follow it in the Downloads tab.") }
    }

    fun play(item: SearchItem) {
        onPlay(PlayRequest(title = item.title, videoUrl = item.url, videoId = item.id))
    }

    fun openInHome(item: SearchItem) = onOpenInHome(item.url)

    fun close() = scope.cancel()

    private fun requestFor(item: SearchItem, audio: Boolean, height: Int, audioKey: String) = DownloadRequest(
        videoId = item.id,
        url = item.url,
        title = item.title,
        channel = item.channel,
        thumbnailUrl = item.thumbnailUrl,
        durationSec = item.durationSec ?: 0L,
        audioOnly = audio,
        // MP4 is kept at 1080p or lower so it plays on every PC (see DownloadCommandBuilder).
        height = if (audio) null else height.coerceAtMost(1080),
        audioKey = audioKey,
        container = "MP4",
        embedThumbnail = true,
        embedSubtitles = false,
        outputDir = settings.state.value.downloadDir,
    )
}
