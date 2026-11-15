package com.skt.netly.pc.data.library

import com.skt.netly.pc.domain.model.DownloadJob
import com.skt.netly.pc.domain.model.DownloadRequest
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LibraryRepositoryTest {
    private val dir: File = Files.createTempDirectory("netly-lib").toFile()
    private val file = File(dir, "library.json")

    private fun request(title: String, audio: Boolean = false) = DownloadRequest(
        videoId = "abcdefghijk", url = "u", title = title, channel = "Chan", thumbnailUrl = "t", durationSec = 90,
        audioOnly = audio, height = 720, audioKey = "mp3-192", container = "MP4",
        embedThumbnail = false, embedSubtitles = false, outputDir = dir.absolutePath,
    )

    @Test
    fun savesCompletedJobAndReloads() {
        val media = File(dir, "Song.mp4").apply { writeText("x") }
        val repo = LibraryRepository(file) { dir }
        repo.addFromJob(DownloadJob(1, request("Song"), filePath = media.absolutePath))
        assertEquals(listOf("Song"), repo.items.value.map { it.title })

        val again = LibraryRepository(file) { dir }
        assertEquals(listOf(media.absolutePath), again.items.value.map { it.path })
    }

    @Test
    fun findsNewestFileWhenJobHasNoPath() {
        File(dir, "Found.mp3").writeText("x")
        val repo = LibraryRepository(file) { dir }
        repo.addFromJob(DownloadJob(2, request("Found", audio = true)))
        assertEquals(1, repo.items.value.size)
        assertTrue(repo.items.value[0].audioOnly)
    }

    @Test
    fun scanAddsUnknownFilesSkipsPartialsAndDropsMissing() {
        File(dir, "A.mp4").writeText("x")
        File(dir, "B.f137.mp4").writeText("x")
        File(dir, "C.part").writeText("x")
        File(dir, "D.mp3").writeText("x")
        val repo = LibraryRepository(file) { dir }
        assertTrue(repo.scan())
        assertEquals(listOf("A", "D"), repo.items.value.map { it.title }.sorted())

        File(dir, "A.mp4").delete()
        repo.scan()
        assertEquals(listOf("D"), repo.items.value.map { it.title })
    }

    @Test
    fun removeForgetsItem() {
        val media = File(dir, "R.mp4").apply { writeText("x") }
        val repo = LibraryRepository(file) { dir }
        repo.addFromJob(DownloadJob(3, request("R"), filePath = media.absolutePath))
        repo.remove(media.absolutePath)
        assertTrue(repo.items.value.isEmpty())
    }
}
