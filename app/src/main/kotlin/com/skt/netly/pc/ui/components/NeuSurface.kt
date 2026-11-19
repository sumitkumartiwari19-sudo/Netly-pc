package com.skt.netly.pc.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.skt.netly.pc.ui.theme.Neu
import com.skt.netly.pc.ui.theme.NeuElevation
import com.skt.netly.pc.ui.theme.neuInset
import com.skt.netly.pc.ui.theme.neuRaised

enum class NeuLevel { Raised, RaisedMedium, Inset }

@Composable
fun NeuSurface(
    modifier: Modifier = Modifier,
    level: NeuLevel = NeuLevel.Raised,
    radius: Dp = 22.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val c = Neu.colors
    val look = when (level) {
        NeuLevel.Raised -> Modifier.neuRaised(c, radius)
        NeuLevel.RaisedMedium -> Modifier.neuRaised(c, radius, NeuElevation.Medium)
        NeuLevel.Inset -> Modifier.neuInset(c, radius)
    }
    Box(modifier = modifier.then(look), content = content)
}
