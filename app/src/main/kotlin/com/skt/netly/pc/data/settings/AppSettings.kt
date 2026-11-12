package com.skt.netly.pc.data.settings

import com.skt.netly.pc.platform.AppPaths
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

data class AppSettings(
    /** System, Light or Dark. */
    val theme: String = "System",
    val downloadDir: String = AppPaths.defaultDownloadDir().absolutePath,
    /** Preferred maximum video height; 0 means best available. */
    val defaultHeight: Int = 1080,
    val maxConcurrent: Int = 2,
    /** KB/s; 0 means unlimited. */
    val speedLimitKb: Int = 0,
    /** title, channel_title or title_id. */
    val nameTemplate: String = "title",
    /** none, chrome, edge, firefox or brave. */
    val cookiesBrowser: String = "none",
    val proxy: String = "",
    val autoUpdateEngine: Boolean = true,
    val clipboardWatcher: Boolean = false,
    val notifications: Boolean = true,
    val minimizeToTray: Boolean = true,
    val startWithWindows: Boolean = false,
)

/** Holds the settings in memory and keeps them in settings.json. A damaged file falls back to defaults. */
class SettingsRepository(private val file: File) {
    private val _state = MutableStateFlow(load())
    val state: StateFlow<AppSettings> = _state.asStateFlow()

    @Synchronized
    fun update(change: (AppSettings) -> AppSettings) {
        val next = change(_state.value)
        _state.value = next
        save(next)
    }

    private fun load(): AppSettings {
        if (!file.isFile) return AppSettings()
        return try {
            decode(Json.parseToJsonElement(file.readText(Charsets.UTF_8)).jsonObject)
        } catch (_: Exception) {
            AppSettings()
        }
    }

    private fun save(s: AppSettings) {
        try {
            file.parentFile?.mkdirs()
            val temp = File(file.parentFile, file.name + ".tmp")
            temp.writeText(Json.encodeToString(JsonObject.serializer(), encode(s)), Charsets.UTF_8)
            Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
        } catch (_: IOException) {
            // Best effort; the in-memory settings stay correct.
        }
    }

    companion object {
        fun encode(s: AppSettings): JsonObject = buildJsonObject {
            put("theme", s.theme)
            put("downloadDir", s.downloadDir)
            put("defaultHeight", s.defaultHeight)
            put("maxConcurrent", s.maxConcurrent)
            put("speedLimitKb", s.speedLimitKb)
            put("nameTemplate", s.nameTemplate)
            put("cookiesBrowser", s.cookiesBrowser)
            put("proxy", s.proxy)
            put("autoUpdateEngine", s.autoUpdateEngine)
            put("clipboardWatcher", s.clipboardWatcher)
            put("notifications", s.notifications)
            put("minimizeToTray", s.minimizeToTray)
            put("startWithWindows", s.startWithWindows)
        }

        fun decode(o: JsonObject): AppSettings {
            val d = AppSettings()
            fun str(k: String, def: String) = o[k]?.jsonPrimitive?.contentOrNull ?: def
            fun int(k: String, def: Int) = o[k]?.jsonPrimitive?.intOrNull ?: def
            fun bool(k: String, def: Boolean) = o[k]?.jsonPrimitive?.booleanOrNull ?: def
            return AppSettings(
                theme = str("theme", d.theme),
                downloadDir = str("downloadDir", d.downloadDir).ifBlank { d.downloadDir },
                defaultHeight = int("defaultHeight", d.defaultHeight),
                maxConcurrent = int("maxConcurrent", d.maxConcurrent).coerceIn(1, 6),
                speedLimitKb = int("speedLimitKb", d.speedLimitKb).coerceAtLeast(0),
                nameTemplate = str("nameTemplate", d.nameTemplate),
                cookiesBrowser = str("cookiesBrowser", d.cookiesBrowser),
                proxy = str("proxy", d.proxy),
                autoUpdateEngine = bool("autoUpdateEngine", d.autoUpdateEngine),
                clipboardWatcher = bool("clipboardWatcher", d.clipboardWatcher),
                notifications = bool("notifications", d.notifications),
                minimizeToTray = bool("minimizeToTray", d.minimizeToTray),
                startWithWindows = bool("startWithWindows", d.startWithWindows),
            )
        }
    }
}
