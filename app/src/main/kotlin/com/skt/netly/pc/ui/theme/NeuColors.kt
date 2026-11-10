package com.skt.netly.pc.ui.theme

import androidx.compose.ui.graphics.Color

enum class ThemeMode { Light, Dark, System }

data class NeuColors(
    val bg: Color,
    val shadowDark: Color,
    val shadowLight: Color,
    val text: Color,
    val textMuted: Color,
    val accent: Color,
    val accentText: Color,
    val onAccent: Color,
    val success: Color,
    val error: Color,
    val isDark: Boolean,
)

val LightNeuColors = NeuColors(
    bg = Color(0xFFE6EAF1),
    shadowDark = Color(0xFFBCC4D3),
    shadowLight = Color(0xFFFFFFFF),
    text = Color(0xFF1F2433),
    textMuted = Color(0xFF586177),
    accent = Color(0xFF4F5BFF),
    accentText = Color(0xFF3A45E0),
    onAccent = Color(0xFFFFFFFF),
    success = Color(0xFF0E6B4C),
    error = Color(0xFFB3321F),
    isDark = false,
)

val DarkNeuColors = NeuColors(
    bg = Color(0xFF23272F),
    shadowDark = Color(0xFF15181D),
    shadowLight = Color(0xFF31373F),
    text = Color(0xFFECEFF6),
    textMuted = Color(0xFFA5AEC2),
    accent = Color(0xFF4F5BFF),
    accentText = Color(0xFF9AA4FF),
    onAccent = Color(0xFFFFFFFF),
    success = Color(0xFF5FD3A2),
    error = Color(0xFFFF8A7A),
    isDark = true,
)
