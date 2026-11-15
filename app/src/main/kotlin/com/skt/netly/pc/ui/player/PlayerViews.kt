package com.skt.netly.pc.ui.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.toPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.skt.netly.pc.player.PlayItem
import com.skt.netly.pc.player.PlayStatus
import com.skt.netly.pc.player.PlayerChoices
import com.skt.netly.pc.player.PlayerEngine
import com.skt.netly.pc.player.PlayerState
import com.skt.netly.pc.ui.components.NetThumbnail
import com.skt.netly.pc.ui.components.NeuButton
import com.skt.netly.pc.ui.components.NeuIcon
import com.skt.netly.pc.ui.components.NeuLevel
import com.skt.netly.pc.ui.components.NeuProgress
import com.skt.netly.pc.ui.components.NeuSegmented
import com.skt.netly.pc.ui.components.NeuSurface
import com.skt.netly.pc.ui.components.NeuText
import com.skt.netly.pc.ui.components.NeuTextButton
import com.skt.netly.pc.ui.theme.Neu
import com.skt.netly.pc.util.Formatters

private object G {
    const val Play = "M7 4l13 8-13 8z"
    const val Pause = "M6 4h4v16H6zM14 4h4v16h-4z"
    const val Prev = "M6 5h2v14H6zM20 5v14L9 12z"
    const val Next = "M16 5h2v14h-2zM4 5v14l11-7z"
    const val Headphones = "M4 14v-2a8 8 0 0 1 16 0v2M4 14h3v6H4zM17 14h3v6h-3z"
    const val Expand = "M15 3h6v6M9 21H3v-6M21 3l-7 7M3 21l7-7"
    const val Close = "M6 6l12 12M18 6L6 18"
    const val Volume = "M11 5L6 9H2v6h4l5 4zM15.5 8.5a5 5 0 0 1 0 7"
}

/** Solid (filled) icon on a 24x24 grid. */
@Composable
private fun Glyph(pathData: String, tint: Color, size: Dp = 20.dp) {
    val path = remember(pathData) { PathParser().parsePathString(pathData).toPath() }
    Canvas(Modifier.size(size)) {
        val s = this.size.width / 24f
        scale(s, s, pivot = Offset.Zero) { drawPath(path, tint) }
    }
}

@Composable
private fun RoundButton(
    path: String,
    description: String,
    onClick: () -> Unit,
    size: Dp,
    iconSize: Dp,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    enabled: Boolean = true,
) {
    val c = Neu.colors
    NeuButton(
        onClick = onClick,
        modifier = modifier.size(size),
        primary = primary,
        enabled = enabled,
        radius = size / 2,
        minHeight = size,
        contentDescription = description,
        contentPadding = PaddingValues(0.dp),
    ) {
        Glyph(path, if (primary) c.onAccent else c.text, iconSize)
    }
}

@Composable
fun SeekBar(fraction: Float, onSeek: (Float) -> Unit, modifier: Modifier = Modifier) {
    var dragging by remember { mutableStateOf<Float?>(null) }
    var width by remember { mutableStateOf(1) }
    val seek by rememberUpdatedState(onSeek)
    Box(
        modifier
            .height(24.dp)
            .onSizeChanged { width = maxOf(1, it.width) }
            .pointerInput(Unit) {
                detectTapGestures { o -> seek((o.x / width).coerceIn(0f, 1f)) }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { o -> dragging = (o.x / width).coerceIn(0f, 1f) },
                    onDragEnd = {
                        dragging?.let(seek)
                        dragging = null
                    },
                    onDragCancel = { dragging = null },
                ) { change, _ -> dragging = (change.position.x / width).coerceIn(0f, 1f) }
            },
        contentAlignment = Alignment.Center,
    ) {
        NeuProgress((dragging ?: fraction).coerceIn(0f, 1f), Modifier.fillMaxWidth())
    }
}

@Composable
private fun TimeRow(s: PlayerState, engine: PlayerEngine) {
    val c = Neu.colors
    val shown = s.positionSec.toLong()
    val fraction = if (s.durationSec > 0) (s.positionSec / s.durationSec).toFloat() else 0f
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        NeuText(Formatters.duration(shown), style = Neu.type.mono, color = c.textMuted)
        SeekBar(
            fraction,
            onSeek = { f -> if (s.durationSec > 0) engine.seekTo(f * s.durationSec) },
            modifier = Modifier.weight(1f),
        )
        NeuText(if (s.durationSec > 0) Formatters.duration(s.durationSec) else "--:--", style = Neu.type.mono, color = c.textMuted)
    }
}

