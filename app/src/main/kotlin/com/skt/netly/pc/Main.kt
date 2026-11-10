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
