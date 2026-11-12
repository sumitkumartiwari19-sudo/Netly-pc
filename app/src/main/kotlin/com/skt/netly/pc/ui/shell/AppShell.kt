package com.skt.netly.pc.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import com.skt.netly.pc.AppContainer
import com.skt.netly.pc.ui.components.NeuSurface
import com.skt.netly.pc.ui.components.NeuText
import com.skt.netly.pc.ui.components.NeuTextButton
import com.skt.netly.pc.ui.downloads.DownloadsScreen
import com.skt.netly.pc.ui.home.HomeScreen
import com.skt.netly.pc.ui.library.LibraryScreen
import com.skt.netly.pc.ui.settings.SettingsScreen
import com.skt.netly.pc.ui.theme.Neu
import com.skt.netly.pc.ui.theme.ThemeMode

@Composable
fun AppShell(container: AppContainer, onThemeChange: (ThemeMode) -> Unit) {
    var screen by remember { mutableStateOf(Screen.Home) }
    val focus = remember { FocusRequester() }
    val clipboardLink by container.clipboardLink.collectAsState()
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Neu.colors.bg)
            .padding(24.dp)
            .focusRequester(focus)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.isCtrlPressed) {
                    val target = when (event.key) {
                        Key.One -> Screen.Home
                        Key.Two -> Screen.Downloads
                        Key.Three -> Screen.Library
                        Key.Four, Key.Comma -> Screen.Settings
                        else -> null
                    }
                    if (target != null) {
                        screen = target
                        true
                    } else {
                        false
                    }
                } else {
                    false
                }
            },
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Sidebar(
            current = screen,
            onNavigate = { screen = it },
            onThemeChange = onThemeChange,
            modifier = Modifier.width(232.dp).fillMaxHeight(),
        )
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            clipboardLink?.let { link ->
                NeuSurface(Modifier.fillMaxWidth(), radius = 18.dp) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        NeuText("YouTube link copied", style = Neu.type.label, modifier = Modifier.weight(1f))
                        NeuText(link, style = Neu.type.mono, color = Neu.colors.textMuted, maxLines = 1, modifier = Modifier.weight(2f))
                        NeuTextButton("Fetch", primary = true, minHeight = 38.dp, radius = 12.dp, onClick = {
                            container.clipboardLink.value = null
                            screen = Screen.Home
                            container.home.pasteAndFetch(link)
                        })
                        NeuTextButton("Dismiss", minHeight = 38.dp, radius = 12.dp, onClick = { container.clipboardLink.value = null })
                    }
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (screen) {
                    Screen.Home -> HomeScreen(container.home)
                    Screen.Downloads -> DownloadsScreen(container.manager, onGoHome = { screen = Screen.Home })
                    Screen.Library -> LibraryScreen(container.manager, onGoHome = { screen = Screen.Home })
                    Screen.Settings -> SettingsScreen(container, onThemeChange)
                }
            }
        }
    }
}
