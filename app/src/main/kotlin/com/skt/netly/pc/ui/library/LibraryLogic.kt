package com.skt.netly.pc.ui.library

import com.skt.netly.pc.domain.model.DownloadJob
import com.skt.netly.pc.domain.model.JobStatus

enum class LibraryFilter(val label: String) { All("All"), Video("Video"), Audio("Audio") }

enum class LibrarySort(val label: String) { Newest("Newest"), Name("Name") }

object LibraryLogic {
    fun items(jobs: List<DownloadJob>, query: String, filter: LibraryFilter, sort: LibrarySort): List<DownloadJob> {
        val q = query.trim()
        val list = jobs
            .filter { it.status == JobStatus.Completed }
            .filter {
                when (filter) {
                    LibraryFilter.All -> true
                    LibraryFilter.Video -> !it.request.audioOnly
                    LibraryFilter.Audio -> it.request.audioOnly
                }
            }
            .filter { q.isEmpty() || it.request.title.contains(q, ignoreCase = true) || it.request.channel.contains(q, ignoreCase = true) }
        return when (sort) {
            LibrarySort.Newest -> list.sortedWith(compareByDescending<DownloadJob> { it.createdAt }.thenByDescending { it.id })
            LibrarySort.Name -> list.sortedBy { it.request.title.lowercase() }
        }
    }
}
