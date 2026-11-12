package com.skt.netly.pc.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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

@Composable
fun SearchScreen(vm: SearchViewModel) {
    val c = Neu.colors
    val s by vm.state.collectAsState()

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Column {
            NeuText("Search", style = Neu.type.display)
            NeuText(
                "Search videos and playlists on YouTube.",
                modifier = Modifier.padding(top = 6.dp),
                style = Neu.type.body.copy(fontWeight = FontWeight.Medium),
                color = c.textMuted,
            )
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            NeuSurface(Modifier.weight(1f), level = NeuLevel.Inset, radius = 18.dp) {
                Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), contentAlignment = Alignment.CenterStart) {
                    if (s.query.isEmpty()) NeuText("Search YouTube…", color = c.textMuted)
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

        val notice = s.notice
        if (notice != null) NeuText(notice, style = Neu.type.caption, color = c.accentText)

        val playlist = s.playlist
        when {
            playlist != null -> PlaylistView(playlist, vm)
            s.loading -> NeuText("Searching…", color = c.textMuted)
            s.error != null -> NeuText(s.error.orEmpty(), color = c.error)
            !s.searched -> NeuText("Type something to search, or paste a YouTube link.", color = c.textMuted)
            s.videos.isEmpty() && s.playlists.isEmpty() -> NeuText("No results found.", color = c.textMuted)
            else -> LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (s.playlists.isNotEmpty()) {
                    item { NeuText("Playlists".uppercase(), style = Neu.type.overline, color = c.textMuted) }
                    items(s.playlists, key = { "p" + it.id }) { PlaylistRow(it, vm) }
                }
                if (s.videos.isNotEmpty()) {
                    item { NeuText("Videos".uppercase(), style = Neu.type.overline, color = c.textMuted) }
                    items(s.videos, key = { "v" + it.id }) { VideoRow(it, vm) }
                }
            }
        }
    }
}

@Composable
private fun Thumb(item: SearchItem) {
    NetThumbnail(
        item.thumbnailUrl,
        Modifier.width(144.dp).height(81.dp).clip(RoundedCornerShape(12.dp)),
        contentDescription = item.title,
    )
}

@Composable
private fun VideoRow(item: SearchItem, vm: SearchViewModel) {
    val c = Neu.colors
    NeuSurface(Modifier.fillMaxWidth(), radius = 20.dp) {
        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Thumb(item)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                NeuText(item.title, style = Neu.type.label, maxLines = 2)
                NeuText(
                    listOfNotNull(item.channel.takeIf { it.isNotBlank() }, item.durationSec?.let { Formatters.duration(it) }).joinToString(" · "),
                    style = Neu.type.caption,
                    color = c.textMuted,
                    maxLines = 1,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                NeuTextButton("Play", onClick = { vm.play(item) }, primary = true, minHeight = 38.dp, radius = 12.dp)
                NeuTextButton("Video", onClick = { vm.quickDownload(item, audio = false) }, minHeight = 38.dp, radius = 12.dp)
                NeuTextButton("MP3", onClick = { vm.quickDownload(item, audio = true) }, minHeight = 38.dp, radius = 12.dp)
                NeuTextButton("Options", onClick = { vm.openInHome(item) }, minHeight = 38.dp, radius = 12.dp)
            }
        }
    }
}

@Composable
private fun PlaylistRow(item: SearchItem, vm: SearchViewModel) {
    val c = Neu.colors
    NeuSurface(Modifier.fillMaxWidth(), radius = 20.dp) {
        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Thumb(item)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                NeuText(item.title, style = Neu.type.label, maxLines = 2)
                NeuText(
                    listOfNotNull("Playlist", item.channel.takeIf { it.isNotBlank() }, item.count?.let { "$it videos" }).joinToString(" · "),
                    style = Neu.type.caption,
                    color = c.textMuted,
                    maxLines = 1,
                )
            }
            NeuTextButton("Open playlist", onClick = { vm.openPlaylist(item.id) }, primary = true, minHeight = 38.dp, radius = 12.dp)
        }
    }
}

@Composable
private fun PlaylistView(p: PlaylistState, vm: SearchViewModel) {
    val c = Neu.colors
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            NeuTextButton("Back", onClick = vm::closePlaylist, minHeight = 40.dp, radius = 12.dp)
            NeuText(p.title.ifBlank { "Playlist" }, style = Neu.type.title, maxLines = 1, modifier = Modifier.weight(1f))
        }
        when {
            p.loading -> NeuText("Loading playlist…", color = c.textMuted)
            p.error != null -> NeuText(p.error, color = c.error)
            else -> {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    NeuText("${p.selected.size} / ${p.items.size}", style = Neu.type.mono, color = c.textMuted)
                    NeuTextButton("Select all", onClick = vm::selectAll, minHeight = 38.dp, radius = 12.dp)
                    NeuTextButton("Select none", onClick = vm::selectNone, minHeight = 38.dp, radius = 12.dp)
                    Box(Modifier.weight(1f))
                    NeuSegmented(listOf("Video", "Audio"), if (p.audio) 1 else 0, { vm.setPlaylistAudio(it == 1) })
                    if (p.audio) {
                        NeuSegmented(audioLabels, audioKeys.indexOf(p.audioKey).coerceAtLeast(0), { vm.setPlaylistAudioKey(audioKeys[it]) })
                    } else {
                        NeuSegmented(heightLabels, heightValues.indexOf(p.height).coerceAtLeast(0), { vm.setPlaylistHeight(heightValues[it]) })
                    }
                    NeuTextButton("Download · ${p.selected.size}", onClick = vm::downloadSelected, primary = true, enabled = p.selected.isNotEmpty())
                }
                LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(p.items, key = { it.id }) { item ->
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
    }
}
