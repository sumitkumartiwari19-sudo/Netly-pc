package com.skt.netly.pc.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.LaunchedEffect
import com.skt.netly.pc.ui.player.DownloadDialog
import com.skt.netly.pc.ui.player.FullscreenPlayer
import com.skt.netly.pc.ui.theme.NeuPerf
import androidx.compose.foundation.layout.Box
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
import com.skt.netly.pc.ui.player.FullPlayer
import com.skt.netly.pc.ui.player.MiniPlayer
import com.skt.netly.pc.ui.search.SearchScreen
import com.skt.netly.pc.ui.settings.SettingsScreen
import com.skt.netly.pc.ui.theme.Neu
import com.skt.netly.pc.ui.theme.ThemeMode

@Composable
fun AppShell(container: AppContainer, onThemeChange: (ThemeMode) -> Unit) {
    var screen by remember { mutableStateOf(Screen.Home) }
    val focus = remember { FocusRequester() }
    val clipboardLink by container.clipboardLink.collectAsState()
    val requested by container.requestScreen.collectAsState()
    val playerOpen by container.playerOpen.collectAsState()
    val playing by container.player.state.collectAsState()
    val settings by container.settings.state.collectAsState()
    val fullscreen by container.fullscreen.collectAsState()
    val inFullscreen = fullscreen && playing.item != null
    LaunchedEffect(playing.item == null) { if (playing.item == null) container.fullscreen.value = false }
    LaunchedEffect(requested) {
        requested?.let {
            screen = it
            container.requestScreen.value = null
        }
    }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (inFullscreen) androidx.compose.ui.graphics.Color.Black else Neu.colors.bg)
            .focusRequester(focus)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && !event.isCtrlPressed && inFullscreen && event.key == Key.Escape) {
                    container.fullscreen.value = false
                    true
                } else if (event.type == KeyEventType.KeyDown && !event.isCtrlPressed && event.key == Key.F11 && playing.item != null) {
                    container.fullscreen.value = !container.fullscreen.value
                    true
                } else if (event.type == KeyEventType.KeyDown && !event.isCtrlPressed && inFullscreen && event.key == Key.Spacebar) {
                    container.player.togglePause()
                    true
                } else if (event.type == KeyEventType.KeyDown && event.isCtrlPressed) {
                    val target = when (event.key) {
                        Key.One -> Screen.Home
                        Key.Two -> Screen.Search
                        Key.Three -> Screen.Downloads
                        Key.Four -> Screen.Library
                        Key.Five, Key.Comma -> Screen.Settings
                        else -> null
                    }
                    if (target != null) {
                        screen = target
                        container.playerOpen.value = false
                        true
                    } else {
                        false
                    }
                } else {
                    false
                }
            },
    ) {
      if (inFullscreen) {
        FullscreenPlayer(container.player, onExit = { container.fullscreen.value = false })
      } else Row(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        val sidebarWidth by animateDpAsState(
            if (settings.sidebarCollapsed) 72.dp else 232.dp,
            animationSpec = if (NeuPerf.reduced) snap() else tween(200),
        )
        Sidebar(
            current = screen,
            collapsed = settings.sidebarCollapsed,
            onToggleCollapsed = { container.settings.update { it.copy(sidebarCollapsed = !it.sidebarCollapsed) } },
            onNavigate = {
                screen = it
                container.playerOpen.value = false
            },
            onThemeChange = onThemeChange,
            modifier = Modifier.width(sidebarWidth).fillMaxHeight(),
        )
        Column(Modifier.weight(1f).fillMaxHeight()) {
            clipboardLink?.let { link ->
                NeuSurface(Modifier.fillMaxWidth().padding(bottom = 16.dp), radius = 18.dp) {
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
                val showPlayer = playerOpen && playing.item != null
                Crossfade(
                    targetState = if (showPlayer) "player" else screen.name,
                    animationSpec = if (NeuPerf.reduced) snap() else tween(180),
                ) { key ->
                    if (key == "player") {
                        FullPlayer(
                            container.player,
                            onClose = { container.playerOpen.value = false },
                            onDownload = container::downloadCurrent,
                            onFullscreen = { container.fullscreen.value = true },
                        )
                    } else when (Screen.valueOf(key)) {
                        Screen.Home -> HomeScreen(container.home)
                        Screen.Search -> SearchScreen(container.search)
                        Screen.Downloads -> DownloadsScreen(container.manager, onGoHome = { screen = Screen.Home })
                        Screen.Library -> LibraryScreen(
                            container.library,
                            onPlay = { items, index -> container.playLibrary(items, index) },
                            onGoHome = { screen = Screen.Home },
                        )
                        Screen.Settings -> SettingsScreen(container, onThemeChange)
                    }
                }
            }
            AnimatedVisibility(
                visible = !playerOpen && playing.item != null,
                enter = if (NeuPerf.reduced) EnterTransition.None else fadeIn(tween(160)) + expandVertically(tween(200)),
                exit = if (NeuPerf.reduced) ExitTransition.None else fadeOut(tween(120)) + shrinkVertically(tween(160)),
            ) {
                MiniPlayer(container.player, onExpand = { container.playerOpen.value = true }, modifier = Modifier.padding(top = 16.dp))
            }
        }
      }
      val dialogItem by container.downloadDialog.collectAsState()
      dialogItem?.let { di ->
        DownloadDialog(
            engine = container.player,
            item = di,
            onDismiss = { container.downloadDialog.value = null },
            onStart = { audio, h, key -> container.startDownload(di, audio, h, key) },
        )
      }
    }
}
