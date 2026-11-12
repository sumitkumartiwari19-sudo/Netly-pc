package com.skt.netly.pc.ui.theme

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font

/** Fonts bundled in the app (src/main/resources/fonts). If one is missing the system font is used. */
object NeuFonts {
    private fun exists(res: String): Boolean = Thread.currentThread().contextClassLoader?.getResource(res) != null

    private fun load(res: String, weight: FontWeight): androidx.compose.ui.text.font.Font? =
        if (exists(res)) Font(resource = res, weight = weight) else null

    private fun family(fonts: List<androidx.compose.ui.text.font.Font?>, fallback: FontFamily): FontFamily =
        fonts.filterNotNull().let { if (it.isEmpty()) fallback else FontFamily(it) }

    val manrope: FontFamily by lazy {
        family(
            listOf(
                load("fonts/Manrope-Medium.ttf", FontWeight.Medium),
                load("fonts/Manrope-SemiBold.ttf", FontWeight.SemiBold),
                load("fonts/Manrope-Bold.ttf", FontWeight.Bold),
                load("fonts/Manrope-ExtraBold.ttf", FontWeight.ExtraBold),
            ),
            FontFamily.Default,
        )
    }

    val mono: FontFamily by lazy {
        family(listOf(load("fonts/JetBrainsMono-Medium.ttf", FontWeight.Medium)), FontFamily.Monospace)
    }

    /** Used when the app language is Hindi, so Devanagari text looks right. */
    val devanagari: FontFamily by lazy {
        family(
            listOf(
                load("fonts/NotoSansDevanagari-Regular.ttf", FontWeight.Normal),
                load("fonts/NotoSansDevanagari-Regular.ttf", FontWeight.Medium),
                load("fonts/NotoSansDevanagari-Bold.ttf", FontWeight.SemiBold),
                load("fonts/NotoSansDevanagari-Bold.ttf", FontWeight.Bold),
                load("fonts/NotoSansDevanagari-Bold.ttf", FontWeight.ExtraBold),
            ),
            FontFamily.Default,
        )
    }
}

fun neuTypography(language: String): NeuTypography {
    val base = DefaultNeuTypography
    val text = if (language == "hi") NeuFonts.devanagari else NeuFonts.manrope
    return NeuTypography(
        display = base.display.copy(fontFamily = text),
        title = base.title.copy(fontFamily = text),
        body = base.body.copy(fontFamily = text),
        label = base.label.copy(fontFamily = text),
        caption = base.caption.copy(fontFamily = text),
        overline = base.overline.copy(fontFamily = text),
        mono = base.mono.copy(fontFamily = NeuFonts.mono),
    )
}
