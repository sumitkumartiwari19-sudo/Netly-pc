package com.skt.netly.pc.data.library

import com.skt.netly.pc.data.engine.ProcessRunner
import com.skt.netly.pc.platform.AppPaths
import java.io.File
import java.security.MessageDigest

/** Small preview pictures for downloaded files, cut from the file itself with ffmpeg (works offline). */
object ThumbnailCache {
    private fun dir(): File = File(AppPaths.dataDir(), "thumbs")

    fun fileFor(mediaPath: String): File {
        val sha = MessageDigest.getInstance("SHA-1").digest(mediaPath.lowercase().toByteArray(Charsets.UTF_8))
        return File(dir(), sha.joinToString("") { "%02x".format(it) } + ".jpg")
    }

    /** Path of an already made picture, or null. */
    fun existing(mediaPath: String): String? = fileFor(mediaPath).takeIf { it.isFile && it.length() > 0 }?.absolutePath

    /** Makes the picture if needed. Returns its path, or null when the file has no picture (for example most audio files). */
    suspend fun ensure(ffmpeg: File, mediaPath: String): String? {
        existing(mediaPath)?.let { return it }
        if (!ffmpeg.isFile || !File(mediaPath).isFile) return null
        val out = fileFor(mediaPath)
        out.parentFile?.mkdirs()
        val tries = listOf(listOf("-ss", "1"), emptyList())
        for (seek in tries) {
            val cmd = buildList {
                add(ffmpeg.absolutePath); add("-hide_banner"); add("-loglevel"); add("error"); add("-y")
                addAll(seek)
                add("-i"); add(mediaPath)
                add("-an"); add("-frames:v"); add("1")
                add("-vf"); add("scale=320:-2")
                add("-q:v"); add("4")
                add(out.absolutePath)
            }
            runCatching { ProcessRunner.run(cmd, 20_000) }
            if (out.isFile && out.length() > 0) return out.absolutePath
            out.delete()
        }
        return null
    }
}
