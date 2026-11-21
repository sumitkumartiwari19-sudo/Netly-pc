package com.skt.netly.pc.data.update

import com.skt.netly.pc.platform.Links
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppUpdateCheckerTest {
    @Test
    fun versionOrder() {
        assertTrue(Versions.isNewer("v1.0.1", "1.0.0"))
        assertTrue(Versions.isNewer("v1.1", "1.0.9"))
        assertFalse(Versions.isNewer("v1.0.0", "1.0.0"))
        assertFalse(Versions.isNewer("v0.9.9", "1.0.0"))
        assertFalse(Versions.isNewer("v9.0.0", "dev"))
        assertTrue(Versions.isNewer("v1.0.0", "0.0.1"))
    }

    @Test
    fun parsesLatestRelease() {
        val json = """{"tag_name":"v1.2.0","html_url":"https://github.com/sumitkumartiwari19-sudo/Netly-pc/releases/tag/v1.2.0","body":"Notes"}"""
        val r = AppUpdateChecker.parse(json, "1.0.0") as UpdateCheck.Available
        assertEquals("1.2.0", r.update.version)
        assertEquals(UpdateCheck.UpToDate, AppUpdateChecker.parse(json, "1.2.0"))
    }

    @Test
    fun foreignReleaseLinkIsReplaced() {
        val json = """{"tag_name":"v2.0.0","html_url":"https://evil.example/x"}"""
        val r = AppUpdateChecker.parse(json, "1.0.0") as UpdateCheck.Available
        assertTrue(r.update.pageUrl.startsWith("https://github.com/sumitkumartiwari19-sudo/Netly-pc/"))
    }

    @Test
    fun onlyKnownHttpsLinksOpen() {
        assertTrue(Links.isAllowed("https://github.com/sumitkumartiwari19-sudo"))
        assertTrue(Links.isAllowed("https://www.instagram.com/sumitkumartiwari19"))
        assertFalse(Links.isAllowed("http://github.com/x"))
        assertFalse(Links.isAllowed("https://evil.example/x"))
        assertFalse(Links.isAllowed("file:///C:/Windows/System32/cmd.exe"))
    }

    @Test
    fun mailLinkIsEncoded() {
        val m = Links.mailtoUri("a@b.c", "Netly feedback", "Hi there\nline2")
        assertTrue(m.startsWith("mailto:a@b.c?subject=Netly%20feedback&body=Hi%20there%0Aline2"))
    }
}
