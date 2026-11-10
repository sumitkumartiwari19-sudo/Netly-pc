package com.skt.netly.pc.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.test.Test
import kotlin.test.assertTrue

/** Guards the accessibility promise in docs/DESIGN.md: text and state colours stay at 4.5:1 or better. */
class ContrastTest {
    private fun ratio(a: Color, b: Color): Double {
        val l1 = a.luminance().toDouble()
        val l2 = b.luminance().toDouble()
        return (maxOf(l1, l2) + 0.05) / (minOf(l1, l2) + 0.05)
    }

    private fun assertAa(name: String, fg: Color, bg: Color) {
        val r = ratio(fg, bg)
        assertTrue(r >= 4.5, "$name contrast is $r, expected at least 4.5")
    }

    private fun check(label: String, c: NeuColors) {
        assertAa("$label text", c.text, c.bg)
        assertAa("$label textMuted", c.textMuted, c.bg)
        assertAa("$label accentText", c.accentText, c.bg)
        assertAa("$label success", c.success, c.bg)
        assertAa("$label error", c.error, c.bg)
        assertAa("$label onAccent", c.onAccent, c.accent)
    }

    @Test
    fun lightThemeMeetsAa() = check("light", LightNeuColors)

    @Test
    fun darkThemeMeetsAa() = check("dark", DarkNeuColors)
}
