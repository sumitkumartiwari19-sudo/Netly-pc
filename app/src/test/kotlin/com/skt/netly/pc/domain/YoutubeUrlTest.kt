package com.skt.netly.pc.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class YoutubeUrlTest {
    private val canonical = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"

    private fun assertVideo(input: String) {
        val link = YoutubeUrl.parse(input)
        assertIs<YoutubeLink.Video>(link, "expected a video for: $input")
        assertEquals(canonical, link.canonicalUrl)
    }

    @Test
    fun acceptsWatchUrls() {
        assertVideo("https://www.youtube.com/watch?v=dQw4w9WgXcQ")
        assertVideo("youtube.com/watch?v=dQw4w9WgXcQ&list=PL123&t=42s")
        assertVideo("  https://m.youtube.com/watch?v=dQw4w9WgXcQ  ")
    }

    @Test
    fun acceptsShortAndShortsUrls() {
        assertVideo("https://youtu.be/dQw4w9WgXcQ?si=abc")
        assertVideo("https://www.youtube.com/shorts/dQw4w9WgXcQ")
        assertVideo("https://music.youtube.com/watch?v=dQw4w9WgXcQ")
    }

    @Test
    fun recognisesPlaylistOnlyLinks() {
        assertEquals(YoutubeLink.PlaylistOnly, YoutubeUrl.parse("https://www.youtube.com/playlist?list=PLabc123"))
    }

    @Test
    fun rejectsEverythingElse() {
        assertEquals(YoutubeLink.Invalid, YoutubeUrl.parse(""))
        assertEquals(YoutubeLink.Invalid, YoutubeUrl.parse("hello world"))
        assertEquals(YoutubeLink.Invalid, YoutubeUrl.parse("https://example.com/watch?v=dQw4w9WgXcQ"))
        assertEquals(YoutubeLink.Invalid, YoutubeUrl.parse("https://www.youtube.com/watch?v=short"))
        assertEquals(YoutubeLink.Invalid, YoutubeUrl.parse("https://evil.com/?u=https://youtu.be/dQw4w9WgXcQ"))
        assertEquals(YoutubeLink.Invalid, YoutubeUrl.parse("https://www.youtube.com/"))
    }

    @Test
    fun readsPlaylistId() {
        assertEquals("PLabc_123", YoutubeUrl.playlistId("https://www.youtube.com/playlist?list=PLabc_123"))
        assertEquals(null, YoutubeUrl.playlistId("https://youtu.be/dQw4w9WgXcQ"))
    }
}
