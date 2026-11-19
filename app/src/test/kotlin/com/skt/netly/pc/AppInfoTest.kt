package com.skt.netly.pc

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class AppInfoTest {
    @Test
    fun appNameIsNetly() {
        assertEquals("Netly", AppInfo.NAME)
    }

    @Test
    fun missingToolIsReportedAsNotBundled() {
        assertFalse(AppInfo.hasBundledTool("definitely-not-a-real-tool.exe"))
    }
}
