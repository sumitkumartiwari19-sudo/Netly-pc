package com.skt.netly.pc.util

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class FormattersTest {
    @Test
    fun formatsDuration() {
        assertEquals("0:05", Formatters.duration(5))
        assertEquals("3:32", Formatters.duration(212))
        assertEquals("1:02:05", Formatters.duration(3725))
    }

    @Test
    fun formatsBytes() {
        assertEquals("—", Formatters.bytes(null))
        assertEquals("—", Formatters.bytes(0))
        assertEquals("500 KB", Formatters.bytes(512_000))
        assertEquals("61 MB", Formatters.bytes(63_500_000))
        assertEquals("1.1 GB", Formatters.bytes(1_200_000_000))
        assertEquals("~61 MB", Formatters.approxBytes(63_500_000))
        assertEquals("—", Formatters.approxBytes(null))
    }

    @Test
    fun formatsViews() {
        assertEquals("", Formatters.views(null))
        assertEquals("842 views", Formatters.views(842))
        assertEquals("12K views", Formatters.views(12_000))
        assertEquals("1.2M views", Formatters.views(1_234_567))
        assertEquals("2.5B views", Formatters.views(2_500_000_000))
    }

    @Test
    fun formatsAge() {
        val today = LocalDate.of(2026, 8, 4)
        assertEquals("", Formatters.age(null, today))
        assertEquals("", Formatters.age("garbage", today))
        assertEquals("today", Formatters.age("20260804", today))
        assertEquals("yesterday", Formatters.age("20260803", today))
        assertEquals("3 days ago", Formatters.age("20260801", today))
        assertEquals("2 months ago", Formatters.age("20260604", today))
        assertEquals("1 year ago", Formatters.age("20250801", today))
    }
}
