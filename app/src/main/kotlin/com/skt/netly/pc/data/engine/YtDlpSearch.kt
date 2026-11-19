package com.skt.netly.pc.data.engine

import com.skt.netly.pc.domain.model.SearchItem
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** [items] keeps YouTube's own order (videos and playlists mixed, best match first). */
data class SearchResults(val items: List<SearchItem>) {
    val videos: List<SearchItem> get() = items.filter { !it.isPlaylist }
    val playlists: List<SearchItem> get() = items.filter { it.isPlaylist }
}

/** Searches YouTube. The fast direct request comes first; yt-dlp's flat mode is the slower backup. */
class YtDlpSearch(
    private val engine: EngineRepository,
    private val optionsProvider: () -> EngineOptions = { EngineOptions() },
) {
    private val cache = object : LinkedHashMap<String, SearchResults>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, SearchResults>?) = size > 10
    }

    suspend fun search(query: String): SearchResults {
        val key = query.trim().lowercase()
        synchronized(cache) { cache[key] }?.let { return it }
        val fast = runCatching {
            withContext(Dispatchers.IO) { InnertubeSearchParser.parse(Innertube.search(query, optionsProvider().proxy)) }
        }.getOrNull()
        val result = if (fast != null && fast.any { !it.isPlaylist }) {
            SearchResults(fast)
        } else {
            SearchResults(run(listOf("--playlist-end", "20"), "ytsearch20:$query").items.filter { !it.isPlaylist })
        }
        synchronized(cache) { cache[key] = result }
        return result
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
            ProcessRunner.run(command, timeoutMs = 90_000)
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
