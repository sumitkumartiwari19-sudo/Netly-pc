package com.skt.netly.pc.data.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class ProgressParserTest {
    @Test
    fun parsesProgressLine() {
        val line = ProgressParser.parse("NETLY_DL|downloading|1048576|4194304|NA|2097152.5|3|C:\\x\\Video.f137.mp4")
        assertIs<ParsedLine.Progress>(line)
        assertEquals(1_048_576L, line.downloaded)
        assertEquals(4_194_304L, line.total)
        assertEquals(2_097_152L, line.speedBytesPerSec)
        assertEquals(3L, line.etaSec)
    }

    @Test
    fun usesEstimateWhenTotalUnknown() {
        val line = ProgressParser.parse("NETLY_DL|downloading|500|NA|1000|NA|NA|video.mp4")
        assertIs<ParsedLine.Progress>(line)
        assertEquals(1000L, line.total)
        assertNull(line.speedBytesPerSec)
        assertNull(line.etaSec)
    }

    @Test
    fun ignoresSubtitleAndThumbnailFiles() {
        assertEquals(ParsedLine.Other, ProgressParser.parse("NETLY_DL|downloading|100|100|NA|10|0|Video.en.vtt"))
        assertEquals(ParsedLine.Other, ProgressParser.parse("NETLY_DL|finished|100|100|NA|NA|NA|Video.webp"))
    }

    @Test
    fun parsesPostProcessAndFileLines() {
        assertEquals(ParsedLine.Finishing("Merger"), ProgressParser.parse("NETLY_PP|started|Merger"))
        assertEquals(ParsedLine.Other, ProgressParser.parse("NETLY_PP|finished|Merger"))
        assertEquals(ParsedLine.FilePath("C:\\Users\\a\\Downloads\\Netly\\Song.mp3"), ProgressParser.parse("NETLY_FILE|C:\\Users\\a\\Downloads\\Netly\\Song.mp3\r"))
        assertEquals(ParsedLine.Other, ProgressParser.parse("NETLY_FILE|NA"))
    }

    @Test
    fun readsPlainPercentLineAsFallback() {
        assertEquals(ParsedLine.Percent(0.5f), ProgressParser.parse("[download]  50.0% of   10.00MiB at    1.20MiB/s ETA 00:05"))
        assertEquals(ParsedLine.Percent(1f), ProgressParser.parse("[download] 100% of 10.00MiB in 00:08"))
        assertEquals(ParsedLine.Other, ProgressParser.parse("[download] Destination: a.mp4"))
    }

    @Test
    fun ignoresUnrelatedAndBrokenLines() {
        assertEquals(ParsedLine.Other, ProgressParser.parse("[youtube] Extracting URL"))
        assertEquals(ParsedLine.Other, ProgressParser.parse("NETLY_DL|downloading|abc"))
        assertEquals(ParsedLine.Other, ProgressParser.parse(""))
    }
}
