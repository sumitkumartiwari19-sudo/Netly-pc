package com.skt.netly.pc.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.skt.netly.pc.ui.theme.Neu
import com.skt.netly.pc.ui.theme.NeuElevation
import com.skt.netly.pc.ui.theme.neuInset
import com.skt.netly.pc.ui.theme.neuRaised

/**
 * Raised button that sinks in when pressed or selected.
 * primary = filled with the accent colour (use once per area).
 */
@Composable
fun NeuButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    selected: Boolean = false,
    enabled: Boolean = true,
    radius: Dp = 16.dp,
    minHeight: Dp = 44.dp,
    contentDescription: String? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp),
    content: @Composable RowScope.() -> Unit,
) {
    val c = Neu.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(radius)

    val look = when {
        primary -> Modifier.neuRaised(
            c, radius, NeuElevation.Medium,
            fill = if (pressed) c.accent.copy(alpha = 0.85f) else c.accent,
        )
        pressed || selected -> Modifier.neuInset(c, radius, NeuElevation.InsetSmall)
        else -> Modifier.neuRaised(c, radius, NeuElevation.Medium)
    }

    Row(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.45f)
            .defaultMinSize(minHeight = minHeight)
            .then(look)
            .then(if (focused) Modifier.border(2.dp, c.accentText, shape) else Modifier)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .then(
                if (contentDescription != null) {
                    Modifier.semantics { this.contentDescription = contentDescription }
                } else {
                    Modifier
                }
            )
            .padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
fun NeuTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    selected: Boolean = false,
    enabled: Boolean = true,
    radius: Dp = 16.dp,
    minHeight: Dp = 44.dp,
) {
    val c = Neu.colors
    val color = when {
        primary -> c.onAccent
        selected -> c.accentText
        else -> c.text
    }
    NeuButton(
        onClick = onClick,
        modifier = modifier,
        primary = primary,
        selected = selected,
        enabled = enabled,
        radius = radius,
        minHeight = minHeight,
    ) {
        NeuText(text = text, style = Neu.type.body.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), color = color)
    }
}
