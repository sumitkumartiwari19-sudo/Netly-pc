package com.skt.netly.pc.data.engine

import kotlin.test.Test
import kotlin.test.assertEquals

class ErrorMapperTest {
    private fun kind(stderr: String) = ErrorMapper.map(stderr).kind

    @Test
    fun detectsBotBlock() {
        assertEquals(ErrorKind.Blocked, kind("ERROR: [youtube] abc: Sign in to confirm you're not a bot"))
        assertEquals(ErrorKind.Blocked, kind("ERROR: unable to download video data: HTTP Error 403: Forbidden"))
    }

    @Test
    fun ageRestrictionWinsOverSignInText() {
        assertEquals(ErrorKind.AgeRestricted, kind("ERROR: Sign in to confirm your age. This video may be inappropriate"))
    }

    @Test
    fun detectsUnavailableVideos() {
        assertEquals(ErrorKind.Unavailable, kind("ERROR: [youtube] abc: Private video. Sign in if you've been granted access"))
        assertEquals(ErrorKind.Unavailable, kind("ERROR: [youtube] abc: Video unavailable"))
    }

    @Test
    fun detectsNetworkProblems() {
        assertEquals(ErrorKind.Network, kind("ERROR: Unable to download webpage: <urlopen error [Errno 11001] getaddrinfo failed>"))
    }

    @Test
    fun unknownErrorsKeepLastLineAsDetail() {
        val error = ErrorMapper.map("WARNING: something\nERROR: weird failure\n")
        assertEquals(ErrorKind.Unknown, error.kind)
        assertEquals("ERROR: weird failure", error.detail)
    }
}
