package com.skt.netly.pc

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.skt.netly.pc.ui.shell.AppShell
import com.skt.netly.pc.ui.theme.NetlyTheme
import com.skt.netly.pc.ui.theme.ThemeMode
import java.awt.Dimension

fun main() = application {
    var mode by remember { mutableStateOf(ThemeMode.System) }

    Window(
        onCloseRequest = ::exitApplication,
        title = AppInfo.NAME,
        state = rememberWindowState(width = 1280.dp, height = 800.dp),
    ) {
        LaunchedEffect(Unit) { window.minimumSize = Dimension(1100, 700) }
        NetlyTheme(mode) {
            AppShell(onThemeChange = { mode = it })
        }
    }
}
 ::exitApplication,
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
