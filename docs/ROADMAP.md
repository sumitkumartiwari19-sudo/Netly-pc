# Netly for PC — Roadmap and Task Checklist

Each milestone ends with something you can install and try. Build and test them in order; tick boxes as you go. "Done when" is the acceptance test.

## M0 — Project setup and pipeline
- [x] Create GitHub repo `netly-pc`; add `docs/`, `README.md`, `AGENTS.md`
- [x] Gradle project (Kotlin + Compose Desktop); Koin, SQLDelight and logging are added in M2/M3 when first used
- [x] `.github/workflows/build.yml` produces a Windows installer for a hello-world window
- [x] Bundle `resources/windows-x64/` (yt-dlp.exe and ffmpeg.exe downloaded in CI)
- **Done when:** a tag `v0.0.1` produces a GitHub Release with an installer that opens a window on Windows.

## M1 — Shell and theme
- [x] `NetlyTheme` with light and dark `NeuColors` (system font for now; Manrope and JetBrains Mono are bundled in the M6 polish step)
- [ ] Reusable components: `NeuSurface`, `NeuButton`, `NeuSegmented`, `NeuText`, `NeuIcon` (M1); `NeuChip`, `NeuSwitch`, `NeuProgress`, `NeuInput` are added in the milestone that first needs them
- [ ] Sidebar navigation with 4 placeholder screens (native Windows title bar for now; custom title bar is a later polish item, see DESIGN.md section 11)
- [x] Theme switch (Light / Dark; follows Windows by default); saved to settings in M4
- **Done when:** the empty shell matches `DESIGN.md` in both themes.

## M2 — Home: fetch and preview
- [x] `EngineRepository` copies bundled yt-dlp and FFmpeg to the engine folder on first run
- [x] `YtDlpClient.fetch(url)` returns `MediaInfo` (title, channel, duration, thumbnail, formats)
- [x] Link bar with Paste, Fetch, validation and error messages; Ctrl+V auto-detect
- [x] Preview card and options card (video/audio switch, quality grid with sizes, container, toggles)
- [x] Loading skeleton and error state
- **Done when:** pasting a public video link shows the correct title, thumbnail and quality list.

## M3 — Download manager
Split in two parts: M3a (engine and queue, no new screen) and M3b (Downloads screen).
- [x] Downloads saved in `downloads.json` (a small JSON file replaces SQLDelight; swap later only if the list gets large)
- [x] `DownloadManager` with queue, 2 parallel downloads, status machine, throttled progress
- [x] `ProgressParser`, `DownloadCommandBuilder` and `DownloadErrorMapper` with unit tests
- [x] "Download" and "Add to queue" create jobs; merging step handled
- [x] Downloads screen: summary cards, tabs, rows with pause, resume, cancel, retry (M3b)
- [x] Restart safety (interrupted items become Paused)
- **Done when:** a 1080p video and an MP3 download fully, pause and resume works, and failures show a readable reason.

## M4 — Library and Settings
- [x] Library screen: grid/list, search, filters, sort, play, show in folder, delete
- [x] Settings screen with all P0 options saved to `settings.json`
- [x] Language switch English / हिन्दी (text lookup in `ui/i18n/Strings.kt`, Noto Sans Devanagari bundled)
- [x] Filename template and save-location picker
- **Done when:** completed downloads appear in the Library and settings survive a restart.

## M5 — Reliability and engine updates
- [x] Engine updater (uses yt-dlp's own verified updater; own rollback later)
- [x] Browser-cookies option and "request blocked" guidance
- [x] Connection test; proxy support
- [x] Auto-retry on network errors; speed limit
- [x] Playlist support: search, open a playlist, select items, enqueue many
- **Done when:** the engine updates in-app and a blocked video can be fixed with browser cookies.

## M6 — Desktop polish
- [x] Tray icon (open, pause all, quit) and minimise-to-tray
- [x] Clipboard watcher prompt and notifications
- [x] Start with Windows; single-instance handling
- [x] Shortcuts Ctrl+1..4 and Ctrl+, ; full accessibility pass later
- [x] App icon and installer branding; first-run notice later
- **Done when:** the app can live in the tray and offers to download copied links.

## M7 — Beta and release
- [ ] Manual test pass on Windows 10 and 11 (100%, 150%, 200% scaling)
- [ ] Measure installer size, cold start and idle RAM against PRD targets
- [ ] README with install steps and the SmartScreen note; publish checksums
- [ ] `v1.0.0` tag → GitHub Release; submit to Orion Store
- [ ] Self-update banner (P1) and portable zip (if included)
- **Done when:** v1.0.0 is public with installer and checksum files.

## Added after M6
- [x] Search screen (videos and playlists) with Play, quick download and options
- [x] Built-in player window: speed 0.5x to 4x, volume, seek, 360p stream and 720p/1080p (prepared first)
- [x] H.264 first for MP4 downloads (fixes the "AV1 Video Extension" message)
- [x] Smooth mode for slow PCs (default on for 2 cores or fewer), small memory settings
- [x] Bundled fonts: Manrope, JetBrains Mono, Noto Sans Devanagari

## After v1.0
- P1 leftovers: embed subtitles/tags, playlist polish, per-item resume across restarts
- Google sign-in WebView (E-04), favorites (L-04), FLAC/WAV
- Code signing certificate
- Optional: macOS/Linux builds

## Working with AI Studio / Claude (phone workflow)
1. Give the model `AGENTS.md`, the relevant doc (`DESIGN.md` for UI, `ARCHITECTURE.md` for logic) and **one milestone at a time**.
2. Ask for complete files with paths; commit through MGit with message `M2: fetch and preview`.
3. Push, wait for the GitHub Actions build, and fix errors by pasting the failing log lines back.
4. Install the artifact from the Actions run or a pre-release tag on a Windows PC and note bugs in `docs/BUGS.md`.
