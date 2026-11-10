package com.skt.netly.pc.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.skt.netly.pc.ui.components.NeuSegmented
import com.skt.netly.pc.ui.components.NeuSurface
import com.skt.netly.pc.ui.components.NeuSwitch
import com.skt.netly.pc.ui.components.NeuText
import com.skt.netly.pc.ui.components.NeuTextButton
import com.skt.netly.pc.ui.components.NeuProgress
import com.skt.netly.pc.ui.theme.Neu

/** Temporary screen for M1: shows the title and a live preview of the shared components. */
@Composable
fun PlaceholderScreen(title: String, subtitle: String, milestone: String) {
    val c = Neu.colors
    var segment by remember { mutableStateOf(0) }
    var switchOn by remember { mutableStateOf(true) }
    var clicks by remember { mutableStateOf(0) }

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Column {
            NeuText(title, style = Neu.type.display)
            NeuText(
                subtitle,
                modifier = Modifier.padding(top = 6.dp),
                style = Neu.type.body.copy(fontWeight = FontWeight.Medium),
                color = c.textMuted,
            )
        }

        NeuSurface(modifier = Modifier.fillMaxWidth(), radius = 28.dp) {
            Column(Modifier.padding(28.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
                NeuText("Coming in $milestone", style = Neu.type.title)
                NeuText("Component preview (temporary)", style = Neu.type.overline, color = c.textMuted)

                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    NeuTextButton("Primary", onClick = { clicks++ }, primary = true, minHeight = 52.dp, radius = 18.dp)
                    NeuTextButton("Default", onClick = { clicks++ })
                    NeuTextButton("Selected", onClick = { clicks++ }, selected = true)
                    NeuText("Clicks: $clicks", style = Neu.type.mono, color = c.textMuted)
                }

                NeuSegmented(
                    options = listOf("Video", "Audio only"),
                    selectedIndex = segment,
                    onSelect = { segment = it },
                    modifier = Modifier.width(320.dp),
                )

                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    NeuSwitch(checked = switchOn, onCheckedChange = { switchOn = it }, contentDescription = "Demo switch")
                    NeuText(if (switchOn) "Switch is on" else "Switch is off", style = Neu.type.label)
                }

                NeuProgress(progress = 0.68f, modifier = Modifier.width(320.dp))
            }
        }
    }
}
