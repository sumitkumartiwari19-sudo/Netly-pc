package com.skt.netly.pc.player

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.skt.netly.pc.data.engine.EngineOptions
import com.skt.netly.pc.data.engine.EngineRepository
import com.skt.netly.pc.data.engine.ProcessRunner
import java.io.DataInputStream
import java.io.File
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.SourceDataLine
import kotlin.concurrent.thread
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo

private class Frame(val time: Double, val image: ImageBitmap)

/**
 * Plays YouTube streams and local files with the bundled ffmpeg: sound goes to the speakers,
 * pictures are decoded as small frames and shown in sync with the sound.
 * Seeking, speed, quality and music mode restart the session at the current position.
 */
class PlayerEngine(
    private val engine: EngineRepository,
    private val optionsProvider: () -> EngineOptions,
    initial: PlayerState,
    private val onPrefsChanged: (quality: Int, audioOnly: Boolean, autoplay: Boolean) -> Unit = { _, _, _ -> },
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val resolver = StreamResolver(engine, optionsProvider)

    private val _state = MutableStateFlow(initial)
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private val _frame = MutableStateFlow<ImageBitmap?>(null)
    val frame: StateFlow<ImageBitmap?> = _frame.asStateFlow()

    private var session: Session? = null
    private var starting: Job? = null
    private var generation = 0

    @Volatile
    private var gain = initial.volume * initial.volume

    // ---- controls ----

    fun playQueue(items: List<PlayItem>, index: Int) {
        if (items.isEmpty()) return
        _state.update { it.copy(queue = items, index = index.coerceIn(0, items.lastIndex)) }
        startCurrent(0.0)
    }

    suspend fun mediaInfo(url: String) = resolver.mediaInfo(url)

    /** Updates queue pictures (key = item key) without touching playback. */
    fun setThumbnails(map: Map<String, String>) {
        if (map.isEmpty()) return
        _state.update { st -> st.copy(queue = st.queue.map { q -> map[q.key]?.let { q.copy(thumbnailUrl = it) } ?: q }) }
    }

    fun togglePause() {
        val s = _state.value
        when (s.status) {
            PlayStatus.Playing -> {
                session?.pause()
                _state.update { it.copy(status = PlayStatus.Paused) }
            }
            PlayStatus.Paused -> {
                session?.resume()
                _state.update { it.copy(status = PlayStatus.Playing) }
            }
            PlayStatus.Ended, PlayStatus.Error -> startCurrent(if (s.status == PlayStatus.Ended) 0.0 else s.positionSec)
            else -> Unit
        }
    }

    fun seekTo(sec: Double) {
        val s = _state.value
        if (s.item == null) return
        val max = if (s.durationSec > 0) (s.durationSec - 1).toDouble() else Double.MAX_VALUE
        startCurrent(sec.coerceIn(0.0, maxOf(0.0, max)))
    }

    fun seekBy(deltaSec: Double) = seekTo(_state.value.positionSec + deltaSec)

    fun next() {
        val s = _state.value
        if (s.hasNext) jumpTo(s.index + 1)
    }

    fun previous() {
        val s = _state.value
        // Like most players: past the first 4 seconds, "previous" restarts the song.
        if (s.positionSec > 4 || !s.hasPrevious) startCurrent(0.0) else jumpTo(s.index - 1)
    }

    fun jumpTo(index: Int) {
        if (index !in _state.value.queue.indices) return
        _state.update { it.copy(index = index) }
        startCurrent(0.0)
    }

    fun setSpeed(speed: Float) {
        _state.update { it.copy(speed = speed) }
        if (_state.value.item != null && _state.value.status != PlayStatus.Ended) startCurrent(_state.value.positionSec, keepPaused = true)
    }

    fun setQuality(height: Int) {
        _state.update { it.copy(quality = height) }
        persist()
        if (_state.value.item != null && !_state.value.effectiveAudioOnly) startCurrent(_state.value.positionSec, keepPaused = true)
    }

    fun setAudioOnly(on: Boolean) {
        _state.update { it.copy(audioOnly = on) }
        persist()
        if (_state.value.item != null) startCurrent(_state.value.positionSec, keepPaused = true)
    }

    fun setVolume(v: Float) {
        val value = v.coerceIn(0f, 1f)
        gain = value * value
        _state.update { it.copy(volume = value) }
    }

    fun setAutoplay(on: Boolean) {
        _state.update { it.copy(autoplay = on) }
        persist()
    }

    fun stop() {
        generation++
        starting?.cancel()
        session?.close()
        session = null
        _frame.value = null
        _state.update { it.copy(queue = emptyList(), index = 0, status = PlayStatus.Idle, positionSec = 0.0, durationSec = 0, error = null) }
    }

    fun close() {
        stop()
        scope.cancel()
    }

    private fun persist() {
        val s = _state.value
        onPrefsChanged(s.quality, s.audioOnly, s.autoplay)
    }

    // ---- sessions ----

    private fun startCurrent(startSec: Double, keepPaused: Boolean = false) {
        val item = _state.value.item ?: return
        val wasPaused = keepPaused && _state.value.status == PlayStatus.Paused
        val myGeneration = ++generation
        starting?.cancel()
        session?.close()
        session = null
        _state.update {
            it.copy(status = PlayStatus.Loading, positionSec = startSec, error = null, durationSec = item.durationSec)
        }
        if (_state.value.effectiveAudioOnly) _frame.value = null

        starting = scope.launch {
            try {
                val snapshot = _state.value
                val streams = if (item.filePath != null) localStreams(item) else resolver.resolve(item.url ?: error("No address"), snapshot.quality)
                if (myGeneration != generation) return@launch
                if (streams.audioUrl == null) error("No playable stream was found for this video.")
                if (streams.durationSec > 0) _state.update { it.copy(durationSec = streams.durationSec) }
                val ffmpeg = engine.ffmpegFile
                if (!ffmpeg.isFile) error("The built-in player needs ffmpeg, which was not found. Reinstall Netly.")
                val s = Session(
                    ffmpeg = ffmpeg,
                    streams = streams,
                    startSec = startSec,
                    speed = snapshot.speed,
                    quality = snapshot.quality,
                    withVideo = !snapshot.effectiveAudioOnly && streams.videoUrl != null && !item.isAudioFile,
                    proxy = optionsProvider().proxy.trim(),
                    startPaused = wasPaused,
                    generation = myGeneration,
                    item = item,
                )
                if (myGeneration != generation) return@launch
                session = s
                s.start()
                // Warm up the next video's address so "next" starts quickly.
                val nextItem = snapshot.queue.getOrNull(snapshot.index + 1)
                if (nextItem?.url != null) scope.launch { runCatching { resolver.resolve(nextItem.url, snapshot.quality) } }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                if (myGeneration == generation) fail(e.message ?: "Could not play this video.")
            }
        }
    }

    private suspend fun localStreams(item: PlayItem): StreamUrls {
        val path = item.filePath!!
        if (!File(path).isFile) error("This file was not found. It may have been moved or deleted.")
        var duration = item.durationSec
        if (duration <= 0L) {
            duration = runCatching {
                val r = ProcessRunner.run(listOf(engine.ffmpegFile.absolutePath, "-hide_banner", "-i", path), 20_000)
                parseDuration(r.stderr)
            }.getOrDefault(0L)
        }
        return StreamUrls(path, path, duration, null)
    }

    private fun fail(message: String) {
        session?.close()
        session = null
        _state.update { it.copy(status = PlayStatus.Error, error = message) }
    }

    private fun onSessionEnded(gen: Int) {
        if (gen != generation) return
        val s = _state.value
        if (s.autoplay && s.hasNext) {
            jumpTo(s.index + 1)
        } else {
            _state.update { it.copy(status = PlayStatus.Ended, positionSec = it.durationSec.toDouble()) }
        }
    }

    private inner class Session(
        private val ffmpeg: File,
        private val streams: StreamUrls,
        private val startSec: Double,
        private val speed: Float,
        private val quality: Int,
        private val withVideo: Boolean,
        private val proxy: String,
        private val startPaused: Boolean,
        private val generation: Int,
        private val item: PlayItem?,
    ) {
        @Volatile private var closed = false
        @Volatile private var paused = startPaused
        @Volatile private var audioStarted = false
        private var line: SourceDataLine? = null
        private var audioProcess: Process? = null
        private var videoProcess: Process? = null
        private val threads = mutableListOf<Thread>()
        private val frames = ArrayBlockingQueue<Frame>(3)
        private val rate = 44_100f

        private val clock: Double
            get() = line?.let { startSec + it.longFramePosition / rate.toDouble() * speed } ?: startSec

        fun start() {
            val format = AudioFormat(rate, 16, 2, true, false)
            val l = AudioSystem.getLine(DataLine.Info(SourceDataLine::class.java, format)) as SourceDataLine
            l.open(format, (rate * 4 * 0.5).toInt())
            line = l
            if (!startPaused) l.start()

            threads += thread(isDaemon = true, name = "netly-audio") { runAudio(l) }
            if (withVideo) {
                threads += thread(isDaemon = true, name = "netly-video") { runVideo() }
                threads += thread(isDaemon = true, name = "netly-render") { runRender() }
            }
            threads += thread(isDaemon = true, name = "netly-clock") { runClock() }
        }

        fun pause() {
            paused = true
            line?.stop()
        }

        fun resume() {
            paused = false
            line?.start()
        }

        fun close() {
            closed = true
            runCatching { audioProcess?.destroyForcibly() }
            runCatching { videoProcess?.destroyForcibly() }
            runCatching { line?.stop(); line?.flush(); line?.close() }
            threads.forEach { it.interrupt() }
        }

        private fun inputArgs(url: String): List<String> = buildList {
            addAll(listOf("-hide_banner", "-loglevel", "error", "-nostdin", "-probesize", "1500000", "-analyzeduration", "1000000"))
            if (url.startsWith("http")) {
                streams.userAgent?.let { addAll(listOf("-user_agent", it)) }
                addAll(listOf("-reconnect", "1", "-reconnect_streamed", "1", "-reconnect_delay_max", "5"))
                if (proxy.isNotBlank() && proxy.startsWith("http")) addAll(listOf("-http_proxy", proxy))
            }
            if (startSec > 0.5) addAll(listOf("-ss", "%.2f".format(java.util.Locale.ROOT, startSec)))
            addAll(listOf("-i", url))
        }

        private fun tempoFilter(): String = when {
            speed > 2f -> "atempo=2.0,atempo=${speed / 2f}"
            else -> "atempo=$speed"
        }

        private fun runAudio(l: SourceDataLine) {
            try {
                val cmd = listOf(ffmpeg.absolutePath) + inputArgs(streams.audioUrl!!) +
                    listOf("-vn", "-af", tempoFilter(), "-f", "s16le", "-ac", "2", "-ar", "44100", "-")
                val p = ProcessBuilder(cmd).start()
                audioProcess = p
                val errors = drainErrors(p)
                val input = p.inputStream
                val buffer = ByteArray(8192)
                var gotData = false
                while (!closed) {
                    val n = input.readNBytes(buffer, 0, buffer.size)
                    if (n <= 0) break
                    if (!gotData) {
                        gotData = true
                        audioStarted = true
                        if (!paused) _state.update { it.copy(status = PlayStatus.Playing) }
                        else _state.update { it.copy(status = PlayStatus.Paused) }
                    }
                    applyGain(buffer, n)
                    l.write(buffer, 0, n - n % 4)
                }
                if (closed) return
                if (!gotData && streams.native && item?.url != null) {
                    // The direct address was refused: switch to yt-dlp for this video and try again.
                    val u = item.url
                    resolver.disableNative(u)
                    if (!closed) startCurrent(startSec, keepPaused = startPaused)
                    return
                }
                if (!gotData) {
                    val reason = errors().lineSequence().lastOrNull { it.isNotBlank() }?.take(140)
                    fail(if (reason.isNullOrBlank()) "This video could not be played. Try another quality." else "Could not play: $reason")
                    return
                }
                l.drain()
                if (!closed) onSessionEnded(generation)
            } catch (_: InterruptedException) {
                // Session closed.
            } catch (e: Exception) {
                if (!closed) fail("Playback stopped: ${e.message ?: "unknown error"}")
            }
        }

        private fun applyGain(buf: ByteArray, n: Int) {
            val g = gain
            if (g >= 0.999f) return
            var i = 0
            while (i + 1 < n) {
                val sample = ((buf[i + 1].toInt() shl 8) or (buf[i].toInt() and 0xFF)).toShort().toInt()
                val scaled = (sample * g).toInt()
                buf[i] = (scaled and 0xFF).toByte()
                buf[i + 1] = ((scaled shr 8) and 0xFF).toByte()
                i += 2
            }
        }

        private fun drainErrors(p: Process): () -> String {
            val sb = StringBuilder()
            thread(isDaemon = true) {
                runCatching {
                    p.errorStream.bufferedReader().useLines { lines -> lines.forEach { synchronized(sb) { if (sb.length < 4000) sb.appendLine(it) } } }
                }
            }
            return { synchronized(sb) { sb.toString() } }
        }

        private fun runVideo() {
            try {
                val (w, h) = sizeFor(quality)
                val fps = (24f / speed).coerceIn(4f, 30f)
                val filter = "fps=$fps,scale=$w:$h:force_original_aspect_ratio=decrease:flags=fast_bilinear," +
                    "pad=$w:$h:(ow-iw)/2:(oh-ih)/2"
                val cmd = listOf(ffmpeg.absolutePath) + inputArgs(streams.videoUrl!!) +
                    listOf("-an", "-vf", filter, "-pix_fmt", "bgra", "-f", "rawvideo", "-")
                val p = ProcessBuilder(cmd).start()
                videoProcess = p
                drainErrors(p)
                val input = DataInputStream(p.inputStream.buffered(w * h * 4 * 2))
                val size = w * h * 4
                var i = 0
                while (!closed) {
                    val bytes = ByteArray(size)
                    try {
                        input.readFully(bytes)
                    } catch (_: java.io.EOFException) {
                        break
                    }
                    val info = ImageInfo(w, h, ColorType.BGRA_8888, ColorAlphaType.OPAQUE)
                    val bitmap = Image.makeRaster(info, bytes, w * 4).toComposeImageBitmap()
                    val frame = Frame(startSec + i / fps.toDouble(), bitmap)
                    i++
                    while (!closed && !frames.offer(frame, 100, TimeUnit.MILLISECONDS)) Unit
                }
            } catch (_: InterruptedException) {
                // Session closed.
            } catch (_: Exception) {
                // Without pictures the sound keeps playing.
            }
        }

        private fun runRender() {
            try {
                while (!closed) {
                    var frame = frames.poll(100, TimeUnit.MILLISECONDS) ?: continue
                    while (!closed) {
                        val now = clock
                        if (!audioStarted || paused) {
                            // Show the first picture right away, then wait for the sound.
                            if (_frame.value == null || !audioStarted) _frame.value = frame.image
                            Thread.sleep(40)
                            if (!audioStarted || paused) continue
                        }
                        val wait = frame.time - now
                        if (wait > 0.012) {
                            Thread.sleep(minOf((wait / speed * 1000).toLong().coerceAtLeast(1L), 40L))
                            continue
                        }
                        // Late frames are skipped so pictures never fall behind the sound.
                        val nextTime = frames.peek()?.time
                        if (nextTime != null && nextTime <= now) {
                            frame = frames.poll() ?: frame
                            continue
                        }
                        _frame.value = frame.image
                        break
                    }
                }
            } catch (_: InterruptedException) {
                // Session closed.
            }
        }

        private fun runClock() {
            try {
                while (!closed) {
                    if (audioStarted) {
                        val pos = clock
                        _state.update { s ->
                            val dur = s.durationSec.toDouble()
                            s.copy(positionSec = if (dur > 0) minOf(pos, dur) else pos)
                        }
                    }
                    Thread.sleep(250)
                }
            } catch (_: InterruptedException) {
                // Session closed.
            }
        }
    }

    companion object {
        fun sizeFor(height: Int): Pair<Int, Int> = when {
            height <= 144 -> 256 to 144
            height <= 240 -> 426 to 240
            height <= 360 -> 640 to 360
            height <= 480 -> 854 to 480
            else -> 1280 to 720
        }

        private val durationRegex = Regex("""Duration:\s*(\d+):(\d+):(\d+(?:\.\d+)?)""")

        fun parseDuration(text: String): Long {
            val m = durationRegex.find(text) ?: return 0L
            val (h, min, s) = m.destructured
            return (h.toLong() * 3600 + min.toLong() * 60 + s.toDouble()).toLong()
        }
    }
}
