package com.skt.netly.pc.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import com.skt.netly.pc.ui.theme.NeuPerf
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
    const val Shrink = "M4 14h6v6M20 10h-6V4M14 10l7-7M3 21l7-7"
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
        NeuText(Formatters.duration(shown), style = Neu.type.mono, color = c.textMuted, maxLines = 1, softWrap = false)
        SeekBar(
            fraction,
            onSeek = { f -> if (s.durationSec > 0) engine.seekTo((f * s.durationSec).toDouble()) },
            modifier = Modifier.weight(1f),
        )
        NeuText(if (s.durationSec > 0) Formatters.duration(s.durationSec) else "--:--", style = Neu.type.mono, color = c.textMuted, maxLines = 1, softWrap = false)
    }
}

private fun statusText(s: PlayerState): String? = when (s.status) {
    PlayStatus.Loading -> "Loading…"
    PlayStatus.Error -> s.error ?: "Could not play this video."
    else -> null
}

/** Small bar shown under every screen while something is loaded: info and buttons on top, seek bar below. */
@Composable
fun MiniPlayer(engine: PlayerEngine, onExpand: () -> Unit, modifier: Modifier = Modifier) {
    val s by engine.state.collectAsState()
    val item = s.item ?: return
    val c = Neu.colors
    val playing = s.status == PlayStatus.Playing || s.status == PlayStatus.Loading
    NeuSurface(modifier.fillMaxWidth(), radius = 22.dp) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                NetThumbnail(item.thumbnailUrl, Modifier.width(68.dp).height(38.dp).clip(RoundedCornerShape(8.dp)), item.title)
                Column(Modifier.weight(1f)) {
                    NeuText(item.title, style = Neu.type.label, maxLines = 1)
                    NeuText(
                        statusText(s) ?: item.channel.ifBlank { "Netly" },
                        style = Neu.type.caption,
                        color = if (s.status == PlayStatus.Error) c.error else c.textMuted,
                        maxLines = 1,
                    )
                }
                RoundButton(G.Prev, "Previous", engine::previous, 38.dp, 15.dp)
                RoundButton(if (playing && s.status != PlayStatus.Loading) G.Pause else G.Play, if (playing) "Pause" else "Play", engine::togglePause, 44.dp, 18.dp, primary = true)
                RoundButton(G.Next, "Next", engine::next, 38.dp, 15.dp, enabled = s.hasNext)
                NeuTextButton(
                    PlayerChoices.speedLabel(s.speed),
                    onClick = {
                        val list = PlayerChoices.speeds
                        engine.setSpeed(list[(list.indexOf(s.speed) + 1).mod(list.size)])
                    },
                    minHeight = 36.dp,
                    radius = 12.dp,
                    contentPadding = PaddingValues(horizontal = 12.dp),
                )
                NeuButton(
                    onClick = { engine.setAudioOnly(!s.audioOnly) },
                    selected = s.audioOnly,
                    minHeight = 36.dp,
                    radius = 12.dp,
                    contentDescription = "Music mode",
                    contentPadding = PaddingValues(horizontal = 10.dp),
                ) {
                    NeuIcon(G.Headphones, if (s.audioOnly) c.accentText else c.text, size = 18.dp)
                }
                RoundButton(G.Expand, "Open player", onExpand, 36.dp, 15.dp)
                RoundButton(G.Close, "Close player", engine::stop, 36.dp, 13.dp)
            }
            TimeRow(s, engine)
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

