package com.skt.netly.pc.platform

import com.skt.netly.pc.AppInfo
import java.awt.Desktop
import java.net.URI
import java.net.URLEncoder

/** Opens web pages and the mail app. Only https links to known sites are allowed. */
object Links {
    private val allowedHosts = setOf(
        "github.com", "www.github.com", "www.instagram.com", "instagram.com", "developer-sumit.netlify.app",
    )

    fun isAllowed(url: String): Boolean = runCatching {
        val u = URI(url)
        u.scheme == "https" && u.host?.lowercase() in allowedHosts
    }.getOrDefault(false)

    fun open(url: String) {
        if (!isAllowed(url)) return
        runCatching { if (Desktop.isDesktopSupported()) Desktop.getDesktop().browse(URI(url)) }
    }

    fun mailtoUri(to: String, subject: String, body: String): String {
        fun enc(v: String) = URLEncoder.encode(v, Charsets.UTF_8).replace("+", "%20")
        return "mailto:$to?subject=${enc(subject)}&body=${enc(body)}"
    }

    /** Opens the mail app with a ready feedback message (app version and Windows version only, nothing personal). */
    fun feedbackMail() {
        val body = "Hi Sumit,\n\n\n\n--\nNetly ${AppInfo.VERSION}\n${System.getProperty("os.name")} ${System.getProperty("os.version")}\n"
        val uri = mailtoUri(AppInfo.DEV_EMAIL, "Netly feedback", body)
        runCatching { if (Desktop.isDesktopSupported()) Desktop.getDesktop().browse(URI(uri)) }
    }
}
