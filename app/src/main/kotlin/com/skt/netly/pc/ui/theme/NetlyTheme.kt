package com.skt.netly.pc.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

data class NeuTypography(
    val display: TextStyle,
    val title: TextStyle,
    val body: TextStyle,
    val label: TextStyle,
    val caption: TextStyle,
    val overline: TextStyle,
    val mono: TextStyle,
)

// System font for now; Manrope and JetBrains Mono are bundled in M2.
val DefaultNeuTypography = NeuTypography(
    display = TextStyle(fontSize = 34.sp, lineHeight = 40.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.7).sp),
    title = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.ExtraBold),
    body = TextStyle(fontSize = 15.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    label = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold),
    caption = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold),
    overline = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.7.sp),
    mono = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace),
)

val LocalNeuColors = staticCompositionLocalOf { LightNeuColors }
val LocalNeuTypography = staticCompositionLocalOf { DefaultNeuTypography }

object Neu {
    val colors: NeuColors
        @Composable @ReadOnlyComposable get() = LocalNeuColors.current

    val type: NeuTypography
        @Composable @ReadOnlyComposable get() = LocalNeuTypography.current
}

@Composable
fun NetlyTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
        ThemeMode.System -> isSystemInDarkTheme()
    }
    CompositionLocalProvider(
        LocalNeuColors provides if (dark) DarkNeuColors else LightNeuColors,
        LocalNeuTypography provides DefaultNeuTypography,
    ) {
        content()
    }
}
