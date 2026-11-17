package com.skt.netly.pc.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.skt.netly.pc.domain.model.SearchItem
import com.skt.netly.pc.ui.components.NetThumbnail
import com.skt.netly.pc.ui.components.NeuButton
import com.skt.netly.pc.ui.components.NeuLevel
import com.skt.netly.pc.ui.components.NeuSegmented
import com.skt.netly.pc.ui.components.NeuSurface
import com.skt.netly.pc.ui.components.NeuText
import com.skt.netly.pc.ui.components.NeuTextButton
import com.skt.netly.pc.ui.theme.Neu
import com.skt.netly.pc.util.Formatters

private val heightLabels = listOf("1080p", "720p", "480p", "360p")
private val heightValues = listOf(1080, 720, 480, 360)
private val audioLabels = listOf("MP3 320", "MP3 192", "M4A")
private val audioKeys = listOf("mp3-320", "mp3-192", "m4a")

/** The whole page is one scrolling list, so results always get all the space that is left. */
@Composable
fun SearchScreen(vm: SearchViewModel) {
    val c = Neu.colors
    val s by vm.state.collectAsState()
    val playlist = s.playlist

    LazyColumn(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item {
            Column {
                NeuText("Search", style = Neu.type.display)
                NeuText(
                    "Search videos and playlists on YouTube.",
                    modifier = Modifier.padding(top = 6.dp),
                    style = Neu.type.body.copy(fontWeight = FontWeight.Medium),
                    color = c.textMuted,
                )
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                NeuSurface(Modifier.weight(1f), level = NeuLevel.Inset, radius = 18.dp) {
                    Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), contentAlignment = Alignment.CenterStart) {
                        if (s.query.isEmpty()) NeuText("Search YouTube…", color = c.textMuted, maxLines = 1)
                        BasicTextField(
                            value = s.query,
                            onValueChange = vm::onQuery,
                            singleLine = true,
                            textStyle = Neu.type.body.copy(color = c.text),
                            cursorBrush = SolidColor(c.accentText),
                            modifier = Modifier.fillMaxWidth().onPreviewKeyEvent { e ->
                                if (e.type == KeyEventType.KeyDown && e.key == Key.Enter) {
                                    vm.submit()
                                    true
                                } else {
                                    false
                                }
                            },
                        )
                    }
                }
                NeuTextButton("Search", onClick = vm::submit, primary = true, minHeight = 54.dp, radius = 18.dp)
            }
        }
        val notice = s.notice
        if (notice != null) item { NeuText(notice, style = Neu.type.caption, color = c.accentText) }

        when {
            playlist != null -> playlistItems(playlist, vm)
            s.loading -> item { NeuText("Searching…", color = c.textMuted) }
            s.error != null -> item { NeuText(s.error.orEmpty(), color = c.error) }
            !s.searched -> item { NeuText("Type something to search, or paste a YouTube link.", color = c.textMuted) }
            s.results.isEmpty() -> item { NeuText("No results found.", color = c.textMuted) }
            else -> items(s.results, key = { (if (it.isPlaylist) "p" else "v") + it.id }) { item ->
                if (item.isPlaylist) PlaylistRow(item, vm) else VideoRow(item, vm)
            }
        }
    }
}

@Composable
private fun Thumb(item: SearchItem) {
    NetThumbnail(
        item.thumbnailUrl,
        Modifier.width(132.dp).height(74.dp).clip(RoundedCornerShape(12.dp)),
        contentDescription = item.title,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VideoRow(item: SearchItem, vm: SearchViewModel) {
    val c = Neu.colors
    NeuSurface(Modifier.fillMaxWidth(), radius = 20.dp) {
        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
            Thumb(item)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                NeuText(item.title, style = Neu.type.label, maxLines = 2)
                NeuText(
                    listOfNotNull(item.channel.takeIf { it.isNotBlank() }, item.durationSec?.let { Formatters.duration(it) }).joinToString(" · "),
                    style = Neu.type.caption,
                    color = c.textMuted,
                    maxLines = 1,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    NeuTextButton("Play", onClick = { vm.play(item) }, primary = true, minHeight = 36.dp, radius = 12.dp)
                    NeuTextButton("Video", onClick = { vm.quickDownload(item, audio = false) }, minHeight = 36.dp, radius = 12.dp)
                    NeuTextButton("MP3", onClick = { vm.quickDownload(item, audio = true) }, minHeight = 36.dp, radius = 12.dp)
                    NeuTextButton("Options", onClick = { vm.openInHome(item) }, minHeight = 36.dp, radius = 12.dp)
                }
            }
        }
    }
}

