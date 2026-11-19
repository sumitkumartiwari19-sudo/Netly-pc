package com.skt.netly.pc.data.downloads

import com.skt.netly.pc.domain.model.DownloadJob
import com.skt.netly.pc.domain.model.DownloadRequest
import com.skt.netly.pc.domain.model.JobStatus
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

/** Keeps the download list in a small JSON file so it survives restarts. A damaged file is treated as empty. */
class DownloadStore(private val file: File) {

    @Synchronized
    fun load(): List<DownloadJob> {
        if (!file.isFile) return emptyList()
        return try {
            val root = Json.parseToJsonElement(file.readText(Charsets.UTF_8)).jsonObject
            val items = root["jobs"] as? JsonArray ?: return emptyList()
            items.mapNotNull { element -> runCatching { decode(element.jsonObject) }.getOrNull() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** Writes through a temporary file so a crash never leaves half a file behind. */
    @Synchronized
    fun save(jobs: List<DownloadJob>) {
        try {
            file.parentFile?.mkdirs()
            val root = buildJsonObject {
                put("version", 1)
                put("jobs", buildJsonArray { jobs.forEach { add(encode(it)) } })
            }
            val temp = File(file.parentFile, file.name + ".tmp")
            temp.writeText(Json.encodeToString(JsonObject.serializer(), root), Charsets.UTF_8)
            Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
        } catch (_: IOException) {
            // Saving is best-effort; the in-memory list stays correct.
        }
    }

    private fun encode(job: DownloadJob): JsonObject {
        val r = job.request
        return buildJsonObject {
            put("id", job.id)
            put("status", job.status.name)
            put("progress", job.progress)
            put("downloadedBytes", job.downloadedBytes)
            put("totalBytes", job.totalBytes)
            put("filePath", job.filePath)
            put("errorMessage", job.errorMessage)
            put("createdAt", job.createdAt)
            put("videoId", r.videoId)
            put("url", r.url)
            put("title", r.title)
            put("channel", r.channel)
            put("thumbnailUrl", r.thumbnailUrl)
            put("durationSec", r.durationSec)
            put("audioOnly", r.audioOnly)
            put("height", r.height)
            put("audioKey", r.audioKey)
            put("container", r.container)
            put("embedThumbnail", r.embedThumbnail)
            put("embedSubtitles", r.embedSubtitles)
            put("outputDir", r.outputDir)
        }
    }

    private fun decode(o: JsonObject): DownloadJob {
        fun str(key: String) = o[key]?.jsonPrimitive?.contentOrNull
        fun long(key: String) = o[key]?.jsonPrimitive?.longOrNull
        fun bool(key: String) = o[key]?.jsonPrimitive?.booleanOrNull

        val request = DownloadRequest(
            videoId = str("videoId") ?: error("videoId"),
            url = str("url") ?: error("url"),
            title = str("title").orEmpty(),
            channel = str("channel").orEmpty(),
            thumbnailUrl = str("thumbnailUrl").orEmpty(),
            durationSec = long("durationSec") ?: 0L,
            audioOnly = bool("audioOnly") ?: false,
            height = long("height")?.toInt(),
            audioKey = str("audioKey") ?: "mp3-320",
            container = str("container") ?: "MP4",
            embedThumbnail = bool("embedThumbnail") ?: true,
            embedSubtitles = bool("embedSubtitles") ?: false,
            outputDir = str("outputDir") ?: error("outputDir"),
        )
        return DownloadJob(
            id = long("id") ?: error("id"),
            request = request,
            status = runCatching { JobStatus.valueOf(str("status").orEmpty()) }.getOrDefault(JobStatus.Paused),
            progress = o["progress"]?.jsonPrimitive?.floatOrNull ?: 0f,
            downloadedBytes = long("downloadedBytes") ?: 0L,
            totalBytes = long("totalBytes"),
            filePath = str("filePath"),
            errorMessage = str("errorMessage"),
            createdAt = long("createdAt") ?: System.currentTimeMillis(),
        )
    }
}
