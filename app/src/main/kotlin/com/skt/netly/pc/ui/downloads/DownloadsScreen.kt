package com.skt.netly.pc.ui.downloads

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.skt.netly.pc.data.downloads.DownloadManager
import com.skt.netly.pc.domain.model.DownloadJob
import com.skt.netly.pc.domain.model.JobStatus
import com.skt.netly.pc.ui.components.NetThumbnail
import com.skt.netly.pc.ui.components.NeuLevel
import com.skt.netly.pc.ui.components.NeuProgress
import com.skt.netly.pc.ui.components.NeuSegmented
import com.skt.netly.pc.ui.components.NeuSurface
import com.skt.netly.pc.ui.components.NeuText
import com.skt.netly.pc.ui.components.NeuTextButton
import com.skt.netly.pc.ui.theme.Neu
import com.skt.netly.pc.util.Formatters
import java.awt.Desktop
import java.io.File

@Composable
fun DownloadsScreen(manager: DownloadManager, onGoHome: () -> Unit) {
    val c = Neu.colors
    val jobs by manager.jobs.collectAsState()
    var tabIndex by remember { mutableStateOf(0) }
    val tabs = DownloadTab.entries
    val tab = tabs[tabIndex]
    val summary = DownloadsLogic.summary(jobs)
    val visible = DownloadsLogic.visible(jobs, tab)

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                NeuText("Downloads", style = Neu.type.display)
                NeuText(
                    "Everything Netly is fetching right now.",
                    modifier = Modifier.padding(top = 6.dp),
                    style = Neu.type.body.copy(fontWeight = FontWeight.Medium),
                    color = c.textMuted,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NeuTextButton("Pause all", onClick = manager::pauseAll, enabled = jobs.any { it.isRunning || it.status == JobStatus.Queued })
                NeuTextButton("Resume all", onClick = manager::resumeAll, enabled = jobs.any { it.status == JobStatus.Paused })
                NeuTextButton("Clear completed", onClick = manager::clearCompleted, enabled = summary.completed > 0)
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            SummaryCard("Total speed", if (summary.active > 0) Formatters.bytes(summary.totalSpeed) + "/s" else "—", Modifier.weight(1f))
            SummaryCard("Active", summary.active.toString(), Modifier.weight(1f))
            SummaryCard("Completed", summary.completed.toString(), Modifier.weight(1f))
        }

        NeuSegmented(
            options = tabs.map { t -> "${t.label} (${jobs.count { t.matches(it) }})" },
            selectedIndex = tabIndex,
            onSelect = { tabIndex = it },
        )

        if (visible.isEmpty()) {
            EmptyState(hasAny = jobs.isNotEmpty(), onGoHome = onGoHome)
        } else {
            LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(visible, key = { it.id }) { job -> JobRow(job, manager) }
            }
        }
    }
}

@Composable
private fun SummaryCard(label: String, value: String, modifier: Modifier) {
    NeuSurface(modifier = modifier, radius = 22.dp) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            NeuText(label.uppercase(), style = Neu.type.overline, color = Neu.colors.textMuted)
            NeuText(value, style = Neu.type.title)
        }
    }
}

@Composable
private fun EmptyState(hasAny: Boolean, onGoHome: () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            NeuText(if (hasAny) "Nothing in this tab" else "Nothing downloading", style = Neu.type.title)
            NeuText(
                if (hasAny) "Try another tab." else "Paste a YouTube link on Home to start your first download.",
                color = Neu.colors.textMuted,
            )
            if (!hasAny) NeuTextButton("Go to Home", onClick = onGoHome, primary = true)
        }
    }
}

@Composable
private fun JobRow(job: DownloadJob, manager: DownloadManager) {
    val c = Neu.colors
    val r = job.request
    val failed = job.status == JobStatus.Failed
    val statusColor = when (job.status) {
        JobStatus.Failed -> c.error
        JobStatus.Completed, JobStatus.Merging -> c.success
        else -> c.textMuted
    }
    NeuSurface(Modifier.fillMaxWidth(), level = NeuLevel.Raised, radius = 22.dp) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            NetThumbnail(
                r.thumbnailUrl,
                Modifier.width(112.dp).height(63.dp).clip(RoundedCornerShape(12.dp)),
                contentDescription = r.title,
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                NeuText(r.title, style = Neu.type.label, maxLines = 1)
                NeuText(
                    listOf(r.channel, r.qualityLabel, Formatters.duration(r.durationSec)).filter { it.isNotBlank() }.joinToString(" · "),
                    style = Neu.type.caption,
                    color = c.textMuted,
                    maxLines = 1,
                )
                NeuProgress(
                    progress = job.progress,
                    modifier = Modifier.fillMaxWidth(),
                    color = when {
                        failed -> c.error
                        job.status == JobStatus.Merging || job.status == JobStatus.Completed -> c.success
                        else -> c.accent
                    },
                )
                NeuText(DownloadsLogic.statusText(job), style = Neu.type.caption, color = statusColor, maxLines = 2)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                when (job.status) {
                    JobStatus.Downloading, JobStatus.Queued -> NeuTextButton("Pause", onClick = { manager.pause(job.id) })
                    JobStatus.Paused -> NeuTextButton("Resume", onClick = { manager.resume(job.id) }, primary = true)
                    JobStatus.Failed -> NeuTextButton("Retry", onClick = { manager.resume(job.id) }, primary = true)
                    JobStatus.Completed -> NeuTextButton("Show in folder", onClick = { showInFolder(job) })
                    JobStatus.Merging -> Unit
                }
                NeuTextButton(
                    if (job.status == JobStatus.Completed || failed) "Remove" else "Cancel",
                    onClick = { manager.remove(job.id) },
                )
            }
        }
    }
}

private fun showInFolder(job: DownloadJob) {
    runCatching {
        val file = job.filePath?.let { File(it) }
        val dir = if (file != null && file.exists()) file.parentFile else File(job.request.outputDir)
        if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(dir)
    }
}
