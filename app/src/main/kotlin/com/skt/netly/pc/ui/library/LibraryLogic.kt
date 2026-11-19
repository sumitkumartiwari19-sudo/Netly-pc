package com.skt.netly.pc.ui.library

import com.skt.netly.pc.data.library.LibraryItem

enum class LibraryFilter(val label: String) { All("All"), Video("Video"), Audio("Audio") }

enum class LibrarySort(val label: String) { Newest("Newest"), Name("Name") }

object LibraryLogic {
    fun items(all: List<LibraryItem>, query: String, filter: LibraryFilter, sort: LibrarySort): List<LibraryItem> {
        val q = query.trim()
        val list = all
            .filter {
                when (filter) {
                    LibraryFilter.All -> true
                    LibraryFilter.Video -> !it.audioOnly
                    LibraryFilter.Audio -> it.audioOnly
                }
            }
            .filter { q.isEmpty() || it.title.contains(q, ignoreCase = true) || it.channel.contains(q, ignoreCase = true) }
        return when (sort) {
            LibrarySort.Newest -> list.sortedByDescending { it.addedAt }
            LibrarySort.Name -> list.sortedBy { it.title.lowercase() }
        }
    }
}
