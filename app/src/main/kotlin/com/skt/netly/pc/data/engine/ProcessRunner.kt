package com.skt.netly.pc.data.engine

import java.io.IOException
import java.io.InputStream
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext

data class ProcessResult(val exitCode: Int, val stdout: String, val stderr: String)

class ProcessTimeoutException : Exception("Process timed out")

/** Runs an external program with an argument list (never a shell string) and collects its output. */
object ProcessRunner {
    private const val MAX_STDOUT_CHARS = 30_000_000
    private const val MAX_STDERR_CHARS = 200_000

    suspend fun run(command: List<String>, timeoutMs: Long): ProcessResult = withContext(Dispatchers.IO) {
        val builder = ProcessBuilder(command)
        builder.environment()["PYTHONIOENCODING"] = "utf-8"
        val process = builder.start()
        runCatching { process.outputStream.close() }

        val out = StringBuilder()
        val err = StringBuilder()
        val outThread = pump(process.inputStream, out, MAX_STDOUT_CHARS)
        val errThread = pump(process.errorStream, err, MAX_STDERR_CHARS)

        try {
            val finished = runInterruptible { process.waitFor(timeoutMs, TimeUnit.MILLISECONDS) }
            if (!finished) throw ProcessTimeoutException()
            outThread.join(3000)
            errThread.join(3000)
            val stdout = synchronized(out) { out.toString() }
            val stderr = synchronized(err) { err.toString() }
            ProcessResult(process.exitValue(), stdout, stderr)
        } finally {
            if (process.isAlive) process.destroyForcibly()
        }
    }

    private fun pump(stream: InputStream, sink: StringBuilder, maxChars: Int): Thread =
        thread(isDaemon = true) {
            try {
                stream.bufferedReader(Charsets.UTF_8).use { reader ->
                    val buffer = CharArray(8192)
                    while (true) {
                        val read = reader.read(buffer)
                        if (read < 0) break
                        synchronized(sink) {
                            if (sink.length < maxChars) sink.append(buffer, 0, read)
                        }
                    }
                }
            } catch (_: IOException) {
                // Stream closed because the process ended or was cancelled.
            }
        }
}
