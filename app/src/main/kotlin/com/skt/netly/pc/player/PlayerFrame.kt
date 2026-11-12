package com.skt.netly.pc.player

import com.skt.netly.pc.domain.model.PlayRequest
import java.awt.Dimension
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.File
import javafx.application.Platform
import javafx.embed.swing.JFXPanel
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.ComboBox
import javafx.scene.control.Label
import javafx.scene.control.Slider
import javafx.scene.input.KeyCode
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import javafx.scene.media.MediaView
import javafx.scene.paint.Color
import javafx.util.Duration
import javax.swing.JFrame
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * A simple video window: play and pause, seek bar, volume, speed from 0.5x to 4x and quality (for YouTube videos).
 * Plays H.264/AAC MP4 files and 360p streams; HD is prepared first because YouTube serves HD as separate video and sound.
 */
class PlayerFrame(private val service: PlayerService, private val request: PlayRequest) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val frame = JFrame(request.title.take(90) + " – Netly")
    private val panel = JFXPanel()

    private var player: MediaPlayer? = null
    private var loadJob: Job? = null
    private var rate = 1.0
    private var closed = false

    private val mediaView = MediaView()
    private val status = Label("")
    private val playButton = Button("Pause")
    private val seek = Slider(0.0, 1.0, 0.0)
    private val timeLabel = Label("0:00 / 0:00")
    private val speedBox = ComboBox<String>()
    private val qualityBox = ComboBox<String>()
    private val volume = Slider(0.0, 1.0, 0.8)

    private val speeds = listOf(0.5, 0.75, 1.0, 1.25, 1.5, 2.0, 3.0, 4.0)
    private val speedLabels = listOf("0.5x", "0.75x", "1x", "1.25x", "1.5x", "2x", "3x", "4x")
    private val qualityLabels = listOf("360p (stream)", "720p (HD)", "1080p (HD)")

    /** Call on the Swing thread. */
    fun show() {
        frame.defaultCloseOperation = JFrame.DO_NOTHING_ON_CLOSE
        frame.addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent) = close()
        })
        panel.preferredSize = Dimension(980, 600)
        frame.contentPane.add(panel)
        frame.pack()
        frame.setLocationRelativeTo(null)
        frame.isVisible = true
        Platform.setImplicitExit(false)
        Platform.runLater {
            buildScene()
            load(if (request.filePath != null) null else 0, resumeAt = 0.0)
        }
    }

    private fun buildScene() {
        val darkText = "-fx-text-fill: white;"
        mediaView.isPreserveRatio = true
        status.style = "-fx-text-fill: white; -fx-font-size: 15px; -fx-background-color: rgba(0,0,0,0.55); -fx-padding: 8 14 8 14;"
        status.isWrapText = true
        status.maxWidth = 640.0

        val videoBox = StackPane(mediaView, status)
        videoBox.style = "-fx-background-color: black;"
        videoBox.minWidth = 0.0
        videoBox.minHeight = 0.0

        playButton.setOnAction { togglePlay() }
        timeLabel.style = darkText

        speedBox.items.addAll(speedLabels)
        speedBox.value = "1x"
        speedBox.setOnAction {
            val i = speedBox.selectionModel.selectedIndex
            if (i >= 0) {
                rate = speeds[i]
                player?.rate = rate
            }
        }

        if (request.filePath == null) {
            qualityBox.items.addAll(qualityLabels)
            qualityBox.value = qualityLabels[0]
            qualityBox.setOnAction {
                val i = qualityBox.selectionModel.selectedIndex
                if (i >= 0) load(i, resumeAt = player?.currentTime?.toSeconds() ?: 0.0)
            }
        }

        volume.prefWidth = 90.0
        volume.valueProperty().addListener { _, _, v -> player?.volume = v.toDouble() }

        seek.setOnMouseReleased { player?.seek(Duration.seconds(seek.value)) }

        val spacer = Region()
        HBox.setHgrow(spacer, Priority.ALWAYS)
        val speedLabel = Label("Speed").also { it.style = darkText }
        val volLabel = Label("Vol").also { it.style = darkText }
        val row = HBox(10.0, playButton, timeLabel, spacer, speedLabel, speedBox)
        if (request.filePath == null) {
            row.children.addAll(Label("Quality").also { it.style = darkText }, qualityBox)
        }
        row.children.addAll(volLabel, volume)
        row.alignment = Pos.CENTER_LEFT

        val controls = VBox(8.0, seek, row)
        controls.padding = Insets(10.0)
        controls.style = "-fx-background-color: #15181D;"

        val root = BorderPane()
        root.center = videoBox
        root.bottom = controls
        val scene = Scene(root, 980.0, 600.0, Color.BLACK)
        mediaView.fitWidthProperty().bind(scene.widthProperty())
        mediaView.fitHeightProperty().bind(scene.heightProperty().subtract(controls.heightProperty()))
        scene.setOnKeyPressed { e ->
            when (e.code) {
                KeyCode.SPACE -> togglePlay()
                KeyCode.LEFT -> player?.let { it.seek(it.currentTime.subtract(Duration.seconds(5.0))) }
                KeyCode.RIGHT -> player?.let { it.seek(it.currentTime.add(Duration.seconds(5.0))) }
                else -> Unit
            }
        }
        panel.scene = scene
    }

    /** [quality]: null for a local file, 0 for the 360p stream, 1 and 2 for 720p and 1080p. */
    private fun load(quality: Int?, resumeAt: Double) {
        loadJob?.cancel()
        loadJob = scope.launch {
            try {
                setStatus("Loading…")
                val address: String = when {
                    request.filePath != null -> File(request.filePath).toURI().toString()
                    quality == 0 || quality == null -> service.resolveStream(request.videoUrl.orEmpty())
                        ?: throw IllegalStateException("YouTube gave no stream for this video.")
                    else -> {
                        val height = if (quality == 1) 720 else 1080
                        val path = service.prepareHd(request, height) { pct -> setStatus("Preparing $height" + "p… $pct%") }
                            ?: throw IllegalStateException("Could not prepare this quality.")
                        File(path).toURI().toString()
                    }
                }
                Platform.runLater { if (!closed) startMedia(address, resumeAt) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                setStatus("Could not play this video. ${e.message ?: ""}".trim())
            }
        }
    }

    private fun startMedia(address: String, resumeAt: Double) {
        player?.let {
            it.stop()
            it.dispose()
        }
        val media = Media(address)
        val p = MediaPlayer(media)
        player = p
        p.volume = volume.value
        p.setOnReady {
            seek.max = media.duration.toSeconds()
            if (resumeAt > 0.0) p.seek(Duration.seconds(resumeAt))
            p.rate = rate
            p.play()
            playButton.text = "Pause"
            setStatus("")
        }
        p.setOnError { setStatus("Playback problem: ${p.error?.message ?: "unknown"}. Try another quality or download the video.") }
        p.setOnEndOfMedia { playButton.text = "Play" }
        p.currentTimeProperty().addListener { _, _, t ->
            if (!seek.isPressed && !seek.isValueChanging) seek.value = t.toSeconds()
            timeLabel.text = clock(t.toSeconds()) + " / " + clock(media.duration.toSeconds())
        }
        mediaView.mediaPlayer = p
    }

    private fun togglePlay() {
        val p = player ?: return
        if (p.status == MediaPlayer.Status.PLAYING) {
            p.pause()
            playButton.text = "Play"
        } else {
            if (p.currentTime >= p.totalDuration && p.totalDuration.greaterThan(Duration.ZERO)) p.seek(Duration.ZERO)
            p.play()
            p.rate = rate
            playButton.text = "Pause"
        }
    }

    private fun setStatus(text: String) {
        Platform.runLater {
            status.text = text
            status.isVisible = text.isNotEmpty()
        }
    }

    private fun clock(totalSeconds: Double): String {
        if (totalSeconds.isNaN() || totalSeconds < 0) return "0:00"
        val t = totalSeconds.toLong()
        val h = t / 3600
        val m = (t % 3600) / 60
        val s = t % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }

    private fun close() {
        closed = true
        scope.cancel()
        Platform.runLater {
            player?.let {
                it.stop()
                it.dispose()
            }
            player = null
        }
        frame.dispose()
    }
}
