package com.skt.netly.pc.platform

import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.io.File
import java.io.IOException
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.channels.OverlappingFileLockException
import java.nio.file.StandardOpenOption
import javax.swing.JFileChooser

/** Lets only one Netly run at a time. The lock is released by the system when the app exits. */
object SingleInstance {
    private var channel: FileChannel? = null
    private var lock: FileLock? = null

    fun acquire(): Boolean = try {
        val dir = AppPaths.dataDir().also { it.mkdirs() }
        val ch = FileChannel.open(File(dir, "app.lock").toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE)
        val l = ch.tryLock()
        if (l == null) {
            ch.close()
            false
        } else {
            channel = ch
            lock = l
            true
        }
    } catch (_: OverlappingFileLockException) {
        false
    } catch (_: IOException) {
        true
    }
}

object StartupRegistry {
    private const val KEY = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run"

    /** Adds or removes Netly from the current user's Windows start-up list. Only works for the installed app. */
    fun set(enabled: Boolean) {
        if (!AppPaths.isWindows) return
        runCatching {
            if (enabled) {
                val exe = ProcessHandle.current().info().command().orElse(null) ?: return
                if (!exe.endsWith("Netly.exe", ignoreCase = true)) return
                ProcessBuilder("reg", "add", KEY, "/v", "Netly", "/t", "REG_SZ", "/d", "\"$exe\"", "/f").start().waitFor()
            } else {
                ProcessBuilder("reg", "delete", KEY, "/v", "Netly", "/f").start().waitFor()
            }
        }
    }
}

object SystemClipboard {
    fun readText(): String? = runCatching {
        val cb = Toolkit.getDefaultToolkit().systemClipboard
        if (cb.isDataFlavorAvailable(DataFlavor.stringFlavor)) cb.getData(DataFlavor.stringFlavor) as? String else null
    }.getOrNull()
}

object FileActions {
    fun open(path: String) {
        runCatching { if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(File(path)) }
    }

    fun showInFolder(path: String?, fallbackDir: String) {
        runCatching {
            val file = path?.let { File(it) }
            if (AppPaths.isWindows && file != null && file.exists()) {
                ProcessBuilder("explorer.exe", "/select," + file.absolutePath).start()
            } else if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(if (file != null && file.exists()) file.parentFile else File(fallbackDir))
            }
        }
    }

    fun delete(path: String?) {
        runCatching { path?.let { File(it).delete() } }
    }

    fun exists(path: String?): Boolean = path != null && File(path).isFile

    /** Shows a folder picker. Returns null when the user cancels. */
    fun chooseFolder(start: String): String? = runCatching {
        val chooser = JFileChooser(File(start).takeIf { it.isDirectory })
        chooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        chooser.dialogTitle = "Choose download folder"
        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) chooser.selectedFile.absolutePath else null
    }.getOrNull()
}
