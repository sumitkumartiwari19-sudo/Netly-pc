package com.skt.netly.pc.data.engine

import kotlin.test.Test
import kotlin.test.assertEquals

class DownloadErrorMapperTest {
    @Test
    fun mapsCommonFailures() {
        assertEquals("Not enough free space on the disk.", DownloadErrorMapper.map("ERROR: [Errno 28] No space left on device").message)
        assertEquals("Netly cannot write to the download folder.", DownloadErrorMapper.map("PermissionError: [Errno 13] Permission denied").message)
        assertEquals(ErrorKind.EngineMissing, DownloadErrorMapper.map("ERROR: ffmpeg not found").kind)
        assertEquals(ErrorKind.Unavailable, DownloadErrorMapper.map("ERROR: Requested format is not available").kind)
        assertEquals(ErrorKind.Unavailable, DownloadErrorMapper.map("ERROR: Private video").kind)
        assertEquals("The download failed. Try again.", DownloadErrorMapper.map("something odd").message)
    }
}
