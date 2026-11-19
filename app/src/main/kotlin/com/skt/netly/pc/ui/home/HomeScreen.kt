package com.skt.netly.pc.ui.home

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skt.netly.pc.data.engine.EngineStatus
import com.skt.netly.pc.data.engine.ErrorKind
import com.skt.netly.pc.domain.model.MediaInfo
import com.skt.netly.pc.ui.components.NeuButton
import com.skt.netly.pc.ui.components.NeuIcon
import com.skt.netly.pc.ui.components.NeuIcons
import com.skt.netly.pc.ui.components.NeuLevel
import com.skt.netly.pc.ui.components.NeuPill
import com.skt.netly.pc.ui.components.NeuProgress
import com.skt.netly.pc.ui.components.NeuSegmented
import com.skt.netly.pc.ui.components.NeuSurface
import com.skt.netly.pc.ui.components.NeuSwitch
import com.skt.netly.pc.ui.components.NeuText
import com.skt.netly.pc.ui.components.NeuTextButton
import com.skt.netly.pc.ui.components.NetThumbnail
import com.skt.netly.pc.ui.theme.Neu
import com.skt.netly.pc.util.Formatters

@Composable
fun HomeScreen(vm: HomeViewModel) {
    val state by vm.state.collectAsState()
    val engine by vm.engineStatus.collectAsState()
    val clipboard = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        HomeHeader(engine)
        LinkBar(
            value = state.input,
            loading = state.phase == FetchPhase.Loading,
            onChange = vm::onInputChange,
            onFetch = vm::fetch,
            onPaste = { clipboard.getText()?.text?.let { vm.pasteAndFetch(it) } },
        )
        SupportRow()
        when (state.phase) {
            FetchPhase.Empty -> EmptyCard()
            FetchPhase.Loading -> LoadingCard()
            FetchPhase.Error -> ErrorCard(state, onRetry = vm::fetch)
            FetchPhase.Ready -> ReadyCards(state, vm)
        }
    }
}

@Composable
private fun HomeHeader(status: EngineStatus) {
    val c = Neu.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(Modifier.weight(1f)) {
            NeuText("Download anything from YouTube", style = Neu.type.display)
            NeuText(
                "Paste a link, pick a quality, and Netly does the rest.",
                modifier = Modifier.padding(top = 6.dp),
                style = Neu.type.body.copy(fontWeight = FontWeight.Medium),
                color = c.textMuted,
            )
        }
        EnginePill(status)
    }
}

@Composable
private fun EnginePill(status: EngineStatus) {
    val c = Neu.colors
    val (dot, text) = when (status) {
        EngineStatus.Preparing -> c.textMuted to "Preparing engine…"
        is EngineStatus.Ready -> c.success to "Engine ready · yt-dlp ${status.version}"
        is EngineStatus.Failed -> c.error to "Engine problem"
    }
    NeuSurface(level = NeuLevel.Inset, radius = 20.dp) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(Modifier.size(9.dp).background(dot, CircleShape))
            NeuText(text, style = Neu.type.label.copy(fontSize = 13.sp), maxLines = 1)
        }
    }
}

@Composable
private fun LinkBar(
    value: String,
    loading: Boolean,
    onChange: (String) -> Unit,
    onFetch: () -> Unit,
    onPaste: () -> Unit,
) {
    val c = Neu.colors
    NeuSurface(modifier = Modifier.fillMaxWidth().height(72.dp), level = NeuLevel.Inset, radius = 24.dp) {
        Row(
            modifier = Modifier.fillMaxSize().padding(start = 22.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            NeuIcon(HomeIcons.Link, c.textMuted, size = 22.dp)
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = Neu.type.body.copy(fontSize = 17.sp, color = c.text),
                cursorBrush = SolidColor(c.accentText),
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = "YouTube link" }
                    .onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown && event.key == Key.Enter) {
                            onFetch()
                            true
                        } else {
                            false
                        }
                    },
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty()) {
                            NeuText(
                                "Paste a YouTube link…",
                                style = Neu.type.body.copy(fontSize = 17.sp),
                                color = c.textMuted,
                            )
                        }
                        inner()
                    }
                },
            )
            NeuButton(
                onClick = onPaste,
                minHeight = 48.dp,
                contentDescription = "Paste link from clipboard",
                contentPadding = PaddingValues(horizontal = 18.dp),
            ) {
                NeuIcon(HomeIcons.Clipboard, c.text, size = 18.dp)
                NeuText("Paste", style = Neu.type.body.copy(fontWeight = FontWeight.Bold))
            }
            NeuTextButton(
                text = if (loading) "Fetching…" else "Fetch",
                onClick = onFetch,
                primary = true,
                enabled = !loading,
                minHeight = 52.dp,
                radius = 18.dp,
            )
        }
    }
}

