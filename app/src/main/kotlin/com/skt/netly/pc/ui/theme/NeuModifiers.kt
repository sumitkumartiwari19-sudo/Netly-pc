package com.skt.netly.pc.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.asFrameworkPaint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jetbrains.skia.FilterBlurMode
import org.jetbrains.skia.MaskFilter

data class NeuShadow(val offset: Dp, val blur: Dp)

object NeuElevation {
    val Large = NeuShadow(8.dp, 18.dp)
    val Medium = NeuShadow(5.dp, 10.dp)
    val Small = NeuShadow(3.dp, 6.dp)
    val InsetMedium = NeuShadow(5.dp, 10.dp)
    val InsetSmall = NeuShadow(4.dp, 8.dp)
}

private fun blurPaint(color: Color, sigma: Float): Paint = Paint().apply {
    this.color = color
    isAntiAlias = true
    asFrameworkPaint().maskFilter = MaskFilter.makeBlur(FilterBlurMode.NORMAL, sigma)
}

private fun fillPaint(color: Color): Paint = Paint().apply {
    this.color = color
    isAntiAlias = true
}

/** Surface raised from the background: dark shadow bottom-right, light highlight top-left. */
fun Modifier.neuRaised(
    colors: NeuColors,
    radius: Dp,
    shadow: NeuShadow = NeuElevation.Large,
    fill: Color = colors.bg,
): Modifier = drawBehind {
    val r = radius.toPx()
    val o = shadow.offset.toPx()
    val sigma = shadow.blur.toPx() / 2f
    val w = size.width
    val h = size.height
    val dark = blurPaint(colors.shadowDark, sigma)
    val light = blurPaint(colors.shadowLight, sigma)
    val body = fillPaint(fill)
    drawIntoCanvas { canvas ->
        canvas.drawRoundRect(o, o, w + o, h + o, r, r, dark)
        canvas.drawRoundRect(-o, -o, w - o, h - o, r, r, light)
        canvas.drawRoundRect(0f, 0f, w, h, r, r, body)
    }
}

/** Surface pressed into the background: dark shadow inside top-left, light inside bottom-right. */
fun Modifier.neuInset(
    colors: NeuColors,
    radius: Dp,
    shadow: NeuShadow = NeuElevation.InsetMedium,
): Modifier = drawBehind {
    val r = radius.toPx()
    val o = shadow.offset.toPx()
    val sigma = shadow.blur.toPx() / 2f
    val w = size.width
    val h = size.height
    val dark = blurPaint(colors.shadowDark, sigma)
    val light = blurPaint(colors.shadowLight, sigma)
    val base = fillPaint(colors.bg)
    val shape = Path().apply { addRoundRect(RoundRect(0f, 0f, w, h, r, r)) }
    val outer = Path().apply { addRect(Rect(-w, -h, 2f * w, 2f * h)) }
    val ring = Path.combine(PathOperation.Difference, outer, shape)
    drawIntoCanvas { canvas ->
        canvas.save()
        canvas.clipPath(shape, ClipOp.Intersect)
        canvas.drawPath(shape, base)
        canvas.save()
        canvas.translate(o, o)
        canvas.drawPath(ring, dark)
        canvas.restore()
        canvas.save()
        canvas.translate(-o, -o)
        canvas.drawPath(ring, light)
        canvas.restore()
        canvas.restore()
    }
}
