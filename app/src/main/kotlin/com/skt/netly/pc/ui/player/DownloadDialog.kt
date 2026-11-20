package com.skt.netly.pc.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.skt.netly.pc.domain.model.MediaInfo
import com.skt.netly.pc.player.PlayItem
import com.skt.netly.pc.player.PlayerEngine
import com.skt.netly.pc.ui.components.NetThumbnail
import com.skt.netly.pc.ui.components.NeuSegmented
import com.skt.netly.pc.ui.components.NeuSurface
import com.skt.netly.pc.ui.components.NeuText
import com.skt.netly.pc.ui.components.NeuTextButton
import com.skt.netly.pc.ui.theme.Neu
import com.skt.netly.pc.util.Formatters

/** Choose video quality (lowest to highest available) or audio format, then start the download. */
@Composable
fun DownloadDialog(
    engine: PlayerEngine,
    item: PlayItem,
    onDismiss: () -> Unit,
    onStart: (audio: Boolean, height: Int?, audioKey: String) -> Unit,
) {
    val c = Neu.colors
    var info by remember(item.key) { mutableStateOf<MediaInfo?>(null) }
    var err by remember(item.key) { mutableStateOf<String?>(null) }
    var attempt by remember(item.key) { mutableStateOf(0) }
    var audioMode by remember { mutableStateOf(false) }
    var height by remember(item.key) { mutableStateOf<Int?>(null) }
    var audioKey by remember { mutableStateOf("mp3-320") }

    LaunchedEffect(item.key, attempt) {
        info = null
        err = null
        try {
            val m = engine.mediaInfo(item.url ?: error("No address"))
            info = m
            val sorted = m.videoQualities.map { it.height }.sorted()
            height = (sorted.filter { it <= 1080 }.maxOrNull() ?: sorted.lastOrNull())
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            err = e.message ?: "Could not load the quality list."
        }
    }

    Box(
        Modifier.fillMaxSize().background(Color(0x99000000)).pointerInput(Unit) { detectTapsToDismiss(onDismiss) },
        contentAlignment = Alignment.Center,
    ) {
        NeuSurface(Modifier.width(480.dp).padding(16.dp).pointerInput(Unit) { detectTapGestures { } }, radius = 24.dp) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NetThumbnail(item.thumbnailUrl, Modifier.width(96.dp).heightIn(min = 54.dp, max = 54.dp).clip(RoundedCornerShape(10.dp)), item.title)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        NeuText("Download", style = Neu.type.overline, color = c.textMuted)
                        NeuText(item.title, style = Neu.type.label, maxLines = 2)
                    }
                }
                NeuSegmented(listOf("Video", "Audio"), if (audioMode) 1 else 0, { audioMode = it == 1 })

                val m = info
                when {
                    err != null -> {
                        NeuText(err.orEmpty(), style = Neu.type.caption, color = Color(0xFFFF8A8A), maxLines = 4)
                        NeuTextButton("Retry", onClick = { attempt++ }, minHeight = 38.dp, radius = 12.dp)
                    }
                    m == null -> NeuText("Loading qualities…", style = Neu.type.caption, color = c.textMuted)
                    else -> Column(
                        Modifier.fillMaxWidth().heightIn(max = 300.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (!audioMode) {
                            if (m.videoQualities.isEmpty()) NeuText("No video quality list was found. Choose Audio, or try again.", style = Neu.type.caption, color = c.textMuted)
                            m.videoQualities.sortedBy { it.height }.forEach { q ->
                                ChoiceLine(q.label, Formatters.approxBytes(q.estimatedBytes), height == q.height) { height = q.height }
                            }
                        } else {
                            m.audioFormats.forEach { a ->
                                ChoiceLine(a.label, Formatters.approxBytes(a.estimatedBytes), audioKey == a.key) { audioKey = a.key }
                            }
                        }
                    }
                }
                if (!audioMode && (height ?: 0) > 1080) {
                    NeuText("Above 1080p the file is saved as MKV (best quality; plays in VLC and most players).", style = Neu.type.caption, color = c.textMuted)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)) {
                    NeuTextButton("Cancel", onClick = onDismiss, minHeight = 40.dp, radius = 12.dp)
                    NeuTextButton(
                        "Download",
                        onClick = { onStart(audioMode, if (audioMode) null else height, audioKey) },
                        primary = true,
                        enabled = info != null && (audioMode || height != null),
                        minHeight = 40.dp,
                        radius = 12.dp,
                    )
                }
            }
        }
    }
}

@Composable
private fun ChoiceLine(label: String, size: String, selected: Boolean, onClick: () -> Unit) {
    val c = Neu.colors
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(if (selected) c.accent.copy(alpha = 0.16f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NeuText(label, style = Neu.type.label, color = if (selected) c.accentText else c.text, maxLines = 1, softWrap = false, modifier = Modifier.weight(1f))
        NeuText(size, style = Neu.type.caption, color = c.textMuted, maxLines = 1, softWrap = false)
    }
}

private suspend fun androidx.compose.ui.input.pointer.PointerInputScope.detectTapsToDismiss(onDismiss: () -> Unit) {
    detectTapGestures(onTap = { onDismiss() })
}