/** A row of small choice buttons that wraps onto more lines instead of squeezing the text. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChoiceRow(label: String, options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        NeuText(label, style = Neu.type.overline, color = Neu.colors.textMuted)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEachIndexed { i, text ->
                NeuTextButton(
                    text,
                    onClick = { onSelect(i) },
                    selected = i == selected,
                    minHeight = 34.dp,
                    radius = 12.dp,
                    contentPadding = PaddingValues(horizontal = 12.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlayerMain(
    s: PlayerState,
    frame: ImageBitmap?,
    engine: PlayerEngine,
    videoHeight: Dp,
    onClose: () -> Unit,
    onDownload: () -> Unit,
    onFullscreen: () -> Unit,
    modifier: Modifier,
) {
    val c = Neu.colors
    val item = s.item
    val playing = s.status == PlayStatus.Playing || s.status == PlayStatus.Loading
    val audioMode = s.effectiveAudioOnly

    Column(modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NeuTextButton("Back", onClose, minHeight = 40.dp, radius = 12.dp)
            NeuText(item?.title.orEmpty(), style = Neu.type.title, maxLines = 2, modifier = Modifier.weight(1f))
        }

        Box(
            Modifier.fillMaxWidth().height(videoHeight).clip(RoundedCornerShape(20.dp)).background(Color(0xFF0B0D11))
                .pointerInput(audioMode) {
                    if (!audioMode) detectTapGestures(onDoubleTap = { onFullscreen() })
                },
            contentAlignment = Alignment.Center,
        ) {
            if (audioMode) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    NetThumbnail(
                        item?.thumbnailUrl.orEmpty(),
                        Modifier.height((videoHeight - 64.dp).coerceAtLeast(60.dp)).aspectRatio(16f / 9f).clip(RoundedCornerShape(14.dp)),
                        item?.title,
                    )
                    NeuText("Audio only · saves data and battery", style = Neu.type.label, color = Color(0xFFB8C0FF), maxLines = 1)
                }
            } else {
                VideoSurface(frame, Modifier.fillMaxSize())
            }
            statusText(s)?.let { msg ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    NeuText(
                        msg,
                        style = Neu.type.label,
                        color = if (s.status == PlayStatus.Error) Color(0xFFFF8A8A) else Color.White,
                        modifier = Modifier.padding(horizontal = 24.dp).background(Color(0x99000000), RoundedCornerShape(10.dp)).padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                    if (s.status == PlayStatus.Error) NeuTextButton("Retry", onClick = engine::togglePause, primary = true, minHeight = 38.dp, radius = 12.dp)
                }
            }
            if (!audioMode) {
                Box(
                    Modifier.align(Alignment.BottomEnd).padding(10.dp).size(36.dp)
                        .background(Color(0x99000000), RoundedCornerShape(10.dp))
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onFullscreen() },
                    contentAlignment = Alignment.Center,
                ) { Glyph(G.Expand, Color.White, 18.dp) }
            }
            if (s.speed != 1f) {
                NeuText(
                    PlayerChoices.speedLabel(s.speed),
                    style = Neu.type.label,
                    color = Color(0xFFB8C0FF),
                    maxLines = 1,
                    modifier = Modifier.align(Alignment.TopEnd).padding(10.dp).background(Color(0x99000000), RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 5.dp),
                )
            }
        }

        TimeRow(s, engine)

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            RoundButton(G.Prev, "Previous", engine::previous, 46.dp, 18.dp)
            RoundButton(if (playing && s.status != PlayStatus.Loading) G.Pause else G.Play, if (playing) "Pause" else "Play", engine::togglePause, 58.dp, 24.dp, primary = true)
            RoundButton(G.Next, "Next", engine::next, 46.dp, 18.dp, enabled = s.hasNext)
            Box(Modifier.weight(1f))
            NeuIcon(G.Volume, c.textMuted, size = 20.dp)
            Box(Modifier.width(110.dp)) { SeekBar(s.volume, engine::setVolume) }
        }

        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            NeuButton(onClick = { engine.setAudioOnly(!s.audioOnly) }, selected = s.audioOnly, minHeight = 40.dp, radius = 12.dp, contentPadding = PaddingValues(horizontal = 14.dp)) {
                NeuIcon(G.Headphones, if (s.audioOnly) c.accentText else c.text, size = 18.dp)
                NeuText("Music mode", style = Neu.type.label, color = if (s.audioOnly) c.accentText else c.text, maxLines = 1, softWrap = false)
            }
            NeuTextButton("Download", onDownload, minHeight = 40.dp, radius = 12.dp, enabled = item?.url != null)
        }

        ChoiceRow("SPEED", PlayerChoices.speeds.map { PlayerChoices.speedLabel(it) }, PlayerChoices.speeds.indexOf(s.speed), { engine.setSpeed(PlayerChoices.speeds[it]) })
        if (!audioMode) {
            ChoiceRow("QUALITY", PlayerChoices.qualities.map { "${it}p" }, PlayerChoices.qualities.indexOf(s.quality), { engine.setQuality(PlayerChoices.qualities[it]) })
        }
        NeuText(
            "Tip: 360p uses the least data and runs smoothly on older PCs. Music mode streams only the sound.",
            style = Neu.type.caption,
            color = c.textMuted,
        )
    }
}

@Composable
fun FullPlayer(engine: PlayerEngine, onClose: () -> Unit, onDownload: () -> Unit, onFullscreen: () -> Unit, modifier: Modifier = Modifier) {
    val s by engine.state.collectAsState()
    val frame by engine.frame.collectAsState()

    BoxWithConstraints(modifier.fillMaxSize()) {
        val total = maxWidth
        val sideBySide = total >= 760.dp
        if (sideBySide) {
            val mainWidth = total - 320.dp
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                PlayerMain(
                    s, frame, engine,
                    videoHeight = (mainWidth * 9f / 16f).coerceIn(160.dp, 360.dp),
                    onClose = onClose, onDownload = onDownload, onFullscreen = onFullscreen,
                    modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(bottom = 16.dp),
                )
                UpNext(s, engine, Modifier.width(300.dp).fillMaxHeight())
            }
        } else {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                PlayerMain(
                    s, frame, engine,
                    videoHeight = (total * 9f / 16f).coerceIn(160.dp, 320.dp),
                    onClose = onClose, onDownload = onDownload, onFullscreen = onFullscreen,
                    modifier = Modifier.fillMaxWidth(),
                )
                UpNext(s, engine, Modifier.fillMaxWidth().height(420.dp))
            }
        }
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


/** Full-screen video: tap = play/pause, double-tap = leave. Controls fade in when the mouse moves and hide after a few seconds. */
@Composable
fun FullscreenPlayer(engine: PlayerEngine, onExit: () -> Unit, modifier: Modifier = Modifier) {
    val s by engine.state.collectAsState()
    val frame by engine.frame.collectAsState()
    val c = Neu.colors
    var activity by remember { mutableStateOf(0) }
    var controls by remember { mutableStateOf(true) }
    val playing = s.status == PlayStatus.Playing || s.status == PlayStatus.Loading
    LaunchedEffect(activity, playing) {
        controls = true
        if (playing) {
            delay(3000)
            controls = false
        }
    }
    val audioMode = s.effectiveAudioOnly
    Box(
        modifier.fillMaxSize().background(Color.Black)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val e = awaitPointerEvent()
                        if (e.type == PointerEventType.Move || e.type == PointerEventType.Press) activity++
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = { onExit() }, onTap = { engine.togglePause() })
            },
        contentAlignment = Alignment.Center,
    ) {
        if (audioMode) {
            NetThumbnail(s.item?.thumbnailUrl.orEmpty(), Modifier.fillMaxSize(0.5f).aspectRatio(16f / 9f).clip(RoundedCornerShape(14.dp)), s.item?.title)
        } else {
            VideoSurface(frame, Modifier.fillMaxSize())
        }
        statusText(s)?.let { msg ->
            NeuText(
                msg,
                style = Neu.type.label,
                color = if (s.status == PlayStatus.Error) Color(0xFFFF8A8A) else Color.White,
                modifier = Modifier.background(Color(0x99000000), RoundedCornerShape(10.dp)).padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
        AnimatedVisibility(
            visible = controls,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = if (NeuPerf.reduced) androidx.compose.animation.EnterTransition.None else fadeIn(),
            exit = if (NeuPerf.reduced) androidx.compose.animation.ExitTransition.None else fadeOut(),
        ) {
            NeuSurface(Modifier.padding(24.dp).fillMaxWidth(0.8f), radius = 20.dp) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    NeuText(s.item?.title.orEmpty(), style = Neu.type.label, maxLines = 1)
                    TimeRow(s, engine)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        RoundButton(G.Prev, "Previous", engine::previous, 42.dp, 16.dp)
                        RoundButton(if (playing && s.status != PlayStatus.Loading) G.Pause else G.Play, if (playing) "Pause" else "Play", engine::togglePause, 52.dp, 22.dp, primary = true)
                        RoundButton(G.Next, "Next", engine::next, 42.dp, 16.dp, enabled = s.hasNext)
                        Box(Modifier.weight(1f))
                        NeuTextButton(
                            PlayerChoices.speedLabel(s.speed),
                            onClick = {
                                val i = PlayerChoices.speeds.indexOf(s.speed)
                                engine.setSpeed(PlayerChoices.speeds[(i + 1) % PlayerChoices.speeds.size])
                            },
                            minHeight = 38.dp, radius = 12.dp,
                        )
                        NeuIcon(G.Volume, c.textMuted, size = 20.dp)
                        Box(Modifier.width(100.dp)) { SeekBar(s.volume, engine::setVolume) }
                        RoundButton(G.Shrink, "Exit full screen", onExit, 42.dp, 18.dp)
                    }
                }
            }
        }
    }
}
