package com.skt.netly.pc.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jetbrains.skia.ClipMode
import org.jetbrains.skia.FilterBlurMode
import org.jetbrains.skia.MaskFilter
import org.jetbrains.skia.Paint
import org.jetbrains.skia.PaintMode
import org.jetbrains.skia.RRect

data class NeuShadow(val offset: Dp, val blur: Dp)

object NeuElevation {
    val Large = NeuShadow(8.dp, 18.dp)
    val Medium = NeuShadow(5.dp, 10.dp)
    val Small = NeuShadow(3.dp, 6.dp)
    val InsetMedium = NeuShadow(5.dp, 10.dp)
    val InsetSmall = NeuShadow(4.dp, 8.dp)
}

private fun blurPaint(color: Color, sigma: Float, strokeWidth: Float = 0f): Paint = Paint().apply {
    this.color = color.toArgb()
    isAntiAlias = true
    if (strokeWidth > 0f) {
        mode = PaintMode.STROKE
        this.strokeWidth = strokeWidth
    }
    maskFilter = MaskFilter.makeBlur(FilterBlurMode.NORMAL, sigma)
}

private fun fillPaint(color: Color): Paint = Paint().apply {
    this.color = color.toArgb()
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
        val nc = canvas.nativeCanvas
        nc.drawRRect(RRect.makeXYWH(o, o, w, h, r), dark)
        nc.drawRRect(RRect.makeXYWH(-o, -o, w, h, r), light)
        nc.drawRRect(RRect.makeXYWH(0f, 0f, w, h, r), body)
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
    // A thick outline hugging the outside of the shape; shifted and blurred, its inner edge becomes the inset shadow.
    val band = maxOf(sigma * 6f, 4f)
    val half = band / 2f
    val ring = RRect.makeXYWH(-half, -half, w + band, h + band, r + half)
    val shape = RRect.makeXYWH(0f, 0f, w, h, r)
    val dark = blurPaint(colors.shadowDark, sigma, band)
    val light = blurPaint(colors.shadowLight, sigma, band)
    val base = fillPaint(colors.bg)
    drawIntoCanvas { canvas ->
        val nc = canvas.nativeCanvas
        nc.save()
        nc.clipRRect(shape, ClipMode.INTERSECT, true)
        nc.drawRRect(shape, base)
        nc.save()
        nc.translate(o, o)
        nc.drawRRect(ring, dark)
        nc.restore()
        nc.save()
        nc.translate(-o, -o)
        nc.drawRRect(ring, light)
        nc.restore()
        nc.restore()
    }
}
