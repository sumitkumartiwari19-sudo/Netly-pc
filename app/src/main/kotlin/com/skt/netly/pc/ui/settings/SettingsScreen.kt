package com.skt.netly.pc.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.skt.netly.pc.AppContainer
import com.skt.netly.pc.AppInfo
import com.skt.netly.pc.data.engine.ConnectionTester
import com.skt.netly.pc.data.engine.EngineStatus
import com.skt.netly.pc.data.settings.AppSettings
import com.skt.netly.pc.platform.FileActions
import com.skt.netly.pc.platform.StartupRegistry
import com.skt.netly.pc.ui.components.NeuLevel
import com.skt.netly.pc.ui.components.NeuSegmented
import com.skt.netly.pc.ui.components.NeuSurface
import com.skt.netly.pc.ui.components.NeuSwitch
import com.skt.netly.pc.ui.components.NeuText
import com.skt.netly.pc.ui.components.NeuTextButton
import com.skt.netly.pc.ui.theme.Neu
import com.skt.netly.pc.ui.theme.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val themeKeys = listOf("System", "Light", "Dark")
private val heightLabels = listOf("Best", "1080p", "720p", "480p")
private val heightValues = listOf(0, 1080, 720, 480)
private val limitLabels = listOf("Unlimited", "1 MB/s", "2 MB/s", "5 MB/s")
private val limitValues = listOf(0, 1024, 2048, 5120)
private val nameLabels = listOf("Title", "Channel – Title", "Title [id]")
private val nameKeys = listOf("title", "channel_title", "title_id")
private val cookieLabels = listOf("None", "Chrome", "Edge", "Firefox")
private val cookieKeys = listOf("none", "chrome", "edge", "firefox")

