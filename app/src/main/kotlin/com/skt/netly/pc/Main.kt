package com.skt.netly.pc

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

private val Bg = Color(0xFFE6EAF1)
private val Text = Color(0xFF1F2433)
private val Muted = Color(0xFF586177)
private val Accent = Color(0xFF3A45E0)

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = AppInfo.NAME,
        state = rememberWindowState(width = 1100.dp, height = 700.dp),
    ) {
        HelloScreen()
    }
}

@Composable
private fun HelloScreen() {
    val ytDlp = AppInfo.hasBundledTool("yt-dlp.exe")
    val ffmpeg = AppInfo.hasBundledTool("ffmpeg.exe")

    Column(
        modifier = Modifier.fillMaxSize().background(Bg).padding(48.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BasicText(
            text = "Netly for PC",
            style = TextStyle(color = Text, fontSize = 44.sp, fontWeight = FontWeight.ExtraBold),
        )
        BasicText(
            text = "M0: project setup works",
            modifier = Modifier.padding(top = 8.dp),
            style = TextStyle(color = Accent, fontSize = 20.sp, fontWeight = FontWeight.Bold),
        )
        BasicText(
            text = "yt-dlp bundled: ${if (ytDlp) "yes" else "no"}   ·   ffmpeg bundled: ${if (ffmpeg) "yes" else "no"}",
            modifier = Modifier.padding(top = 24.dp),
            style = TextStyle(color = Muted, fontSize = 15.sp),
        )
    }
}
