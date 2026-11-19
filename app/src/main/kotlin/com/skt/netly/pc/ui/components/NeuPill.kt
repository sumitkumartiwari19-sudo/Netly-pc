package com.skt.netly.pc.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skt.netly.pc.ui.theme.Neu
import com.skt.netly.pc.ui.theme.NeuElevation
import com.skt.netly.pc.ui.theme.neuInset
import com.skt.netly.pc.ui.theme.neuRaised

/** Small label chip. inset = pressed-in (info), otherwise raised. */
@Composable
fun NeuPill(
    text: String,
    modifier: Modifier = Modifier,
    inset: Boolean = true,
    color: Color = Neu.colors.text,
) {
    val c = Neu.colors
    val look = if (inset) {
        Modifier.neuInset(c, 14.dp, NeuElevation.InsetSmall)
    } else {
        Modifier.neuRaised(c, 14.dp, NeuElevation.Small)
    }
    NeuText(
        text = text,
        modifier = modifier.then(look).padding(horizontal = 14.dp, vertical = 7.dp),
        style = Neu.type.caption.copy(fontSize = 13.sp),
        color = color,
        maxLines = 1,
    )
}
