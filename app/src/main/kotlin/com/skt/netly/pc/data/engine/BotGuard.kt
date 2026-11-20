package com.skt.netly.pc.data.engine

import java.io.File

/**
 * YouTube sometimes answers "Sign in to confirm you're not a bot" (it depends on the IP address and on which
 * YouTube client yt-dlp pretends to be). Every yt-dlp call goes through here: if the first try is refused,
 * other client sets are tried, and (only when the user allowed it in Settings) the browser's sign-in cookies.
 */
object BotGuard {
    /** Clients that normally need no sign-in. Unknown names are skipped by yt-dlp, so this list is safe to extend. */
    const val CLIENTS_A = "default,android_vr,mweb"
    const val CLIENTS_B = "tv,web_safari,android_vr"
    const val CLIENTS_C = "tv_simply,web_embedded,ios"

    val BROWSERS = listOf("firefox", "edge", "chrome", "brave")

    fun isBlocked(text: String): Boolean {
        val l = text.lowercase()
        return "not a bot" in l || "sign in to confirm" in l || "http error 403" in l || "403: forbidden" in l ||
            "confirm you" in l && "bot" in l
    }

    /** A cookie source that could not be read (browser open, no profile, encrypted). Worth trying the next option. */
    fun isCookieProblem(text: String): Boolean {
        val l = text.lowercase()
        return "cookie" in l && ("could not" in l || "failed" in l || "unable" in l || "permission" in l || "no such file" in l || "not found" in l)
    }

    fun shouldTryNext(text: String): Boolean = isBlocked(text) || isCookieProblem(text)

    /** True when the user gave a cookie source (a browser or a cookies.txt file). */
    fun hasCookies(o: EngineOptions): Boolean =
        (o.cookiesFile.isNotBlank() && File(o.cookiesFile).isFile) || o.cookiesBrowser in BROWSERS

    /** The tries, in order. The first one is the normal one. */
    fun variants(base: EngineOptions): List<EngineOptions> {
        val first = base.copy(clients = CLIENTS_A)
        val list = mutableListOf(first, base.copy(clients = CLIENTS_B), base.copy(clients = CLIENTS_C))
        if (!hasCookies(base) && base.cookiesBrowser == "auto") {
            BROWSERS.forEach { b -> list += first.copy(cookiesBrowser = b) }
        }
        return list
    }

    @Volatile
    private var lastGood = 0

    /**
     * Runs [run] with each variant until one is not refused. The answer returned when everything fails is the
     * first one, because that is the real reason (a later cookie error would only confuse).
     */
    suspend fun <T> attempt(base: EngineOptions, refused: (T) -> Boolean, run: suspend (EngineOptions) -> T): T {
        val list = variants(base)
        val start = lastGood.coerceIn(0, list.lastIndex)
        var firstAnswer: T? = null
        for (k in list.indices) {
            val i = (start + k) % list.size
            val answer = run(list[i])
            if (!refused(answer)) {
                lastGood = i
                return answer
            }
            if (firstAnswer == null) firstAnswer = answer
        }
        @Suppress("UNCHECKED_CAST")
        return firstAnswer as T
    }

    /** Runs a yt-dlp command built by [build] with the retry ladder. */
    suspend fun runProcess(base: EngineOptions, timeoutMs: Long, build: (EngineOptions) -> List<String>): ProcessResult =
        attempt(base, { r: ProcessResult -> r.exitCode != 0 && shouldTryNext(r.stderr) }) { o ->
            ProcessRunner.run(build(o), timeoutMs)
        }

    const val BLOCKED_MESSAGE =
        "YouTube wants to confirm you are not a bot. Open Settings → YouTube access and choose Auto (or a browser where you are signed in to YouTube), then try again."
}
