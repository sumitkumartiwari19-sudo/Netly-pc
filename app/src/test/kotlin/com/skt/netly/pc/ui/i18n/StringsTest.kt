package com.skt.netly.pc.ui.i18n

import kotlin.test.Test
import kotlin.test.assertEquals

class StringsTest {
    @Test
    fun englishIsUnchanged() {
        assertEquals("Downloads", Strings.tr("en", "Downloads"))
    }

    @Test
    fun hindiTranslatesKnownTextAndKeepsUnknown() {
        assertEquals("होम", Strings.tr("hi", "Home"))
        assertEquals("some unknown text", Strings.tr("hi", "some unknown text"))
    }

    @Test
    fun hindiKeepsCountsAndJoinedParts() {
        assertEquals("डाउनलोड (3)", Strings.tr("hi", "Downloads (3)"))
    }
}
