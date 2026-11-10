package com.skt.netly.pc.ui.shell

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

@Composable
fun Sidebar(
    current: Screen,
    onNavigate: (Screen) -> Unit,
    onThemeChange: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Neu.colors
    NeuSurface(modifier = modifier, radius = 28.dp) {
        Column(
            modifier = Modifier.fillMaxHeight().padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.padding(start = 8.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier.size(34.dp).neuRaised(c, 11.dp, NeuElevation.Small),
                    contentAlignment = Alignment.Center,
                ) {
                    NeuIcon(NeuIcons.Download, c.accentText, size = 18.dp, strokeWidth = 2.4f)
                }
                NeuText("Netly", style = Neu.type.title)
            }

            Screen.entries.forEach { screen ->
                NavItem(screen = screen, selected = screen == current, onClick = { onNavigate(screen) })
            }

            Spacer(Modifier.weight(1f))

            StorageCard()

            NeuSegmented(
                options = listOf("Light", "Dark"),
                selectedIndex = if (c.isDark) 1 else 0,
                onSelect = { onThemeChange(if (it == 0) ThemeMode.Light else ThemeMode.Dark) },
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            )
        }
    }
}

@Composable
private fun NavItem(screen: Screen, selected: Boolean, onClick: () -> Unit) {
    val c = Neu.colors
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val color = if (selected) c.accentText else c.textMuted
    val shape = RoundedCornerShape(16.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .then(if (selected) Modifier.neuInset(c, 16.dp) else Modifier)
            .then(if (focused) Modifier.border(2.dp, c.accentText, shape) else Modifier)
            .clickable(interactionSource = interaction, indication = null, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        NeuIcon(screen.icon, color)
        NeuText(screen.label, style = Neu.type.label.copy(fontSize = 15.sp), color = color)
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
