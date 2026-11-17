package com.skt.netly.pc.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.skt.netly.pc.data.library.LibraryItem
import com.skt.netly.pc.data.library.LibraryRepository
import com.skt.netly.pc.platform.FileActions
import com.skt.netly.pc.ui.components.NetThumbnail
import com.skt.netly.pc.ui.components.NeuLevel
import com.skt.netly.pc.ui.components.NeuSegmented
import com.skt.netly.pc.ui.components.NeuSurface
import com.skt.netly.pc.ui.components.NeuText
import com.skt.netly.pc.ui.components.NeuTextButton
import com.skt.netly.pc.ui.theme.Neu
import com.skt.netly.pc.util.Formatters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun LibraryScreen(repo: LibraryRepository, onPlay: (List<LibraryItem>, Int) -> Unit, onGoHome: () -> Unit) {
    val c = Neu.colors
    val all by repo.items.collectAsState()
    LaunchedEffect(Unit) { withContext(Dispatchers.IO) { repo.scan() } }
    var query by remember { mutableStateOf("") }
    var filterIndex by remember { mutableStateOf(0) }
    var sortIndex by remember { mutableStateOf(0) }
    val items = LibraryLogic.items(all, query, LibraryFilter.entries[filterIndex], LibrarySort.entries[sortIndex])
    val hasAny = LibraryLogic.items(all, "", LibraryFilter.All, LibrarySort.Newest).isNotEmpty()

    LazyVerticalGrid(
        columns = GridCells.Adaptive(260.dp),
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                NeuText("Library", style = Neu.type.display)
                NeuText(
                    "Your finished downloads, ready to play offline.",
                    modifier = Modifier.padding(top = 6.dp),
                    style = Neu.type.body.copy(fontWeight = FontWeight.Medium),
                    color = c.textMuted,
                )
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            NeuSurface(Modifier.fillMaxWidth(), level = NeuLevel.Inset, radius = 16.dp) {
                Box(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 13.dp), contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) NeuText("Search your downloads…", color = c.textMuted, maxLines = 1)
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = Neu.type.body.copy(color = c.text),
                        cursorBrush = SolidColor(c.accentText),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                NeuSegmented(LibraryFilter.entries.map { it.label }, filterIndex, { filterIndex = it })
                NeuSegmented(LibrarySort.entries.map { it.label }, sortIndex, { sortIndex = it })
            }
        }
        if (items.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(Modifier.fillMaxWidth().padding(top = 32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        NeuText(if (hasAny) "No matches" else "Your downloads will show up here", style = Neu.type.title)
                        NeuText(if (hasAny) "Try another search or filter." else "Finished videos and music appear in this library.", color = c.textMuted)
                        if (!hasAny) NeuTextButton("Go to Home", onClick = onGoHome, primary = true)
                    }
                }
            }
        } else {
            items(items, key = { it.path }) { item -> LibraryCard(item, repo, onPlay = { onPlay(items, items.indexOf(item)) }) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LibraryCard(item: LibraryItem, repo: LibraryRepository, onPlay: () -> Unit) {
    val c = Neu.colors
    var confirmDelete by remember { mutableStateOf(false) }
    val missing = !FileActions.exists(item.path)

    NeuSurface(Modifier.fillMaxWidth(), radius = 22.dp) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (item.thumbnailUrl.isNotBlank()) {
                NetThumbnail(
                    item.thumbnailUrl,
                    Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(14.dp)),
                    contentDescription = item.title,
                )
            } else {
                NeuSurface(Modifier.fillMaxWidth().aspectRatio(16f / 9f), level = NeuLevel.Inset, radius = 14.dp) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        NeuText(if (item.audioOnly) "Audio" else "Video", style = Neu.type.title, color = c.textMuted)
                    }
                }
            }
            NeuText(item.title, style = Neu.type.label, maxLines = 2)
            NeuText(
                listOf(item.quality, Formatters.bytes(item.sizeBytes).takeIf { it != "—" }, item.channel)
                    .filterNotNull().filter { it.isNotBlank() }.joinToString(" · "),
                style = Neu.type.caption,
                color = c.textMuted,
                maxLines = 1,
            )
            if (missing) NeuText("File not found. It may have been moved or deleted.", style = Neu.type.caption, color = c.error, maxLines = 2)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                NeuTextButton("Play", onClick = onPlay, enabled = !missing, primary = true, minHeight = 38.dp, radius = 12.dp)
                NeuTextButton("Folder", onClick = { FileActions.showInFolder(item.path, java.io.File(item.path).parent.orEmpty()) }, minHeight = 38.dp, radius = 12.dp)
                NeuTextButton(
                    if (confirmDelete) "Sure?" else "Delete",
                    onClick = {
                        if (confirmDelete) {
                            FileActions.delete(item.path)
                            repo.remove(item.path)
                        } else {
                            confirmDelete = true
                        }
                    },
                    selected = confirmDelete,
                    minHeight = 38.dp,
                    radius = 12.dp,
                )
            }
        }
    }
}
