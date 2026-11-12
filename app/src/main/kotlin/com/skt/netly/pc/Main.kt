package com.skt.netly.pc

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Notification
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberTrayState
import androidx.compose.ui.window.rememberWindowState
import com.skt.netly.pc.domain.YoutubeLink
import com.skt.netly.pc.domain.YoutubeUrl
import com.skt.netly.pc.domain.model.JobStatus
import com.skt.netly.pc.platform.SingleInstance
import com.skt.netly.pc.platform.SystemClipboard
import com.skt.netly.pc.ui.shell.AppShell
import com.skt.netly.pc.ui.theme.NetlyTheme
import com.skt.netly.pc.ui.theme.NeuPerf
import com.skt.netly.pc.ui.theme.ThemeMode
import java.awt.Dimension
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** Simple tray icon drawn in code: an indigo square with a download arrow. */
private object TrayIcon : Painter() {
    override val intrinsicSize: Size = Size(64f, 64f)

    override fun DrawScope.onDraw() {
        drawRoundRect(Color(0xFF4F5BFF), size = size, cornerRadius = CornerRadius(size.width * 0.22f))
        val w = size.width
        val stroke = w * 0.09f
        drawLine(Color.White, Offset(w * 0.5f, w * 0.2f), Offset(w * 0.5f, w * 0.62f), strokeWidth = stroke)
        drawLine(Color.White, Offset(w * 0.5f, w * 0.64f), Offset(w * 0.32f, w * 0.46f), strokeWidth = stroke)
        drawLine(Color.White, Offset(w * 0.5f, w * 0.64f), Offset(w * 0.68f, w * 0.46f), strokeWidth = stroke)
        drawLine(Color.White, Offset(w * 0.25f, w * 0.80f), Offset(w * 0.75f, w * 0.80f), strokeWidth = stroke)
    }
}

fun main() {
    if (!SingleInstance.acquire()) return

    application {
        val container = remember { AppContainer() }
        val settings by container.settings.state.collectAsState()
        val trayState = rememberTrayState()
        var visible by remember { mutableStateOf(true) }
        SideEffect { NeuPerf.reduced = settings.reducedEffects }
        val mode = runCatching { ThemeMode.valueOf(settings.theme) }.getOrDefault(ThemeMode.System)

        fun quit() {
            container.close()
            exitApplication()
        }

        // Notify when a download finishes or fails.
        LaunchedEffect(container) {
            val seen = HashMap<Long, JobStatus>()
            container.manager.jobs.collect { list ->
                list.forEach { job ->
                    val before = seen[job.id]
                    if (before != null && before != job.status && container.settings.state.value.notifications) {
                        when (job.status) {
                            JobStatus.Completed -> trayState.sendNotification(Notification("Download complete", job.request.title, Notification.Type.Info))
                            JobStatus.Failed -> trayState.sendNotification(Notification("Download failed", job.request.title, Notification.Type.Error))
                            else -> Unit
                        }
                    }
                    seen[job.id] = job.status
                }
            }
        }

        // Clipboard watcher: offers to fetch YouTube links that were just copied.
        LaunchedEffect(settings.clipboardWatcher) {
            if (!settings.clipboardWatcher) return@LaunchedEffect
            var last = withContext(Dispatchers.IO) { SystemClipboard.readText() }?.trim()
            while (true) {
                delay(1500)
                val text = withContext(Dispatchers.IO) { SystemClipboard.readText() }?.trim()
                if (text != null && text != last) {
                    last = text
                    if (YoutubeUrl.parse(text) is YoutubeLink.Video) {
                        container.clipboardLink.value = text
                        if (container.settings.state.value.notifications) {
                            trayState.sendNotification(Notification("YouTube link copied", "Open Netly to download it.", Notification.Type.Info))
                        }
                    }
                }
            }
        }

        Tray(
            icon = TrayIcon,
            state = trayState,
            tooltip = AppInfo.NAME,
            onAction = { visible = true },
            menu = {
                Item("Open Netly", onClick = { visible = true })
                Item("Pause all", onClick = { container.manager.pauseAll() })
                Item("Resume all", onClick = { container.manager.resumeAll() })
                Separator()
                Item("Quit", onClick = { quit() })
            },
        )

        Window(
            visible = visible,
            onCloseRequest = { if (settings.minimizeToTray) visible = false else quit() },
            title = AppInfo.NAME,
            state = rememberWindowState(width = 1280.dp, height = 800.dp),
        ) {
            LaunchedEffect(Unit) { window.minimumSize = Dimension(1100, 700) }
            NetlyTheme(mode, settings.language) {
                AppShell(container, onThemeChange = { m -> container.settings.update { it.copy(theme = m.name) } })
            }
        }
    }
}