private fun statusText(s: PlayerState): String? = when (s.status) {
    PlayStatus.Loading -> "Loading…"
    PlayStatus.Error -> s.error ?: "Could not play this video."
    else -> null
}

/** Small bar shown under every screen while something is loaded. */
@Composable
fun MiniPlayer(engine: PlayerEngine, onExpand: () -> Unit, modifier: Modifier = Modifier) {
    val s by engine.state.collectAsState()
    val item = s.item ?: return
    val c = Neu.colors
    val playing = s.status == PlayStatus.Playing || s.status == PlayStatus.Loading
    NeuSurface(modifier.fillMaxWidth(), radius = 22.dp) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            NetThumbnail(item.thumbnailUrl, Modifier.width(80.dp).aspectRatio(16f / 9f).clip(RoundedCornerShape(10.dp)), item.title)
            Column(Modifier.width(220.dp)) {
                NeuText(item.title, style = Neu.type.label, maxLines = 1)
                NeuText(statusText(s) ?: item.channel.ifBlank { "Netly" }, style = Neu.type.caption, color = if (s.status == PlayStatus.Error) c.error else c.textMuted, maxLines = 1)
            }
            RoundButton(G.Prev, "Previous", engine::previous, 40.dp, 16.dp)
            RoundButton(if (playing && s.status != PlayStatus.Loading) G.Pause else G.Play, if (playing) "Pause" else "Play", engine::togglePause, 48.dp, 20.dp, primary = true)
            RoundButton(G.Next, "Next", engine::next, 40.dp, 16.dp, enabled = s.hasNext)
            Column(Modifier.weight(1f)) { TimeRow(s, engine) }
            NeuTextButton(PlayerChoices.speedLabel(s.speed), onClick = {
                val list = PlayerChoices.speeds
                engine.setSpeed(list[(list.indexOf(s.speed) + 1).mod(list.size)])
            }, minHeight = 38.dp, radius = 12.dp)
            NeuButton(onClick = { engine.setAudioOnly(!s.audioOnly) }, selected = s.audioOnly, minHeight = 38.dp, radius = 12.dp, contentDescription = "Music mode") {
                NeuIcon(G.Headphones, if (s.audioOnly) c.accentText else c.text, size = 18.dp)
            }
            RoundButton(G.Expand, "Open player", onExpand, 38.dp, 16.dp)
            RoundButton(G.Close, "Close player", engine::stop, 38.dp, 14.dp)
        }
    }
}

