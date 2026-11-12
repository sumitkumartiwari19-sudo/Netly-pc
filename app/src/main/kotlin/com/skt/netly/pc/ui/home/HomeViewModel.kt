package com.skt.netly.pc.ui.home

import com.skt.netly.pc.data.downloads.DownloadManager
import com.skt.netly.pc.data.engine.EngineRepository
import com.skt.netly.pc.data.engine.ErrorKind
import com.skt.netly.pc.data.engine.FetchError
import com.skt.netly.pc.data.engine.FetchException
import com.skt.netly.pc.data.engine.YtDlpClient
import com.skt.netly.pc.domain.YoutubeLink
import com.skt.netly.pc.domain.YoutubeUrl
import com.skt.netly.pc.domain.model.DownloadRequest
import com.skt.netly.pc.domain.model.MediaInfo
import com.skt.netly.pc.platform.AppPaths
import com.skt.netly.pc.util.Formatters
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

enum class DownloadMode { Video, Audio }

enum class FetchPhase { Empty, Loading, Ready, Error }

data class HomeUiState(
    val input: String = "",
    val phase: FetchPhase = FetchPhase.Empty,
    val info: MediaInfo? = null,
    val error: FetchError? = null,
    val mode: DownloadMode = DownloadMode.Video,
    val selectedHeight: Int? = null,
    val selectedAudio: String = "mp3-320",
    val container: String = "MP4",
    val embedSubtitles: Boolean = true,
    val embedThumbnail: Boolean = true,
    /** Short confirmation shown under the buttons after a download is added. */
    val notice: String? = null,
) {
    /** Size text for the current choice, e.g. "182 MB" or "~29 MB"; "—" when unknown. */
    fun selectedSizeText(): String {
        val media = info ?: return "—"
        return when (mode) {
            DownloadMode.Video ->
                Formatters.bytes(media.videoQualities.firstOrNull { it.height == selectedHeight }?.estimatedBytes)
            DownloadMode.Audio ->
                Formatters.approxBytes(media.audioFormats.firstOrNull { it.key == selectedAudio }?.estimatedBytes)
        }
    }
}

class HomeViewModel(
    private val engine: EngineRepository,
    private val client: YtDlpClient,
    private val manager: DownloadManager,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()
    val engineStatus = engine.status

    private var fetchJob: Job? = null

    init {
        scope.launch { engine.prepare() }
    }

    fun onInputChange(value: String) = _state.update { it.copy(input = value) }

    fun pasteAndFetch(text: String) {
        _state.update { it.copy(input = text.trim()) }
        fetch()
    }

    fun fetch() {
        when (val link = YoutubeUrl.parse(_state.value.input)) {
            YoutubeLink.Invalid -> showError(
                FetchError(ErrorKind.InvalidUrl, "That does not look like a YouTube link. Paste a video, Shorts or youtu.be link."),
            )
            YoutubeLink.PlaylistOnly -> showError(
                FetchError(ErrorKind.PlaylistNotSupported, "Playlist links are not supported yet. Paste a single video link."),
            )
            is YoutubeLink.Video -> {
                fetchJob?.cancel()
                _state.update { it.copy(phase = FetchPhase.Loading, error = null, notice = null) }
                fetchJob = scope.launch {
                    try {
                        val info = client.fetch(link)
                        _state.update {
                            it.copy(
                                phase = FetchPhase.Ready,
                                info = info,
                                error = null,
                                selectedHeight = defaultHeight(info),
                            )
                        }
                    } catch (e: FetchException) {
                        showError(e.error)
                    }
                }
            }
        }
    }

    fun selectMode(mode: DownloadMode) = _state.update { it.copy(mode = mode) }
    fun selectHeight(height: Int) = _state.update { it.copy(selectedHeight = height) }
    fun selectAudio(key: String) = _state.update { it.copy(selectedAudio = key) }
    fun selectContainer(label: String) = _state.update { it.copy(container = label) }
    fun setEmbedSubtitles(value: Boolean) = _state.update { it.copy(embedSubtitles = value) }
    fun setEmbedThumbnail(value: Boolean) = _state.update { it.copy(embedThumbnail = value) }

    /** "Download" starts as soon as a slot is free; "Add to queue" waits behind other items. */
    fun download(startNow: Boolean) {
        val s = _state.value
        val info = s.info ?: return
        val audio = s.mode == DownloadMode.Audio
        if (!audio && s.selectedHeight == null) {
            _state.update { it.copy(notice = "Pick a quality first.") }
            return
        }
        val request = DownloadRequest(
            videoId = info.id,
            url = info.url,
            title = info.title,
            channel = info.channel,
            thumbnailUrl = info.thumbnailUrl,
            durationSec = info.durationSec,
            audioOnly = audio,
            height = if (audio) null else s.selectedHeight,
            audioKey = s.selectedAudio,
            container = s.container,
            embedThumbnail = s.embedThumbnail,
            embedSubtitles = s.embedSubtitles && !audio,
            outputDir = AppPaths.defaultDownloadDir().absolutePath,
        )
        manager.enqueue(request, priority = startNow)
        _state.update {
            it.copy(
                notice = if (startNow) "Download started. Follow it in the Downloads tab."
                else "Added to the queue. See the Downloads tab.",
            )
        }
    }

    fun close() {
        scope.cancel()
    }

    private fun showError(error: FetchError) {
        _state.update { it.copy(phase = FetchPhase.Error, error = error) }
    }

    private fun defaultHeight(info: MediaInfo): Int? =
        info.videoQualities.filter { it.height <= 1080 }.maxByOrNull { it.height }?.height
            ?: info.videoQualities.firstOrNull()?.height
}
