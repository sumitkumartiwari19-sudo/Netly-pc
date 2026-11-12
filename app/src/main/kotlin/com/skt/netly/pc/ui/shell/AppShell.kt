package com.skt.netly.pc.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.skt.netly.pc.data.downloads.DownloadManager
import com.skt.netly.pc.data.downloads.DownloadStore
import com.skt.netly.pc.data.engine.EngineRepository
import com.skt.netly.pc.data.engine.YtDlpClient
import com.skt.netly.pc.data.engine.YtDlpDownloader
import com.skt.netly.pc.platform.AppPaths
import com.skt.netly.pc.ui.home.HomeScreen
import com.skt.netly.pc.ui.home.HomeViewModel
import com.skt.netly.pc.ui.screens.PlaceholderScreen
import com.skt.netly.pc.ui.theme.Neu
import com.skt.netly.pc.ui.theme.ThemeMode

@Composable
fun AppShell(onThemeChange: (ThemeMode) -> Unit) {
    var screen by remember { mutableStateOf(Screen.Home) }
    val engine = remember { EngineRepository() }
    val manager = remember { DownloadManager(YtDlpDownloader(engine), DownloadStore(AppPaths.downloadsFile())) }
    val homeViewModel = remember { HomeViewModel(engine, YtDlpClient(engine), manager) }
    DisposableEffect(homeViewModel) {
        onDispose {
            homeViewModel.close()
            manager.close()
        }
    }

    Row(
        modifier = Modifier.fillMaxSize().background(Neu.colors.bg).padding(24.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Sidebar(
            current = screen,
            onNavigate = { screen = it },
            onThemeChange = onThemeChange,
            modifier = Modifier.width(232.dp).fillMaxHeight(),
        )
        Box(Modifier.weight(1f).fillMaxHeight()) {
            when (screen) {
                Screen.Home -> HomeScreen(homeViewModel)
                Screen.Downloads -> PlaceholderScreen("Downloads", "Everything Netly is fetching right now.", "M3")
                Screen.Library -> PlaceholderScreen("Library", "Your finished downloads, ready to play offline.", "M4")
                Screen.Settings -> PlaceholderScreen("Settings", "Make Netly work the way you like.", "M4")
            }
        }
    }
}