@Composable
fun SettingsScreen(container: AppContainer, onThemeChange: (ThemeMode) -> Unit) {
    val c = Neu.colors
    val s by container.settings.state.collectAsState()
    val engine by container.engine.status.collectAsState()
    val scope = rememberCoroutineScope()
    var testResult by remember { mutableStateOf<String?>(null) }
    var testing by remember { mutableStateOf(false) }
    var updateResult by remember { mutableStateOf<String?>(null) }
    var updating by remember { mutableStateOf(false) }
    fun change(f: (AppSettings) -> AppSettings) = container.settings.update(f)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Column {
            NeuText("Settings", style = Neu.type.display)
            NeuText(
                "Make Netly work the way you like.",
                modifier = Modifier.padding(top = 6.dp),
                style = Neu.type.body.copy(fontWeight = FontWeight.Medium),
                color = c.textMuted,
            )
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Section("General") {
                    Setting("Language") {
                        NeuSegmented(listOf("English", "हिन्दी"), if (s.language == "hi") 1 else 0, { change { st -> st.copy(language = if (it == 1) "hi" else "en") } })
                    }
                    Setting("Theme") {
                        NeuSegmented(themeKeys, themeKeys.indexOf(s.theme).coerceAtLeast(0), {
                            change { st -> st.copy(theme = themeKeys[it]) }
                            onThemeChange(ThemeMode.valueOf(themeKeys[it]))
                        })
                    }
                    SwitchSetting("Watch the clipboard for YouTube links", s.clipboardWatcher) { v -> change { it.copy(clipboardWatcher = v) } }
                    SwitchSetting("Smooth mode (fewer effects, for older PCs)", s.reducedEffects) { v -> change { it.copy(reducedEffects = v) } }
                    SwitchSetting("Show notifications", s.notifications) { v -> change { it.copy(notifications = v) } }
                    SwitchSetting("Keep running in the tray when closed", s.minimizeToTray) { v -> change { it.copy(minimizeToTray = v) } }
                    SwitchSetting("Start with Windows", s.startWithWindows) { v ->
                        change { it.copy(startWithWindows = v) }
                        StartupRegistry.set(v)
                    }
                }
                Section("YouTube access") {
                    Setting("Use cookies from browser") {
                        NeuSegmented(cookieLabels, cookieKeys.indexOf(s.cookiesBrowser).coerceAtLeast(0), { change { st -> st.copy(cookiesBrowser = cookieKeys[it]) } })
                    }
                    NeuText("Pick the browser where you are signed in to YouTube if a video asks you to sign in.", style = Neu.type.caption, color = c.textMuted)
                    Setting("Proxy") {
                        TextBox(s.proxy, "http://host:port (optional)", Modifier.weight(1f)) { v -> change { it.copy(proxy = v) } }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        NeuTextButton(if (testing) "Testing…" else "Test connection", enabled = !testing, onClick = {
                            testing = true
                            scope.launch {
                                testResult = ConnectionTester.test(s.proxy)
                                testing = false
                            }
                        })
                        testResult?.let { NeuText(it, style = Neu.type.caption, color = c.textMuted, maxLines = 2, modifier = Modifier.weight(1f)) }
                    }
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Section("Downloads") {
                    Setting("Folder") {
                        NeuText(s.downloadDir, style = Neu.type.mono, color = c.textMuted, maxLines = 2, modifier = Modifier.weight(1f))
                        NeuTextButton("Change", onClick = {
                            scope.launch {
                                val picked = withContext(Dispatchers.IO) { FileActions.chooseFolder(s.downloadDir) }
                                if (picked != null) change { it.copy(downloadDir = picked) }
                            }
                        })
                    }
                    Setting("Default quality") {
                        NeuSegmented(heightLabels, heightValues.indexOf(s.defaultHeight).coerceAtLeast(1), { change { st -> st.copy(defaultHeight = heightValues[it]) } })
                    }
                    Setting("At the same time") {
                        NeuSegmented(listOf("1", "2", "3", "4"), (s.maxConcurrent - 1).coerceIn(0, 3), { change { st -> st.copy(maxConcurrent = it + 1) } })
                    }
                    Setting("Speed limit") {
                        NeuSegmented(limitLabels, limitValues.indexOf(s.speedLimitKb).coerceAtLeast(0), { change { st -> st.copy(speedLimitKb = limitValues[it]) } })
                    }
                    Setting("File name") {
                        NeuSegmented(nameLabels, nameKeys.indexOf(s.nameTemplate).coerceAtLeast(0), { change { st -> st.copy(nameTemplate = nameKeys[it]) } })
                    }
                }
                Section("Engine and about") {
                    Setting("yt-dlp") {
                        NeuText(
                            when (val e = engine) {
                                EngineStatus.Preparing -> "Starting…"
                                is EngineStatus.Ready -> "Version ${e.version}"
                                is EngineStatus.Failed -> e.message
                            },
                            style = Neu.type.mono,
                            color = c.textMuted,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        NeuTextButton(if (updating) "Updating…" else "Check for updates", enabled = !updating, onClick = {
                            updating = true
                            scope.launch {
                                updateResult = container.updater.update().message
                                updating = false
                            }
                        })
                        updateResult?.let { NeuText(it, style = Neu.type.caption, color = c.textMuted, maxLines = 3, modifier = Modifier.weight(1f)) }
                    }
                    SwitchSetting("Update the engine automatically", s.autoUpdateEngine) { v -> change { it.copy(autoUpdateEngine = v) } }
                    NeuText(
                        "${AppInfo.NAME} downloads only what you ask it to. Save content only when you have the right to.",
                        style = Neu.type.caption,
                        color = c.textMuted,
                    )
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    NeuSurface(Modifier.fillMaxWidth(), radius = 24.dp) {
        Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            NeuText(title.uppercase(), style = Neu.type.overline, color = Neu.colors.textMuted)
            content()
        }
    }
}

@Composable
private fun Setting(label: String, control: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        NeuText(label, style = Neu.type.label)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically, content = control)
    }
}

@Composable
private fun SwitchSetting(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        NeuText(label, style = Neu.type.label, modifier = Modifier.weight(1f))
        NeuSwitch(checked, onChange, contentDescription = label)
    }
}

@Composable
private fun TextBox(value: String, hint: String, modifier: Modifier, onChange: (String) -> Unit) {
    val c = Neu.colors
    NeuSurface(modifier, level = NeuLevel.Inset, radius = 14.dp) {
        Box(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) NeuText(hint, style = Neu.type.caption, color = c.textMuted)
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = Neu.type.body.copy(color = c.text),
                cursorBrush = SolidColor(c.accentText),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
