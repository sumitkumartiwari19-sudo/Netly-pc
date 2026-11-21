# Third-party software in Netly

Netly's own code is under the MIT License (see `LICENSE`). The installer also contains the following programs and libraries, each under its own license.

| Component | Used for | License | Source |
|---|---|---|---|
| yt-dlp | Reading YouTube pages and downloading | The Unlicense (public domain) | https://github.com/yt-dlp/yt-dlp |
| FFmpeg (LGPL build by BtbN) | Merging, converting, playing, previews | LGPL v2.1 or later | https://ffmpeg.org · https://github.com/FFmpeg/FFmpeg · build scripts: https://github.com/BtbN/FFmpeg-Builds |
| Kotlin, kotlinx.coroutines, kotlinx.serialization | Programming language and libraries | Apache License 2.0 | https://github.com/JetBrains/kotlin · https://github.com/Kotlin |
| Compose Multiplatform (Desktop) and Skiko/Skia | User interface | Apache 2.0 / BSD 3-Clause | https://github.com/JetBrains/compose-multiplatform |
| OpenJDK runtime (Eclipse Temurin) | Runs the app | GPL v2 with Classpath Exception | https://adoptium.net |
| Manrope, Noto Sans Devanagari, JetBrains Mono | Fonts | SIL Open Font License 1.1 | https://fonts.google.com |

## FFmpeg (LGPL)
Netly uses FFmpeg as a separate program (`ffmpeg.exe`) started by the app; it is not linked into Netly's code. The FFmpeg build used is the unmodified LGPL build published at the BtbN link above. You may replace `ffmpeg.exe` in the Netly engine folder (`%LOCALAPPDATA%\Netly\engine`) with another compatible build.

## Trademarks
YouTube is a trademark of Google LLC. Netly is an independent project and is not affiliated with, endorsed by or sponsored by YouTube or Google.
