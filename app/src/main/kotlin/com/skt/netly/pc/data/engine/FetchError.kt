package com.skt.netly.pc.data.engine

enum class ErrorKind {
    InvalidUrl,
    PlaylistNotSupported,
    EngineMissing,
    Blocked,
    Unavailable,
    AgeRestricted,
    Network,
    Timeout,
    Unknown,
}

data class FetchError(
    val kind: ErrorKind,
    val message: String,
    val detail: String? = null,
)

class FetchException(val error: FetchError) : Exception(error.message)

/** Turns yt-dlp's stderr text into an error the user can understand (see docs/ARCHITECTURE.md section 4.3). */
object ErrorMapper {
    fun map(stderr: String): FetchError {
        val lower = stderr.lowercase()
        val detail = stderr.lineSequence().map { it.trim() }.lastOrNull { it.isNotEmpty() }

        return when {
            "confirm your age" in lower || "age-restricted" in lower || "age restricted" in lower ->
                FetchError(ErrorKind.AgeRestricted, "This video is age-restricted and needs a signed-in browser.", detail)

            "not a bot" in lower || "sign in to confirm" in lower || "http error 403" in lower || "403: forbidden" in lower ->
                FetchError(ErrorKind.Blocked, "YouTube blocked this request. Try again in a moment.", detail)

            "private video" in lower || "video unavailable" in lower || "is not available" in lower ||
                "has been removed" in lower || "this video is unavailable" in lower ->
                FetchError(ErrorKind.Unavailable, "This video is private or unavailable.", detail)

            "unable to download webpage" in lower || "timed out" in lower || "network is unreachable" in lower ||
                "temporary failure in name resolution" in lower || "urlopen error" in lower || "getaddrinfo" in lower ||
                "connection" in lower && "refused" in lower ->
                FetchError(ErrorKind.Network, "Could not reach YouTube. Check your internet connection and try again.", detail)

            else -> FetchError(ErrorKind.Unknown, "Something went wrong while reading this video.", detail)
        }
    }
}
