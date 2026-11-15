package com.skt.netly.pc.player

import kotlin.test.Test
import kotlin.test.assertEquals

class PlayerEngineTest {
    @Test
    fun parsesFfmpegDuration() {
        assertEquals(3725L, PlayerEngine.parseDuration("  Duration: 01:02:05.40, start: 0.0, bitrate: 128 kb/s"))
        assertEquals(0L, PlayerEngine.parseDuration("no duration here"))
    }

    @Test
    fun mapsQualityToFrameSize() {
        assertEquals(256 to 144, PlayerEngine.sizeFor(144))
        assertEquals(640 to 360, PlayerEngine.sizeFor(360))
        assertEquals(1280 to 720, PlayerEngine.sizeFor(720))
    }

    @Test
    fun queueNavigationFlags() {
        val items = List(3) { PlayItem("k$it", "t$it", "c", "", 10, url = "u") }
        val s = PlayerState(queue = items, index = 1)
        assertEquals(true, s.hasNext)
        assertEquals(true, s.hasPrevious)
        assertEquals("t1", s.item?.title)
        assertEquals("1.5x", PlayerChoices.speedLabel(1.5f))
        assertEquals("2x", PlayerChoices.speedLabel(2f))
    }
}