@Composable
private fun SupportRow() {
    val c = Neu.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        NeuText("Supports", style = Neu.type.caption.copy(fontSize = 13.sp), color = c.textMuted)
        listOf("Video", "Shorts", "Audio").forEach { NeuPill(it, inset = false) }
        Spacer(Modifier.weight(1f))
        NeuText("Playlists are coming soon", style = Neu.type.caption.copy(fontSize = 13.sp), color = c.textMuted)
    }
}

@Composable
private fun EmptyCard() {
    val c = Neu.colors
    NeuSurface(modifier = Modifier.fillMaxWidth(), radius = 28.dp) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp, horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            NeuIcon(NeuIcons.Download, c.accentText, size = 40.dp, strokeWidth = 1.6f)
            NeuText("Paste a YouTube link above to get started", style = Neu.type.title)
            NeuText(
                "1. Paste the link   2. Pick video or audio and a quality   3. Download",
                style = Neu.type.body.copy(fontWeight = FontWeight.Medium),
                color = c.textMuted,
            )
        }
    }
}

@Composable
private fun LoadingCard() {
    val c = Neu.colors
    val transition = rememberInfiniteTransition(label = "loading")
    val progress by transition.animateFloat(
        initialValue = 0.12f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "loadingProgress",
    )
    NeuSurface(modifier = Modifier.fillMaxWidth(), radius = 28.dp) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp, horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            NeuText("Fetching video details…", style = Neu.type.title)
            NeuText("This usually takes a few seconds.", color = c.textMuted)
            NeuProgress(progress = progress, modifier = Modifier.width(320.dp))
        }
    }
}

@Composable
private fun ErrorCard(state: HomeUiState, onRetry: () -> Unit) {
    val c = Neu.colors
    val error = state.error
    NeuSurface(modifier = Modifier.fillMaxWidth(), radius = 28.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(28.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                NeuText("Could not fetch this video", style = Neu.type.title, color = c.error)
                NeuText(
                    error?.message ?: "Something went wrong.",
                    style = Neu.type.body.copy(fontWeight = FontWeight.Medium),
                )
                if (error?.kind == ErrorKind.Blocked) {
                    NeuText(
                        "Support for signing in with your browser is planned for a later update.",
                        style = Neu.type.caption.copy(fontSize = 13.sp),
                        color = c.textMuted,
                    )
                }
                error?.detail?.let {
                    NeuText(it, style = Neu.type.mono.copy(fontSize = 12.sp), color = c.textMuted, maxLines = 2)
                }
            }
            if (error?.kind != ErrorKind.InvalidUrl && error?.kind != ErrorKind.PlaylistNotSupported) {
                NeuTextButton("Retry", onClick = onRetry, minHeight = 48.dp)
            }
        }
    }
}

@Composable
private fun ReadyCards(state: HomeUiState, vm: HomeViewModel) {
    val info = state.info ?: return
    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        PreviewCard(info, Modifier.weight(1f).fillMaxHeight())
        OptionsCard(state, info, vm, Modifier.width(440.dp).fillMaxHeight())
    }
}

@Composable
private fun PreviewCard(info: MediaInfo, modifier: Modifier) {
    val c = Neu.colors
    NeuSurface(modifier = modifier, radius = 28.dp) {
        Column(
            modifier = Modifier.fillMaxHeight().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(20.dp)),
            ) {
                NetThumbnail(info.thumbnailUrl, Modifier.fillMaxSize(), info.title)
                NeuText(
                    Formatters.duration(info.durationSec),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .background(Color(0xC7000000), RoundedCornerShape(8.dp))
                        .padding(horizontal = 9.dp, vertical = 4.dp),
                    style = Neu.type.mono.copy(fontSize = 13.sp),
                    color = Color.White,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                NeuText(info.title, style = Neu.type.title, maxLines = 2)
                val meta = listOf(info.channel, Formatters.views(info.viewCount), Formatters.age(info.uploadDate))
                    .filter { it.isNotBlank() }
                    .joinToString(" · ")
                NeuText(meta, style = Neu.type.body.copy(fontWeight = FontWeight.Medium), color = c.textMuted, maxLines = 1)
            }
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (info.maxHeight >= 2160) NeuPill("4K available")
                if (info.subtitleLanguages > 0) {
                    NeuPill("Subtitles: ${info.subtitleLanguages} language${if (info.subtitleLanguages == 1) "" else "s"}")
                }
                if (info.chapterCount > 0) NeuPill("Chapters: ${info.chapterCount}")
            }
        }
    }
}