@Composable
private fun VideoSurface(frame: ImageBitmap?, modifier: Modifier) {
    Canvas(modifier) {
        drawRect(Color(0xFF0B0D11))
        val f = frame ?: return@Canvas
        val scale = minOf(size.width / f.width, size.height / f.height)
        val w = (f.width * scale).toInt()
        val h = (f.height * scale).toInt()
        drawImage(
            f,
            dstOffset = IntOffset(((size.width - w) / 2).toInt(), ((size.height - h) / 2).toInt()),
            dstSize = IntSize(w, h),
            filterQuality = FilterQuality.Low,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FullPlayer(engine: PlayerEngine, onClose: () -> Unit, onDownload: () -> Unit, modifier: Modifier = Modifier) {
    val s by engine.state.collectAsState()
    val frame by engine.frame.collectAsState()
    val item = s.item
    val c = Neu.colors
    val playing = s.status == PlayStatus.Playing || s.status == PlayStatus.Loading
    val audioMode = s.effectiveAudioOnly

    Row(modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NeuTextButton("Back", onClose, minHeight = 40.dp, radius = 12.dp)
                NeuText(item?.title.orEmpty(), style = Neu.type.title, maxLines = 1, modifier = Modifier.weight(1f))
            }

            Box(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Color(0xFF0B0D11)), contentAlignment = Alignment.Center) {
                if (audioMode) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        NetThumbnail(item?.thumbnailUrl.orEmpty(), Modifier.height(190.dp).aspectRatio(16f / 9f).clip(RoundedCornerShape(16.dp)), item?.title)
                        NeuText("Audio only · saves data and battery", style = Neu.type.label, color = Color(0xFFB8C0FF))
                    }
                } else {
                    VideoSurface(frame, Modifier.fillMaxSize())
                }
                statusText(s)?.let { msg ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        NeuText(msg, style = Neu.type.label, color = if (s.status == PlayStatus.Error) Color(0xFFFF8A8A) else Color.White, modifier = Modifier.padding(horizontal = 24.dp))
                        if (s.status == PlayStatus.Error) NeuTextButton("Retry", onClick = engine::togglePause, primary = true, minHeight = 38.dp, radius = 12.dp)
                    }
                }
                if (s.speed != 1f) {
                    NeuText(
                        PlayerChoices.speedLabel(s.speed),
                        style = Neu.type.label,
                        color = Color(0xFFB8C0FF),
                        modifier = Modifier.align(Alignment.TopEnd).padding(12.dp).background(Color(0x99000000), RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }
            }

            TimeRow(s, engine)

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                RoundButton(G.Prev, "Previous", engine::previous, 46.dp, 18.dp)
                RoundButton(if (playing && s.status != PlayStatus.Loading) G.Pause else G.Play, if (playing) "Pause" else "Play", engine::togglePause, 58.dp, 24.dp, primary = true)
                RoundButton(G.Next, "Next", engine::next, 46.dp, 18.dp, enabled = s.hasNext)
                Box(Modifier.weight(1f))
                NeuIcon(G.Volume, c.textMuted, size = 20.dp)
                Box(Modifier.width(120.dp)) { SeekBar(s.volume, engine::setVolume) }
                NeuButton(onClick = { engine.setAudioOnly(!s.audioOnly) }, selected = s.audioOnly, minHeight = 40.dp, radius = 12.dp) {
                    NeuIcon(G.Headphones, if (s.audioOnly) c.accentText else c.text, size = 18.dp)
                    NeuText("Music mode", style = Neu.type.label, color = if (s.audioOnly) c.accentText else c.text)
                }
                NeuTextButton("Download", onDownload, minHeight = 40.dp, radius = 12.dp)
            }

            FlowRow(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NeuText("SPEED", style = Neu.type.overline, color = c.textMuted)
                    NeuSegmented(PlayerChoices.speeds.map { PlayerChoices.speedLabel(it) }, PlayerChoices.speeds.indexOf(s.speed).coerceAtLeast(0), { engine.setSpeed(PlayerChoices.speeds[it]) })
                }
                if (!audioMode) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        NeuText("QUALITY", style = Neu.type.overline, color = c.textMuted)
                        NeuSegmented(PlayerChoices.qualities.map { "${it}p" }, PlayerChoices.qualities.indexOf(s.quality).coerceAtLeast(0), { engine.setQuality(PlayerChoices.qualities[it]) })
                    }
                }
            }
            NeuText(
                "Tip: 360p uses the least data and runs smoothly on older PCs. Music mode streams only the sound.",
                style = Neu.type.caption,
                color = c.textMuted,
            )
        }

        UpNext(s, engine, Modifier.width(300.dp).fillMaxHeight())
    }
}

@Composable
private fun UpNext(s: PlayerState, engine: PlayerEngine, modifier: Modifier) {
    val c = Neu.colors
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            NeuText("UP NEXT", style = Neu.type.overline, color = c.textMuted)
            NeuTextButton(if (s.autoplay) "Autoplay on" else "Autoplay off", onClick = { engine.setAutoplay(!s.autoplay) }, selected = s.autoplay, minHeight = 34.dp, radius = 10.dp)
        }
        LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            itemsIndexed(s.queue, key = { i, it -> "$i-${it.key}" }) { i, it -> QueueRow(it, i == s.index, onClick = { engine.jumpTo(i) }) }
        }
    }
}

@Composable
private fun QueueRow(item: PlayItem, current: Boolean, onClick: () -> Unit) {
    val c = Neu.colors
    NeuSurface(Modifier.fillMaxWidth().clickable(onClick = onClick), level = if (current) NeuLevel.Inset else NeuLevel.RaisedMedium, radius = 16.dp) {
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NetThumbnail(item.thumbnailUrl, Modifier.width(84.dp).aspectRatio(16f / 9f).clip(RoundedCornerShape(8.dp)), item.title)
            Column(Modifier.weight(1f)) {
                NeuText(item.title, style = Neu.type.caption.copy(fontWeight = FontWeight.Bold), maxLines = 2)
                NeuText(
                    listOfNotNull(if (current) "Playing" else null, item.durationSec.takeIf { it > 0 }?.let { Formatters.duration(it) }).joinToString(" · "),
                    style = Neu.type.caption,
                    color = if (current) c.accentText else c.textMuted,
                    maxLines = 1,
                )
            }
        }
    }
}
