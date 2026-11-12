package com.skt.netly.pc.data.engine

import com.skt.netly.pc.domain.model.DownloadRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DownloadCommandBuilderTest {
    private fun request(
        audioOnly: Boolean = false,
        height: Int? = 1080,
        audioKey: String = "mp3-320",
        container: String = "MP4",
        thumb: Boolean = true,
        subs: Boolean = false,
    ) = DownloadRequest(
        videoId = "abcdefghijk",
        url = "https://www.youtube.com/watch?v=abcdefghijk",
        title = "T",
        channel = "C",
        thumbnailUrl = "",
        durationSec = 100,
        audioOnly = audioOnly,
        height = height,
        audioKey = audioKey,
        container = container,
        embedThumbnail = thumb,
        embedSubtitles = subs,
        outputDir = "C:\\Users\\a\\Downloads\\Netly",
    )

    private fun build(r: DownloadRequest) = DownloadCommandBuilder.build("yt-dlp.exe", "C:\\engine", r)

    private fun List<String>.valueAfter(flag: String): String = this[indexOf(flag) + 1]

    @Test
    fun buildsMp4VideoCommand() {
        val cmd = build(request())
        assertEquals("yt-dlp.exe", cmd.first())
        assertEquals("--", cmd[cmd.size - 2])
        assertEquals("https://www.youtube.com/watch?v=abcdefghijk", cmd.last())
        assertTrue(cmd.valueAfter("-f").startsWith("bv*[vcodec^=avc1][height<=1080]+ba[ext=m4a]"))
        assertEquals("mp4", cmd.valueAfter("--merge-output-format"))
        assertEquals("C:\\Users\\a\\Downloads\\Netly", cmd.valueAfter("-P"))
        assertEquals("C:\\engine", cmd.valueAfter("--ffmpeg-location"))
        assertTrue("--embed-thumbnail" in cmd)
        assertFalse("--embed-subs" in cmd)
        assertTrue("--no-playlist" in cmd)
    }

    @Test
    fun mp4PrefersH264AndNeverFallsBackToAv1FirstChoice() {
        val f = build(request()).let { it[it.indexOf("-f") + 1] }
        assertTrue(f.indexOf("avc1") in 0 until f.indexOf("av01"))
    }

    @Test
    fun webmSkipsThumbnailAndKeepsStrictFormat() {
        val cmd = build(request(container = "WEBM", subs = true))
        assertEquals("webm", cmd.valueAfter("--merge-output-format"))
        assertFalse("--embed-thumbnail" in cmd)
        assertTrue("--embed-subs" in cmd)
        assertTrue(cmd.valueAfter("-f").contains("[ext=webm]"))
    }

    @Test
    fun buildsAudioCommands() {
        val mp3 = build(request(audioOnly = true, audioKey = "mp3-192"))
        assertTrue("-x" in mp3)
        assertEquals("mp3", mp3.valueAfter("--audio-format"))
        assertEquals("192K", mp3.valueAfter("--audio-quality"))
        assertFalse("--merge-output-format" in mp3)

        val m4a = build(request(audioOnly = true, audioKey = "m4a"))
        assertEquals("m4a", m4a.valueAfter("--audio-format"))

        val opus = build(request(audioOnly = true, audioKey = "opus", thumb = false))
        assertEquals("opus", opus.valueAfter("--audio-format"))
        assertFalse("--embed-thumbnail" in opus)
    }

    @Test
    fun bestQualityHasNoHeightLimit() {
        val cmd = build(request(height = null, container = "MKV"))
        assertEquals("bv*+ba/b", cmd.valueAfter("-f"))
    }

    @Test
    fun omitsFfmpegLocationWhenMissing() {
        val cmd = DownloadCommandBuilder.build("yt-dlp", null, request())
        assertFalse("--ffmpeg-location" in cmd)
    }

    @Test
    fun qualityLabels() {
        assertEquals("1080p · MP4", request().qualityLabel)
        assertEquals("MP3 192 kbps", request(audioOnly = true, audioKey = "mp3-192").qualityLabel)
    }
}
