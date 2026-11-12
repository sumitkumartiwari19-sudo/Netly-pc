package com.skt.netly.pc.data.settings

import com.skt.netly.pc.data.engine.EngineOptions
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsRepositoryTest {
    private fun file() = File(Files.createTempDirectory("netly-set").toFile(), "settings.json")

    @Test
    fun savesAndReloads() {
        val f = file()
        SettingsRepository(f).update { it.copy(theme = "Dark", maxConcurrent = 3, proxy = "http://1.2.3.4:80", clipboardWatcher = true) }
        val loaded = SettingsRepository(f).state.value
        assertEquals("Dark", loaded.theme)
        assertEquals(3, loaded.maxConcurrent)
        assertEquals("http://1.2.3.4:80", loaded.proxy)
        assertEquals(true, loaded.clipboardWatcher)
    }

    @Test
    fun brokenFileGivesDefaults() {
        val f = file()
        f.writeText("{ nope")
        assertEquals(AppSettings(), SettingsRepository(f).state.value)
    }

    @Test
    fun engineOptionsFollowSettings() {
        val o = EngineOptions.from(AppSettings(speedLimitKb = 2048, cookiesBrowser = "chrome", proxy = " http://p:1 ", nameTemplate = "title_id"))
        assertEquals(listOf("--cookies-from-browser", "chrome", "--proxy", "http://p:1", "--limit-rate", "2048K"), o.networkArgs())
        assertEquals("%(title).120B [%(id)s].%(ext)s", o.template)
        assertEquals(emptyList(), EngineOptions().networkArgs())
    }
}
