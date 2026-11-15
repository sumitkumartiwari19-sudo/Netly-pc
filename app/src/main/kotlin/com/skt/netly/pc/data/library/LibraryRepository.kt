package com.skt.netly.pc.data.library

import com.skt.netly.pc.domain.model.DownloadJob
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

/** One finished file in the Library. The file path is its identity. */
data class LibraryItem(
    val path: String,
    val title: String,
    val channel: String,
    val thumbnailUrl: String,
    val durationSec: Long,
    val audioOnly: Boolean,
    /** Short quality text, e.g. "1080p · MP4" or "MP3 320 kbps". */
    val quality: String,
    val sizeBytes: Long,
    val addedAt: Long,
)

/**
 * The Library is its own list (library.json), so clearing the Downloads list never removes it.
 * [scan] also picks up media files that are in the download folder but were never recorded.
 */
class LibraryRepository(
    private val file: File,
    private val downloadDir: () -> File,
) {
    private val _items = MutableStateFlow(load())
    val items: StateFlow<List<LibraryItem>> = _items.asStateFlow()

    @Synchronized
    fun addFromJob(job: DownloadJob) {
        val r = job.request
        var path = job.filePath?.takeIf { File(it).isFile }
        if (path == null) path = newestMediaFile(File(r.outputDir))?.absolutePath
        if (path == null) return
        val f = File(path)
        upsert(
            LibraryItem(
                path = path,
                title = r.title.ifBlank { f.nameWithoutExtension },
                channel = r.channel,
                thumbnailUrl = r.thumbnailUrl,
                durationSec = r.durationSec,
                audioOnly = r.audioOnly,
                quality = r.qualityLabel,
                sizeBytes = f.length(),
                addedAt = System.currentTimeMillis(),
            ),
        )
    }

    /** Adds unknown media files from the download folder and drops entries whose file is gone. Returns true if anything changed. */
    @Synchronized
    fun scan(): Boolean {
        val current = _items.value
        val kept = current.filter { File(it.path).isFile }
        val known = kept.map { it.path.lowercase() }.toSet()
        val found = listMedia(downloadDir()).filter { it.absolutePath.lowercase() !in known }.map { f ->
            val ext = f.extension.lowercase()
            LibraryItem(
                path = f.absolutePath,
                title = f.nameWithoutExtension,
                channel = "",
                thumbnailUrl = "",
                durationSec = 0,
                audioOnly = ext in AUDIO_EXT,
                quality = ext.uppercase(),
                sizeBytes = f.length(),
                addedAt = f.lastModified(),
            )
        }
        val next = kept + found
        if (next.size == current.size && found.isEmpty()) return false
        _items.value = next
        save(next)
        return true
    }

    @Synchronized
    fun remove(path: String) {
        val next = _items.value.filterNot { it.path == path }
        _items.value = next
        save(next)
    }

    private fun upsert(item: LibraryItem) {
        val next = _items.value.filterNot { it.path.equals(item.path, ignoreCase = true) } + item
        _items.value = next
        save(next)
    }

    private fun newestMediaFile(dir: File): File? {
        val limit = System.currentTimeMillis() - 15 * 60 * 1000
        return listMedia(dir).filter { it.lastModified() >= limit }.maxByOrNull { it.lastModified() }
    }

    private fun load(): List<LibraryItem> {
        if (!file.isFile) return emptyList()
        return try {
            val root = Json.parseToJsonElement(file.readText(Charsets.UTF_8)).jsonObject
            (root["items"] as? JsonArray).orEmpty().mapNotNull { runCatching { decode(it.jsonObject) }.getOrNull() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun save(items: List<LibraryItem>) {
        try {
            file.parentFile?.mkdirs()
            val root = buildJsonObject {
                put("version", 1)
                put("items", buildJsonArray { items.forEach { add(encode(it)) } })
            }
            val temp = File(file.parentFile, file.name + ".tmp")
            temp.writeText(Json.encodeToString(JsonObject.serializer(), root), Charsets.UTF_8)
            Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
        } catch (_: IOException) {
            // Best effort; the in-memory list stays correct.
        }
    }

    companion object {
        val AUDIO_EXT = setOf("mp3", "m4a", "opus", "aac", "wav", "flac", "ogg")
        val VIDEO_EXT = setOf("mp4", "mkv", "webm", "m4v", "mov")
        private val partial = Regex("""\.f\d{2,4}\.[A-Za-z0-9]+$""")

        fun listMedia(dir: File): List<File> =
            dir.listFiles().orEmpty().filter { f ->
                f.isFile && f.extension.lowercase().let { it in AUDIO_EXT || it in VIDEO_EXT } &&
                    !partial.containsMatchIn(f.name) && !f.name.endsWith(".part") && !f.name.contains(".temp.")
            }

        fun encode(i: LibraryItem): JsonObject = buildJsonObject {
            put("path", i.path)
            put("title", i.title)
            put("channel", i.channel)
            put("thumbnailUrl", i.thumbnailUrl)
            put("durationSec", i.durationSec)
            put("audioOnly", i.audioOnly)
            put("quality", i.quality)
            put("sizeBytes", i.sizeBytes)
            put("addedAt", i.addedAt)
        }

        fun decode(o: JsonObject): LibraryItem {
            fun str(k: String) = o[k]?.jsonPrimitive?.contentOrNull
            fun long(k: String) = o[k]?.jsonPrimitive?.longOrNull
            return LibraryItem(
                path = str("path") ?: error("path"),
                title = str("title").orEmpty(),
                channel = str("channel").orEmpty(),
                thumbnailUrl = str("thumbnailUrl").orEmpty(),
                durationSec = long("durationSec") ?: 0L,
                audioOnly = o["audioOnly"]?.jsonPrimitive?.booleanOrNull ?: false,
                quality = str("quality").orEmpty(),
                sizeBytes = long("sizeBytes") ?: 0L,
                addedAt = long("addedAt") ?: 0L,
            )
        }
    }
}
