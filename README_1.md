# Netly for PC

A fast, good-looking YouTube downloader for Windows. Paste a link, pick a quality, download. Neumorphic UI with light and dark themes, a download queue, and an offline library.

> Status: in development (see `docs/ROADMAP.md`).

## Features
- Video (360p–4K), Shorts, playlists and audio-only (MP3, M4A, Opus)
- Download queue with pause, resume, retry and parallel downloads
- Library of finished downloads with search and filters
- Light, dark and system theme; English and हिन्दी
- Built-in engine updates (yt-dlp), so it keeps working when YouTube changes
- No ads, no account, no tracking

## Install
1. Open the [Releases](../../releases) page and download `Netly-<version>.exe` (installer) or the portable `.zip`.
2. Run the installer. If Windows shows **"Windows protected your PC"**, click **More info → Run anyway** (the app is not code-signed yet).
3. Optional: verify the download with `Get-FileHash .\Netly-<version>.exe -Algorithm SHA256` and compare with `SHA256SUMS.txt`.

Requires Windows 10 (1809+) or Windows 11, 64-bit.

## How to use
1. Copy a YouTube link and paste it in Netly (or press Ctrl+V).
2. Press **Fetch**, choose Video or Audio, quality and format.
3. Press **Download**. Watch progress under **Downloads**; find files in **Library**.

If a video fails with "request blocked", open **Settings → YouTube access** and turn on browser cookies, then retry.

## Documentation
| File | Purpose |
|---|---|
| `docs/PRD.md` | Product requirements |
| `docs/DESIGN.md` | UI spec and design tokens |
| `docs/ARCHITECTURE.md` | Code structure, engine, database |
| `docs/ROADMAP.md` | Milestones and task checklist |
| `docs/PIPELINE.md` | Build and release pipeline |
| `AGENTS.md` | Rules for AI coding assistants |

## Build from source
Requirements: JDK 21, Gradle 8.10+, Windows.
```
mkdir app\resources\windows-x64
:: place yt-dlp.exe and ffmpeg.exe in that folder
gradle run
gradle packageExe
```
Releases are built automatically by GitHub Actions when a `v*` tag is pushed.

## Legal
Download only content you own or have permission to download. You are responsible for following YouTube's Terms of Service and local copyright law. Netly is not affiliated with YouTube or Google. yt-dlp (Unlicense) and FFmpeg (LGPL) are bundled; see `THIRD_PARTY_NOTICES.md`.

## License
To be decided by the project owner (for example MIT).
