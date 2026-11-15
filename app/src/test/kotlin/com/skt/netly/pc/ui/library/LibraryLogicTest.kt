package com.skt.netly.pc.ui.library

import com.skt.netly.pc.data.library.LibraryItem
import kotlin.test.Test
import kotlin.test.assertEquals

class LibraryLogicTest {
    private fun item(n: Int, title: String, audio: Boolean = false) =
        LibraryItem("C:\\m\\$n", title, "Chan$n", "", 1, audio, "720p", 10, n.toLong())

    private val all = listOf(item(1, "Banana song", audio = true), item(2, "apple video"), item(3, "Cherry"))

    private fun ids(q: String, f: LibraryFilter, s: LibrarySort) = LibraryLogic.items(all, q, f, s).map { it.path.last() }

    @Test
    fun filtersByKind() {
        assertEquals(listOf('3', '2', '1'), ids("", LibraryFilter.All, LibrarySort.Newest))
        assertEquals(listOf('1'), ids("", LibraryFilter.Audio, LibrarySort.Newest))
        assertEquals(listOf('3', '2'), ids("", LibraryFilter.Video, LibrarySort.Newest))
    }

    @Test
    fun searchAndSort() {
        assertEquals(listOf('2'), ids("APPLE", LibraryFilter.All, LibrarySort.Newest))
        assertEquals(listOf('2', '1', '3'), ids("", LibraryFilter.All, LibrarySort.Name))
        assertEquals(listOf('3'), ids("chan3", LibraryFilter.All, LibrarySort.Name))
    }
}
