package com.skt.netly.pc.data.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FlatEntryParserTest {
    private val json = """
        {"title":"My list","entries":[
          {"id":"dQw4w9WgXcQ","title":"Song","channel":"Chan","duration":212.0,"url":"https://www.youtube.com/watch?v=dQw4w9WgXcQ"},
          {"id":"abcdefghijk","title":"[Private video]"},
          {"id":"PLabc123","title":"A playlist","uploader":"Up","ie_key":"YoutubeTab","url":"https://www.youtube.com/playlist?list=PLabc123","playlist_count":12},
          {"title":"no id"}
        ]}
    """.trimIndent()

    @Test
    fun readsVideosAndPlaylists() {
        val r = FlatEntryParser.parse(json)
        assertEquals("My list", r.title)
        assertEquals(2, r.items.size)
        val video = r.items[0]
        assertEquals("dQw4w9WgXcQ", video.id)
        assertEquals(212L, video.durationSec)
        assertEquals("https://i.ytimg.com/vi/dQw4w9WgXcQ/mqdefault.jpg", video.thumbnailUrl)
        assertFalse(video.isPlaylist)
        val list = r.items[1]
        assertTrue(list.isPlaylist)
        assertEquals(12, list.count)
        assertEquals("https://www.youtube.com/playlist?list=PLabc123", list.url)
    }

    @Test
    fun noEntriesGivesEmptyList() {
        assertEquals(0, FlatEntryParser.parse("""{"title":"x"}""").items.size)
    }
}
