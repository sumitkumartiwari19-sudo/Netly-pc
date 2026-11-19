package com.skt.netly.pc.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.skt.netly.pc.ui.theme.Neu
import com.skt.netly.pc.ui.theme.NeuElevation
import com.skt.netly.pc.ui.theme.neuInset

/** Pressed-in track with an accent fill. progress is 0f..1f. */
@Composable
fun NeuProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = Neu.colors.accent,
) {
    val c = Neu.colors
    val shape = RoundedCornerShape(5.dp)
    Box(
        modifier = modifier
            .height(10.dp)
            .neuInset(c, 5.dp, NeuElevation.InsetSmall)
            .clip(shape),
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .background(color, shape),
        )
    }
}
