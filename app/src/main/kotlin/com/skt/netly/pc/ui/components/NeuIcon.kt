package com.skt.netly.pc.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Stroke icons on a 24x24 grid (same set as the approved design). */
object NeuIcons {
    const val Home = "M3 11l9-8 9 8v10a1 1 0 0 1-1 1h-5v-7H9v7H4a1 1 0 0 1-1-1z"
    const val Download = "M12 3v12m0 0l-4-4m4 4l4-4M4 20h16"
    const val Folder = "M3 7a2 2 0 0 1 2-2h4l2 2h8a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"
    const val Sliders = "M4 7h9M17 7h3M13 7a2 2 0 1 0 4 0a2 2 0 1 0-4 0M4 17h3M11 17h9M7 17a2 2 0 1 0 4 0a2 2 0 1 0-4 0"
}

@Composable
fun NeuIcon(
    pathData: String,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    strokeWidth: Float = 1.8f,
) {
    val path = remember(pathData) { PathParser().parsePathString(pathData).toPath() }
    Canvas(modifier.size(size)) {
        val s = this.size.width / 24f
        scale(s, s, pivot = Offset.Zero) {
            drawPath(
                path = path,
                color = tint,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}
