package com.skt.netly.pc.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
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
import com.skt.netly.pc.data.update.UpdateCheck
import com.skt.netly.pc.platform.Links
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
private val cookieLabels = listOf("None", "Auto", "Chrome", "Edge", "Firefox")
private val cookieKeys = listOf("none", "auto", "chrome", "edge", "firefox")

@OptIn(ExperimentalLayoutApi::class)
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
    var appChecking by remember { mutableStateOf(false) }
    var appCheckResult by remember { mutableStateOf<String?>(null) }
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

        val left: @Composable () -> Unit = {
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
                    SwitchSetting("Autoplay the next item in the player", s.autoplayNext) { v -> change { it.copy(autoplayNext = v) } }
                    SwitchSetting("Start the player in music mode (audio only)", s.playerAudioOnly) { v -> change { it.copy(playerAudioOnly = v) } }
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
                    NeuText("Auto uses your browser's YouTube sign-in only when YouTube says \"confirm you are not a bot\". Firefox works best; for Chrome or Edge use a cookies file.", style = Neu.type.caption, color = c.textMuted)
                    Setting("Cookies file (cookies.txt)") {
                        NeuText(if (s.cookiesFile.isBlank()) "Not set" else s.cookiesFile, style = Neu.type.mono, color = c.textMuted, maxLines = 2, modifier = Modifier.weight(1f))
                        NeuTextButton("Choose", onClick = {
                            scope.launch {
                                val picked = withContext(Dispatchers.IO) { FileActions.chooseFile(s.cookiesFile, "Choose cookies.txt") }
                                if (picked != null) change { it.copy(cookiesFile = picked) }
                            }
                        })
                        if (s.cookiesFile.isNotBlank()) NeuTextButton("Clear", onClick = { change { it.copy(cookiesFile = "") } })
                    }
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
        val right: @Composable () -> Unit = {
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
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            if (maxWidth >= 1000.dp) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(20.dp)) { left() }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(20.dp)) { right() }
                }
            } else {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    left()
                    right()
                }
            }
        }
        Section("About Netly") {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NeuText("${AppInfo.NAME} for PC", style = Neu.type.title, modifier = Modifier.weight(1f))
                NeuText("Version ${AppInfo.VERSION}", style = Neu.type.mono, color = c.textMuted)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                NeuTextButton(if (appChecking) "Checking…" else "Check for Netly updates", enabled = !appChecking, onClick = {
                    appChecking = true
                    scope.launch {
                        appCheckResult = when (val r = container.checkAppUpdate()) {
                            is UpdateCheck.Available -> "Netly ${r.update.version} is available."
                            UpdateCheck.UpToDate -> "You have the latest version."
                            is UpdateCheck.Failed -> r.message
                        }
                        appChecking = false
                    }
                })
                appCheckResult?.let { NeuText(it, style = Neu.type.caption, color = c.textMuted, maxLines = 2, modifier = Modifier.weight(1f)) }
            }
            SwitchSetting("Check for Netly updates when the app starts", s.checkAppUpdates) { v -> change { it.copy(checkAppUpdates = v) } }
            NeuText(
                "Update checks only read GitHub's public release information. Nothing is installed without you.",
                style = Neu.type.caption,
                color = c.textMuted,
            )
        }
        Section("Developer") {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                NeuText(AppInfo.DEV_NAME, style = Neu.type.title)
                NeuText(AppInfo.DEV_ROLE, style = Neu.type.caption, color = c.textMuted)
            }
            LinkRow {
                NeuTextButton("Portfolio", onClick = { Links.open(AppInfo.DEV_PORTFOLIO) })
                NeuTextButton("GitHub", onClick = { Links.open(AppInfo.DEV_GITHUB) })
                NeuTextButton("Instagram", onClick = { Links.open(AppInfo.DEV_INSTAGRAM) })
                NeuTextButton("Email", onClick = { Links.feedbackMail() })
            }
            NeuText(AppInfo.DEV_EMAIL, style = Neu.type.mono, color = c.textMuted, maxLines = 1)
        }
        Section("Help and feedback") {
            NeuText(
                "Found a bug or have an idea? Tell me. Please say what you did, what you expected and what happened.",
                style = Neu.type.caption,
                color = c.textMuted,
            )
            LinkRow {
                NeuTextButton("Report a problem", primary = true, onClick = { Links.open(AppInfo.ISSUES_URL) })
                NeuTextButton("Send feedback by email", onClick = { Links.feedbackMail() })
                NeuTextButton("Star on GitHub", onClick = { Links.open(AppInfo.REPO_URL) })
            }
            LinkRow {
                NeuTextButton("Privacy", onClick = { Links.open(AppInfo.PRIVACY_URL) })
                NeuTextButton("Open-source licenses", onClick = { Links.open(AppInfo.LICENSES_URL) })
                NeuTextButton("Security", onClick = { Links.open(AppInfo.SECURITY_URL) })
            }
            NeuText(
                "Privacy: Netly has no account, no ads and no tracking. Your settings, downloads list and library stay on this PC. " +
                    "The internet is used only to talk to YouTube (and GitHub for update checks).",
                style = Neu.type.caption,
                color = c.textMuted,
            )
            NeuText(
                "Netly is not affiliated with YouTube or Google. Download only content you own or have permission to save.",
                style = Neu.type.caption,
                color = c.textMuted,
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LinkRow(content: @Composable () -> Unit) {
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { content() }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Setting(label: String, control: @Composable FlowRowScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        NeuText(label, style = Neu.type.label)
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = control)
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
