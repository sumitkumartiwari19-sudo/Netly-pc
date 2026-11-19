package com.skt.netly.pc.data.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MediaInfoParserTest {
    private val sample = """
        {
          "id": "dQw4w9WgXcQ",
          "title": "Sample Video",
          "channel": "Sample Channel",
          "duration": 212,
          "view_count": 1234567,
          "upload_date": "20260801",
          "is_live": false,
          "subtitles": {"en": [{"ext": "vtt"}], "hi": [{"ext": "vtt"}]},
          "chapters": [{"title": "Intro"}, {"title": "Main"}],
          "formats": [
            {"format_id": "140", "ext": "m4a", "vcodec": "none", "acodec": "mp4a.40.2", "abr": 129.5, "filesize": 3400000},
            {"format_id": "251", "ext": "webm", "vcodec": "none", "acodec": "opus", "abr": 135.0, "filesize": 3500000},
            {"format_id": "137", "ext": "mp4", "vcodec": "avc1.640028", "acodec": "none", "height": 1080, "filesize": 60000000},
            {"format_id": "248", "ext": "webm", "vcodec": "vp9", "acodec": "none", "height": 1080, "filesize": 40000000},
            {"format_id": "136", "ext": "mp4", "vcodec": "avc1.4d401f", "acodec": "none", "height": 720, "filesize_approx": 30000000},
            {"format_id": "18", "ext": "mp4", "vcodec": "avc1.42001E", "acodec": "mp4a.40.2", "height": 360, "filesize": 15000000},
            {"format_id": "sb0", "ext": "mhtml", "vcodec": "none", "acodec": "none", "height": 90}
          ]
        }
    """.trimIndent()

    @Test
    fun readsBasicDetails() {
        val info = MediaInfoParser.parse(sample)
        assertEquals("dQw4w9WgXcQ", info.id)
        assertEquals("Sample Video", info.title)
        assertEquals("Sample Channel", info.channel)
        assertEquals(212L, info.durationSec)
        assertEquals(1_234_567L, info.viewCount)
        assertEquals("20260801", info.uploadDate)
        assertEquals("https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg", info.thumbnailUrl)
        assertEquals(2, info.subtitleLanguages)
        assertEquals(2, info.chapterCount)
        assertFalse(info.isLive)
    }

    @Test
    fun buildsVideoQualitiesWithSizes() {
        val info = MediaInfoParser.parse(sample)
        assertEquals(listOf(1080, 720, 360), info.videoQualities.map { it.height })
        assertEquals(listOf("1080p", "720p", "360p"), info.videoQualities.map { it.label })
        // Prefers the mp4 stream and adds the best audio-only stream (3.5 MB) because the video stream has no audio.
        assertEquals(63_500_000L, info.videoQualities[0].estimatedBytes)
        assertEquals(33_500_000L, info.videoQualities[1].estimatedBytes)
        // Muxed stream already contains audio.
        assertEquals(15_000_000L, info.videoQualities[2].estimatedBytes)
        assertEquals(1080, info.maxHeight)
    }

    @Test
    fun buildsAudioFormatsFromDuration() {
        val info = MediaInfoParser.parse(sample)
        assertEquals(listOf("mp3-320", "mp3-192", "m4a", "opus"), info.audioFormats.map { it.key })
        assertEquals(212L * 320_000L / 8, info.audioFormats[0].estimatedBytes)
        assertEquals(129, info.audioFormats[2].bitrateKbps)
        assertEquals(135, info.audioFormats[3].bitrateKbps)
    }

    @Test
    fun labelsFourKQuality() {
        val json = """{"id":"abcdefghijk","formats":[
            {"ext":"mp4","vcodec":"avc1","acodec":"none","height":2160,"filesize":900000000}]}"""
        val info = MediaInfoParser.parse(json)
        assertEquals("2160p · 4K", info.videoQualities.single().label)
        assertNull(MediaInfoParser.parse("""{"id":"abcdefghijk"}""").viewCount)
    }

    @Test
    fun detectsLiveStreams() {
        assertTrue(MediaInfoParser.parse("""{"id":"abcdefghijk","is_live":true}""").isLive)
    }

    @Test
    fun failsWithoutId() {
        assertFailsWith<IllegalArgumentException> {
            MediaInfoParser.parse("""{"title":"No id"}""")
        }
    }
}
