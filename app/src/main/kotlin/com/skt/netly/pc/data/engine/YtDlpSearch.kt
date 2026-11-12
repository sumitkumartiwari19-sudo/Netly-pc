package com.skt.netly.pc.data.engine

import com.skt.netly.pc.domain.model.SearchItem
import java.io.IOException
import java.net.URLEncoder
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

data class SearchResults(val videos: List<SearchItem>, val playlists: List<SearchItem>)

/** Searches YouTube and reads playlists with yt-dlp's fast "flat" mode (no stream details, so it is quick). */
class YtDlpSearch(
    private val engine: EngineRepository,
    private val optionsProvider: () -> EngineOptions = { EngineOptions() },
) {
    suspend fun search(query: String): SearchResults = coroutineScope {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val videos = async { run(listOf("--playlist-end", "20"), "ytsearch20:$query") }
        // "EgIQAw%3D%3D" is YouTube's own "Playlist" filter for search results.
        val playlists = async {
            runCatching {
                run(listOf("--playlist-end", "8"), "https://www.youtube.com/results?search_query=$encoded&sp=EgIQAw%253D%253D")
            }.getOrNull()
        }
        val v = videos.await().items.filter { !it.isPlaylist }
        val p = playlists.await()?.items.orEmpty().filter { it.isPlaylist }
        SearchResults(v, p)
    }

    suspend fun playlist(playlistId: String): FlatResult =
        run(listOf("--playlist-end", "300"), "https://www.youtube.com/playlist?list=$playlistId")

    private suspend fun run(extra: List<String>, target: String): FlatResult {
        val exe = engine.awaitReady()
        val command = buildList {
            add(exe.absolutePath)
            addAll(listOf("--ignore-config", "--no-warnings", "--no-colors", "--flat-playlist", "-J"))
            addAll(extra)
            addAll(optionsProvider().networkArgs(includeRate = false))
            add("--")
            add(target)
        }
        val result = try {
            ProcessRunner.run(command, timeoutMs = 60_000)
        } catch (e: ProcessTimeoutException) {
            throw FetchException(FetchError(ErrorKind.Timeout, "YouTube took too long to respond. Check your connection and try again."))
        } catch (e: IOException) {
            throw FetchException(FetchError(ErrorKind.EngineMissing, "Netly's download engine could not start.", e.message))
        }
        if (result.exitCode != 0 || result.stdout.isBlank()) throw FetchException(ErrorMapper.map(result.stderr))
        return try {
            FlatEntryParser.parse(result.stdout)
        } catch (e: Exception) {
            throw FetchException(FetchError(ErrorKind.Unknown, "Could not read the results.", e.message))
        }
    }
}