@Composable
private fun PlaylistRow(item: SearchItem, vm: SearchViewModel) {
    val c = Neu.colors
    NeuSurface(Modifier.fillMaxWidth(), radius = 20.dp) {
        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
            Thumb(item)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                NeuText(item.title, style = Neu.type.label, maxLines = 2)
                NeuText(
                    listOfNotNull("Playlist", item.channel.takeIf { it.isNotBlank() }, item.count?.let { "$it videos" }).joinToString(" · "),
                    style = Neu.type.caption,
                    color = c.textMuted,
                    maxLines = 1,
                )
                NeuTextButton("Open playlist", onClick = { vm.openPlaylist(item.id) }, primary = true, minHeight = 36.dp, radius = 12.dp)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
private fun LazyListScope.playlistItems(p: PlaylistState, vm: SearchViewModel) {
    item {
        val c = Neu.colors
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            NeuTextButton("Back", onClick = vm::closePlaylist, minHeight = 40.dp, radius = 12.dp)
            NeuText(p.title.ifBlank { "Playlist" }, style = Neu.type.title, maxLines = 2, modifier = Modifier.weight(1f))
        }
    }
    when {
        p.loading -> item { NeuText("Loading playlist…", color = Neu.colors.textMuted) }
        p.error != null -> item { NeuText(p.error, color = Neu.colors.error) }
        else -> {
            item {
                val c = Neu.colors
                FlowRow(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    NeuText("${p.selected.size} / ${p.items.size}", style = Neu.type.mono, color = c.textMuted, softWrap = false, modifier = Modifier.padding(top = 10.dp))
                    NeuTextButton("Select all", onClick = vm::selectAll, minHeight = 38.dp, radius = 12.dp)
                    NeuTextButton("Select none", onClick = vm::selectNone, minHeight = 38.dp, radius = 12.dp)
                    NeuSegmented(listOf("Video", "Audio"), if (p.audio) 1 else 0, { vm.setPlaylistAudio(it == 1) })
                    if (p.audio) {
                        NeuSegmented(audioLabels, audioKeys.indexOf(p.audioKey).coerceAtLeast(0), { vm.setPlaylistAudioKey(audioKeys[it]) })
                    } else {
                        NeuSegmented(heightLabels, heightValues.indexOf(p.height).coerceAtLeast(0), { vm.setPlaylistHeight(heightValues[it]) })
                    }
                    NeuTextButton("Download · ${p.selected.size}", onClick = vm::downloadSelected, primary = true, enabled = p.selected.isNotEmpty(), minHeight = 40.dp, radius = 14.dp)
                }
            }
            items(p.items, key = { "i" + it.id }) { item ->
                val c = Neu.colors
                val checked = item.id in p.selected
                NeuSurface(Modifier.fillMaxWidth(), radius = 16.dp) {
                    Row(Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        NeuButton(
                            onClick = { vm.toggle(item.id) },
                            selected = checked,
                            minHeight = 34.dp,
                            radius = 10.dp,
                            contentPadding = PaddingValues(horizontal = 0.dp),
                            modifier = Modifier.width(34.dp),
                            contentDescription = item.title,
                        ) { NeuText(if (checked) "✓" else "", color = c.accentText) }
                        Thumb(item)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            NeuText(item.title, style = Neu.type.label, maxLines = 2)
                            NeuText(
                                listOfNotNull(item.channel.takeIf { it.isNotBlank() }, item.durationSec?.let { Formatters.duration(it) }).joinToString(" · "),
                                style = Neu.type.caption,
                                color = c.textMuted,
                                maxLines = 1,
                            )
                        }
                        NeuTextButton("Play", onClick = { vm.play(item) }, minHeight = 36.dp, radius = 12.dp)
                    }
                }
            }
        }
    }
}
