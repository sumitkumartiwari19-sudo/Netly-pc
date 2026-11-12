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
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
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
import com.skt.netly.pc.data.downloads.DownloadManager
import com.skt.netly.pc.domain.model.DownloadJob
import com.skt.netly.pc.platform.FileActions
import com.skt.netly.pc.ui.components.NetThumbnail
import com.skt.netly.pc.ui.components.NeuLevel
import com.skt.netly.pc.ui.components.NeuSegmented
import com.skt.netly.pc.ui.components.NeuSurface
import com.skt.netly.pc.ui.components.NeuText
import com.skt.netly.pc.ui.components.NeuTextButton
import com.skt.netly.pc.ui.theme.Neu
import com.skt.netly.pc.util.Formatters

@Composable
fun LibraryScreen(manager: DownloadManager, onGoHome: () -> Unit) {
    val c = Neu.colors
    val jobs by manager.jobs.collectAsState()
    var query by remember { mutableStateOf("") }
    var filterIndex by remember { mutableStateOf(0) }
    var sortIndex by remember { mutableStateOf(0) }
    val items = LibraryLogic.items(jobs, query, LibraryFilter.entries[filterIndex], LibrarySort.entries[sortIndex])
    val hasAny = LibraryLogic.items(jobs, "", LibraryFilter.All, LibrarySort.Newest).isNotEmpty()

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Column {
            NeuText("Library", style = Neu.type.display)
            NeuText(
                "Your finished downloads, ready to play offline.",
                modifier = Modifier.padding(top = 6.dp),
                style = Neu.type.body.copy(fontWeight = FontWeight.Medium),
                color = c.textMuted,
            )
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            NeuSurface(Modifier.weight(1f), level = NeuLevel.Inset, radius = 16.dp) {
                Box(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 13.dp), contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) NeuText("Search your downloads…", color = c.textMuted)
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
            NeuSegmented(LibraryFilter.entries.map { it.label }, filterIndex, { filterIndex = it })
            NeuSegmented(LibrarySort.entries.map { it.label }, sortIndex, { sortIndex = it })
        }

        if (items.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    NeuText(if (hasAny) "No matches" else "Your downloads will show up here", style = Neu.type.title)
                    NeuText(if (hasAny) "Try another search or filter." else "Finished videos and music appear in this library.", color = c.textMuted)
                    if (!hasAny) NeuTextButton("Go to Home", onClick = onGoHome, primary = true)
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(280.dp),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(items, key = { it.id }) { job -> LibraryCard(job, manager) }
            }
        }
    }
}

@Composable
private fun LibraryCard(job: DownloadJob, manager: DownloadManager) {
    val c = Neu.colors
    val r = job.request
    var confirmDelete by remember { mutableStateOf(false) }
    val missing = !FileActions.exists(job.filePath)

    NeuSurface(Modifier.fillMaxWidth(), radius = 22.dp) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            NetThumbnail(
                r.thumbnailUrl,
                Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(14.dp)),
                contentDescription = r.title,
            )
            NeuText(r.title, style = Neu.type.label, maxLines = 2)
            NeuText(
                listOf(r.qualityLabel, Formatters.bytes(job.totalBytes).takeIf { it != "—" }, r.channel)
                    .filterNotNull().filter { it.isNotBlank() }.joinToString(" · "),
                style = Neu.type.caption,
                color = c.textMuted,
                maxLines = 1,
            )
            if (missing) NeuText("File not found. It may have been moved or deleted.", style = Neu.type.caption, color = c.error, maxLines = 2)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                NeuTextButton("Play", onClick = { job.filePath?.let { FileActions.open(it) } }, enabled = !missing, primary = true, minHeight = 38.dp, radius = 12.dp)
                NeuTextButton("Folder", onClick = { FileActions.showInFolder(job.filePath, r.outputDir) }, minHeight = 38.dp, radius = 12.dp)
                NeuTextButton(
                    if (confirmDelete) "Sure?" else "Delete",
                    onClick = {
                        if (confirmDelete) {
                            FileActions.delete(job.filePath)
                            manager.remove(job.id)
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
