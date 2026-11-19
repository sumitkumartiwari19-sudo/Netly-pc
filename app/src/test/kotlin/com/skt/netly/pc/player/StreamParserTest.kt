package com.skt.netly.pc.player

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class StreamParserTest {
    private val json = """
      {"duration": 200, "http_headers": {"User-Agent": "UA"},
       "formats": [
        {"url":"a-low","protocol":"https","vcodec":"none","acodec":"opus","abr":50},
        {"url":"a-mid","protocol":"https","vcodec":"none","acodec":"mp4a.40.2","abr":129},
        {"url":"a-high","protocol":"https","vcodec":"none","acodec":"opus","abr":250},
        {"url":"v360-av1","protocol":"https","vcodec":"av01.0","acodec":"none","height":360},
        {"url":"v360-avc","protocol":"https","vcodec":"avc1.4d","acodec":"none","height":360},
        {"url":"v720-avc","protocol":"https","vcodec":"avc1.64","acodec":"none","height":720},
        {"url":"v1080","protocol":"https","vcodec":"vp9","acodec":"none","height":1080},
        {"url":"hls","protocol":"m3u8_native","vcodec":"avc1","acodec":"mp4a","height":360}
       ]}
    """.trimIndent()

    @Test
    fun picksAudioUnder160AndPreferredCodecAtHeight() {
        val s = StreamParser.parse(json, 360)
        assertEquals("a-mid", s.audioUrl)
        assertEquals("v360-avc", s.videoUrl)
        assertEquals(200L, s.durationSec)
        assertEquals("UA", s.userAgent)
    }

    @Test
    fun picksHighestHeightNotAboveTarget() {
        assertEquals("v720-avc", StreamParser.parse(json, 720).videoUrl)
        assertEquals("v360-avc", StreamParser.parse(json, 480).videoUrl)
    }

    @Test
    fun fallsBackToLowestWhenAllTooBig() {
        assertEquals("v360-avc", StreamParser.parse(json, 144).videoUrl)
    }

    @Test
    fun usesCombinedStreamWhenOnlyThatExists() {
        val s = StreamParser.parse("""{"formats":[{"url":"m","protocol":"https","vcodec":"avc1","acodec":"mp4a","height":360}]}""", 360)
        assertEquals("m", s.videoUrl)
        assertEquals("m", s.audioUrl)
    }

    @Test
    fun audioOnlyWhenNoVideoAndNothingPlayableWhenEmpty() {
        val a = StreamParser.parse("""{"formats":[{"url":"a","vcodec":"none","acodec":"opus","abr":70}]}""", 360)
        assertNull(a.videoUrl)
        assertNotNull(a.audioUrl)
        assertNull(StreamParser.parse("""{"formats":[]}""", 360).audioUrl)
    }
}

class NativeStreamParserTest {
    private val json = """
      {"playabilityStatus":{"status":"OK"},"videoDetails":{"lengthSeconds":"215"},
       "streamingData":{"adaptiveFormats":[
        {"url":"a1","mimeType":"audio/mp4; codecs=\"mp4a.40.2\"","averageBitrate":130000},
        {"url":"v360","mimeType":"video/mp4; codecs=\"avc1.4d401e\"","height":360,"averageBitrate":400000},
        {"url":"v720","mimeType":"video/mp4; codecs=\"avc1.64001f\"","height":720,"averageBitrate":900000}
       ]}}
    """.trimIndent()

    @Test
    fun picksStreamsFromPlayerApi() {
        val r = StreamParser.parseNative(json, 360, "UA")
        assertNotNull(r)
        assertEquals("a1", r.audioUrl)
        assertEquals("v360", r.videoUrl)
        assertEquals(215L, r.durationSec)
        assertEquals(true, r.native)
    }

    @Test
    fun refusedVideoGivesNull() {
        assertNull(StreamParser.parseNative("""{"playabilityStatus":{"status":"LOGIN_REQUIRED"}}""", 360, "UA"))
    }
}
