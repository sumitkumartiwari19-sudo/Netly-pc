package com.skt.netly.pc.data.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InnertubeSearchParserTest {
    private val json = """
      {"contents":{"sections":[{"items":[
        {"videoRenderer":{"videoId":"aaaaaaaaaaa","title":{"runs":[{"text":"First "},{"text":"song"}]},
          "ownerText":{"runs":[{"text":"Chan A"}]},"lengthText":{"simpleText":"3:25"}}},
        {"playlistRenderer":{"playlistId":"PL1","title":{"simpleText":"Mix"},"videoCount":"12",
          "shortBylineText":{"runs":[{"text":"Chan B"}]}}},
        {"videoRenderer":{"videoId":"bbbbbbbbbbb","title":{"simpleText":"Long"},"ownerText":{"runs":[{"text":"C"}]},
          "lengthText":{"simpleText":"1:02:03"}}},
        {"videoRenderer":{"videoId":"aaaaaaaaaaa","title":{"simpleText":"Duplicate"}}}
      ]}]}}
    """.trimIndent()

    @Test
    fun keepsYoutubeOrderAndJoinsRuns() {
        val items = InnertubeSearchParser.parse(json)
        assertEquals(listOf("aaaaaaaaaaa", "PL1", "bbbbbbbbbbb"), items.map { it.id })
        assertEquals("First song", items[0].title)
        assertEquals(205L, items[0].durationSec)
        assertTrue(items[1].isPlaylist)
        assertEquals(12, items[1].count)
        assertEquals(3723L, items[2].durationSec)
    }

    @Test
    fun clockParsing() {
        assertEquals(65L, InnertubeSearchParser.parseClock("1:05"))
        assertEquals(null, InnertubeSearchParser.parseClock(""))
    }
}
