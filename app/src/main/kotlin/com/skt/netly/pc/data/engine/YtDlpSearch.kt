package com.skt.netly.pc.data.engine

import com.skt.netly.pc.domain.model.SearchItem
import java.io.IOException
import java.net.URLEncoder
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/** [items] keeps YouTube's own order (videos and playlists mixed, best match first). */
data class SearchResults(val items: List<SearchItem>) {
    val videos: List<SearchItem> get() = items.filter { !it.isPlaylist }
    val playlists: List<SearchItem> get() = items.filter { it.isPlaylist }
}

/** Searches YouTube and reads playlists with yt-dlp's fast "flat" mode (no stream details, so it is quick). */
class YtDlpSearch(
    private val engine: EngineRepository,
    private val optionsProvider: () -> EngineOptions = { EngineOptions() },
) {
    suspend fun search(query: String): SearchResults = coroutineScope {
        val encoded = URLEncoder.encode(query, "UTF-8")
        // The normal results page has YouTube's own ranking, so a playlist appears first only when it really matches best.
        val mixed = async {
            runCatching { run(listOf("--playlist-end", "30"), "https://www.youtube.com/results?search_query=$encoded") }.getOrNull()
        }
        val plain = async { runCatching { run(listOf("--playlist-end", "20"), "ytsearch20:$query") }.getOrNull() }
        val ordered = mixed.await()?.items.orEmpty().filter { !it.isPlaylist || it.isRealPlaylist() }
        val fallback = plain.await()?.items.orEmpty().filter { !it.isPlaylist }
        val result = if (ordered.count { !it.isPlaylist } >= 3) ordered else (ordered + fallback).distinctBy { it.id }
        if (result.isEmpty() && fallback.isEmpty()) {
            // Both failed: raise the real error.
            run(listOf("--playlist-end", "20"), "ytsearch20:$query")
        }
        SearchResults(result)
    }

    private fun SearchItem.isRealPlaylist(): Boolean = "list=" in url || "/playlist" in url

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
