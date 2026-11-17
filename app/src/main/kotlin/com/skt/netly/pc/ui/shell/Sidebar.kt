package com.skt.netly.pc.ui.shell

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.skt.netly.pc.ui.components.NeuButton
import com.skt.netly.pc.ui.i18n.LocalLanguage
import com.skt.netly.pc.ui.i18n.Strings
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skt.netly.pc.ui.components.NeuIcon
import com.skt.netly.pc.ui.components.NeuIcons
import com.skt.netly.pc.ui.components.NeuLevel
import com.skt.netly.pc.ui.components.NeuProgress
import com.skt.netly.pc.ui.components.NeuSegmented
import com.skt.netly.pc.ui.components.NeuSurface
import com.skt.netly.pc.ui.components.NeuText
import com.skt.netly.pc.ui.theme.Neu
import com.skt.netly.pc.ui.theme.NeuElevation
import com.skt.netly.pc.ui.theme.ThemeMode
import com.skt.netly.pc.ui.theme.neuInset
import com.skt.netly.pc.ui.theme.neuRaised
import java.io.File

private const val ChevronLeft = "M15 6l-6 6 6 6"
private const val ChevronRight = "M9 6l6 6-6 6"
private const val SunPath = "M12 8a4 4 0 1 0 0 8a4 4 0 0 0 0-8zM12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4"
private const val MoonPath = "M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z"

/** Left menu. [collapsed] shows icons only. The menu list scrolls when the window is short. */
@Composable
fun Sidebar(
    current: Screen,
    collapsed: Boolean,
    onToggleCollapsed: () -> Unit,
    onNavigate: (Screen) -> Unit,
    onThemeChange: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Neu.colors
    NeuSurface(modifier = modifier, radius = 28.dp) {
        Column(
            modifier = Modifier.fillMaxHeight().padding(horizontal = if (collapsed) 10.dp else 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = if (collapsed) Alignment.CenterHorizontally else Alignment.Start,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = if (collapsed) Arrangement.Center else Arrangement.spacedBy(10.dp),
                ) {
                    if (!collapsed) {
                        Box(
                            modifier = Modifier.size(34.dp).neuRaised(c, 11.dp, NeuElevation.Small),
                            contentAlignment = Alignment.Center,
                        ) {
                            NeuIcon(NeuIcons.Download, c.accentText, size = 18.dp, strokeWidth = 2.4f)
                        }
                        NeuText("Netly", style = Neu.type.title, modifier = Modifier.weight(1f), maxLines = 1)
                    }
                    NeuButton(
                        onClick = onToggleCollapsed,
                        radius = 12.dp,
                        minHeight = 34.dp,
                        contentDescription = if (collapsed) "Expand menu" else "Collapse menu",
                        contentPadding = PaddingValues(horizontal = 6.dp),
                        modifier = Modifier.width(34.dp),
                    ) {
                        NeuIcon(if (collapsed) ChevronRight else ChevronLeft, c.text, size = 18.dp, strokeWidth = 2.2f)
                    }
                }

                Screen.entries.forEach { screen ->
                    NavItem(screen = screen, collapsed = collapsed, selected = screen == current, onClick = { onNavigate(screen) })
                }
                if (!collapsed) {
                    Spacer(Modifier.height(8.dp))
                    StorageCard()
                }
            }

            if (collapsed) {
                NeuButton(
                    onClick = { onThemeChange(if (c.isDark) ThemeMode.Light else ThemeMode.Dark) },
                    radius = 14.dp,
                    minHeight = 44.dp,
                    contentDescription = if (c.isDark) "Switch to light theme" else "Switch to dark theme",
                    contentPadding = PaddingValues(horizontal = 0.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    NeuIcon(if (c.isDark) SunPath else MoonPath, c.text, size = 20.dp)
                }
            } else {
                NeuSegmented(
                    options = listOf("Light", "Dark"),
                    selectedIndex = if (c.isDark) 1 else 0,
                    onSelect = { onThemeChange(if (it == 0) ThemeMode.Light else ThemeMode.Dark) },
                    modifier = Modifier.fillMaxWidth(),
                    equalWidth = true,
                )
            }
        }
    }
}

@Composable
private fun NavItem(screen: Screen, collapsed: Boolean, selected: Boolean, onClick: () -> Unit) {
    val c = Neu.colors
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val color = if (selected) c.accentText else c.textMuted
    val shape = RoundedCornerShape(16.dp)
    val label = Strings.tr(LocalLanguage.current, screen.label)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .then(if (selected) Modifier.neuInset(c, 16.dp) else Modifier)
            .then(if (focused) Modifier.border(2.dp, c.accentText, shape) else Modifier)
            .clickable(interactionSource = interaction, indication = null, role = Role.Tab, onClick = onClick)
            .semantics { contentDescription = label }
            .padding(horizontal = if (collapsed) 0.dp else 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (collapsed) Arrangement.Center else Arrangement.spacedBy(12.dp),
    ) {
        NeuIcon(screen.icon, color)
        if (!collapsed) NeuText(screen.label, style = Neu.type.label.copy(fontSize = 15.sp), color = color, maxLines = 1)
    }
}

@Composable
private fun StorageCard() {
    val c = Neu.colors
    val (freeText, usedFraction) = remember { storageInfo() }
    NeuSurface(modifier = Modifier.fillMaxWidth(), level = NeuLevel.Inset, radius = 20.dp) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            NeuText("Saving to", style = Neu.type.caption, color = c.textMuted)
            NeuText("Downloads\\Netly", style = Neu.type.mono.copy(fontSize = 12.sp), maxLines = 1)
            NeuProgress(progress = usedFraction, modifier = Modifier.fillMaxWidth())
            NeuText(freeText, style = Neu.type.caption, color = c.textMuted)
        }
    }
}

private fun storageInfo(): Pair<String, Float> {
    val root = File(System.getProperty("user.home") ?: ".")
    val total = root.totalSpace
    val free = root.usableSpace
    if (total <= 0L) return "Free space unknown" to 0f
    val freeGb = free / 1_073_741_824.0
    return "%.0f GB free".format(freeGb) to (1f - free.toFloat() / total.toFloat())
}