private data class OptionChip(
    val key: String,
    val label: String,
    val size: String,
    val selected: Boolean,
    val onClick: () -> Unit,
)

@Composable
private fun OptionsCard(state: HomeUiState, info: MediaInfo, vm: HomeViewModel, modifier: Modifier) {
    val c = Neu.colors
    val isVideo = state.mode == DownloadMode.Video

    val chips: List<OptionChip> = if (isVideo) {
        info.videoQualities.map { q ->
            OptionChip(q.height.toString(), q.label, Formatters.bytes(q.estimatedBytes), q.height == state.selectedHeight) {
                vm.selectHeight(q.height)
            }
        }
    } else {
        info.audioFormats.map { a ->
            OptionChip(a.key, a.label, Formatters.approxBytes(a.estimatedBytes), a.key == state.selectedAudio) {
                vm.selectAudio(a.key)
            }
        }
    }

    NeuSurface(modifier = modifier, radius = 28.dp) {
        Column(
            modifier = Modifier.fillMaxHeight().padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            NeuSegmented(
                options = listOf("Video", "Audio only"),
                selectedIndex = if (isVideo) 0 else 1,
                onSelect = { vm.selectMode(if (it == 0) DownloadMode.Video else DownloadMode.Audio) },
                modifier = Modifier.fillMaxWidth(),
            )

            NeuText(
                if (isVideo) "VIDEO QUALITY" else "AUDIO FORMAT",
                style = Neu.type.overline,
                color = c.textMuted,
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                chips.chunked(3).forEach { rowChips ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        rowChips.forEach { chip ->
                            NeuButton(
                                onClick = chip.onClick,
                                modifier = Modifier.weight(1f),
                                selected = chip.selected,
                                radius = 14.dp,
                                minHeight = 56.dp,
                                contentPadding = PaddingValues(horizontal = 6.dp),
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    NeuText(
                                        chip.label,
                                        style = Neu.type.label,
                                        color = if (chip.selected) c.accentText else c.text,
                                        maxLines = 1,
                                    )
                                    NeuText(chip.size, style = Neu.type.mono.copy(fontSize = 11.sp), color = c.textMuted, maxLines = 1)
                                }
                            }
                        }
                        repeat(3 - rowChips.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }

            if (isVideo) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    NeuText("Format", style = Neu.type.label)
                    val formats = listOf("MP4", "WEBM", "MKV")
                    NeuSegmented(
                        options = formats,
                        selectedIndex = formats.indexOf(state.container).coerceAtLeast(0),
                        onSelect = { vm.selectContainer(formats[it]) },
                        modifier = Modifier.width(240.dp),
                    )
                }
                ToggleRow("Embed subtitles", state.embedSubtitles, vm::setEmbedSubtitles)
            }
            ToggleRow("Embed thumbnail & tags", state.embedThumbnail, vm::setEmbedThumbnail)

            NeuSurface(modifier = Modifier.fillMaxWidth(), level = NeuLevel.Inset, radius = 14.dp) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    NeuIcon(NeuIcons.Folder, c.textMuted, size = 18.dp)
                    NeuText("Downloads\\Netly", style = Neu.type.mono.copy(fontSize = 12.sp), maxLines = 1)
                }
            }

            Spacer(Modifier.weight(1f))

            val size = state.selectedSizeText()
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NeuTextButton("Add to queue", onClick = { vm.download(startNow = false) }, minHeight = 54.dp)
                NeuTextButton(
                    text = if (size == "—") "Download" else "Download · $size",
                    onClick = { vm.download(startNow = true) },
                    primary = true,
                    modifier = Modifier.weight(1f),
                    minHeight = 54.dp,
                    radius = 18.dp,
                )
            }
            NeuText(
                state.notice ?: " ",
                style = Neu.type.caption.copy(fontSize = 13.sp),
                color = c.textMuted,
            )
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(36.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        NeuText(label, style = Neu.type.label)
        NeuSwitch(checked = checked, onCheckedChange = onChange, contentDescription = label)
    }
}
