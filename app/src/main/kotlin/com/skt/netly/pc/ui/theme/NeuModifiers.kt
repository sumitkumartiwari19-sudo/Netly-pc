package com.skt.netly.pc.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jetbrains.skia.ClipMode
import org.jetbrains.skia.FilterBlurMode
import org.jetbrains.skia.MaskFilter
import org.jetbrains.skia.Paint
import org.jetbrains.skia.PaintMode
import kotlin.math.ceil
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

/** When true, surfaces are drawn flat (no blurred shadows). Meant for older PCs; set from Settings. */
object NeuPerf {
    var reduced by mutableStateOf(false)
}

private const val MAX_CACHED_PIXELS = 12_000_000L

private fun DrawScope.flatSurface(colors: NeuColors, fill: Color, radius: Float, raised: Boolean) {
    val corner = CornerRadius(radius)
    drawRoundRect(if (raised) fill else colors.bg, cornerRadius = corner)
    if (!raised) drawRoundRect(colors.shadowDark.copy(alpha = 0.22f), cornerRadius = corner)
    drawRoundRect(colors.shadowDark.copy(alpha = if (raised) 0.45f else 0.6f), cornerRadius = corner, style = Stroke(1f))
}

/**
 * Surface raised from the background: dark shadow bottom-right, light highlight top-left.
 * The blurred shadows are drawn once into a bitmap and reused on every frame, which keeps the UI smooth on slow PCs.
 */
fun Modifier.neuRaised(
    colors: NeuColors,
    radius: Dp,
    shadow: NeuShadow = NeuElevation.Large,
    fill: Color = colors.bg,
): Modifier = drawWithCache {
    val r = radius.toPx()
    val o = shadow.offset.toPx()
    val sigma = shadow.blur.toPx() / 2f
    val w = size.width
    val h = size.height
    val pad = ceil(o + sigma * 3f).toInt()
    val bw = ceil(w).toInt() + pad * 2
    val bh = ceil(h).toInt() + pad * 2
    if (NeuPerf.reduced || w < 1f || h < 1f || bw.toLong() * bh > MAX_CACHED_PIXELS) {
        onDrawBehind { flatSurface(colors, fill, r, raised = true) }
    } else {
        val bitmap = ImageBitmap(bw, bh)
        val nc = Canvas(bitmap).nativeCanvas
        nc.translate(pad.toFloat(), pad.toFloat())
        nc.drawRRect(RRect.makeXYWH(o, o, w, h, r), blurPaint(colors.shadowDark, sigma))
        nc.drawRRect(RRect.makeXYWH(-o, -o, w, h, r), blurPaint(colors.shadowLight, sigma))
        nc.drawRRect(RRect.makeXYWH(0f, 0f, w, h, r), fillPaint(fill))
        onDrawBehind { drawImage(bitmap, topLeft = Offset(-pad.toFloat(), -pad.toFloat())) }
    }
}

/** Surface pressed into the background: dark shadow inside top-left, light inside bottom-right. Cached like [neuRaised]. */
fun Modifier.neuInset(
    colors: NeuColors,
    radius: Dp,
    shadow: NeuShadow = NeuElevation.InsetMedium,
): Modifier = drawWithCache {
    val r = radius.toPx()
    val o = shadow.offset.toPx()
    val sigma = shadow.blur.toPx() / 2f
    val w = size.width
    val h = size.height
    val bw = ceil(w).toInt()
    val bh = ceil(h).toInt()
    if (NeuPerf.reduced || w < 1f || h < 1f || bw.toLong() * bh > MAX_CACHED_PIXELS) {
        onDrawBehind { flatSurface(colors, colors.bg, r, raised = false) }
    } else {
        // A thick outline hugging the outside of the shape; shifted and blurred, its inner edge becomes the inset shadow.
        val band = maxOf(sigma * 6f, 4f)
        val half = band / 2f
        val ring = RRect.makeXYWH(-half, -half, w + band, h + band, r + half)
        val shape = RRect.makeXYWH(0f, 0f, w, h, r)
        val bitmap = ImageBitmap(bw, bh)
        val nc = Canvas(bitmap).nativeCanvas
        nc.clipRRect(shape, ClipMode.INTERSECT, true)
        nc.drawRRect(shape, fillPaint(colors.bg))
        nc.save()
        nc.translate(o, o)
        nc.drawRRect(ring, blurPaint(colors.shadowDark, sigma, band))
        nc.restore()
        nc.save()
        nc.translate(-o, -o)
        nc.drawRRect(ring, blurPaint(colors.shadowLight, sigma, band))
        nc.restore()
        onDrawBehind { drawImage(bitmap) }
    }
}
