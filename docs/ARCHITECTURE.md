# Netly for PC — Architecture

## 1. Overview
Kotlin + Compose Multiplatform (Desktop, JVM) app. UI follows MVVM with unidirectional data flow. Extraction and downloading are delegated to a bundled **yt-dlp.exe**; merging and conversion to a bundled **ffmpeg.exe**. The app never reverse-engineers YouTube itself.

```
┌──────────────────────────── Compose UI ────────────────────────────┐
│ Home · Downloads · Library · Settings · Tray · Dialogs             │
└───────────────▲────────────────────────────────────────────────────┘
                │ StateFlow<UiState> / events
┌───────────────┴──────────── ViewModels ───────────────────────────┐
│ HomeVM · DownloadsVM · LibraryVM · SettingsVM                      │
└───────────────▲────────────────────────────────────────────────────┘
                │ suspend / Flow
┌───────────────┴──────────── Domain / Repositories ─────────────────┐
│ MediaRepository · DownloadRepository · LibraryRepository ·         │
│ SettingsRepository · EngineRepository                              │
└──────▲────────────▲─────────────▲──────────────▲───────────────────┘
       │            │             │              │
 YtDlpClient   DownloadManager  SQLDelight DB   SettingsStore (JSON)
 (process)     (queue+workers)                  EngineUpdater
```

## 2. Tech choices
| Concern | Choice |
|---|---|
| Language / UI | Kotlin 2.x, Compose Multiplatform Desktop |
| DI | Koin |
| Async | Kotlin Coroutines + Flow |
| Database | SQLDelight (SQLite) |
| JSON | kotlinx.serialization |
| Images | Coil 3 (network thumbnails) with disk cache |
| Logging | SLF4J + Logback rolling file in `%LOCALAPPDATA%\Netly\logs` |
| Tray | Compose `Tray` API |
| Tests | kotlin.test, Turbine (Flow), MockK; Compose UI tests for key screens |
| Packaging | Compose Gradle plugin → jpackage (EXE and MSI) |

## 3. Package layout
```
app/src/main/kotlin/com/skt/netly/pc/
├─ App.kt, Main.kt               // window, tray, DI start
├─ di/                           // Koin modules
├─ ui/
│  ├─ theme/                     // NetlyTheme, NeuColors, NeuSurface, typography
│  ├─ components/                // NeuButton, NeuChip, NeuSwitch, NeuProgress…
│  ├─ home/  downloads/  library/  settings/
│  └─ shell/                     // TitleBar, Sidebar, navigation
├─ domain/
│  ├─ model/                     // MediaInfo, FormatOption, DownloadItem, DownloadState
│  └─ usecase/                   // FetchMedia, EnqueueDownload, RetryDownload…
├─ data/
│  ├─ engine/                    // YtDlpClient, FfmpegLocator, EngineUpdater, ProgressParser
│  ├─ download/                  // DownloadManager, DownloadWorker
│  ├─ db/                        // *.sq files, DbFactory
│  ├─ settings/                  // SettingsStore
│  └─ repo/                      // repositories
├─ platform/                     // Paths, Clipboard, Notifications, Startup, FileOps
└─ util/                         // Result, errors, formatters
```

## 4. Engine integration (yt-dlp + FFmpeg)
### 4.1 Locations
| What | Path |
|---|---|
| Bundled (read-only) | `<install>\app\resources\` via `compose.application.resources.dir` |
| Working copy (updatable) | `%LOCALAPPDATA%\Netly\engine\yt-dlp.exe` |
| FFmpeg | `%LOCALAPPDATA%\Netly\engine\ffmpeg.exe` (copied from bundle on first run) |

Program Files is not writable, so on first launch the bundled binaries are copied to the engine folder and the app always runs the working copy. Updates replace only the working copy.

### 4.2 Commands
Always: `--no-colors --no-warnings --ignore-config --ffmpeg-location <engine dir>`.

| Task | Arguments |
|---|---|
| Fetch metadata | `-J --no-playlist <url>` (playlist: `-J --flat-playlist`) |
| Video download | `-f "bv*[height<=H]+ba/b[height<=H]" --merge-output-format mp4 -o "<template>" <url>` |
| Audio download | `-x --audio-format mp3 --audio-quality 320K -o "<template>" <url>` |
| Subtitles / tags | `--embed-subs --embed-thumbnail --embed-metadata` |
| Cookies | `--cookies-from-browser chrome` (or edge, firefox) |
| Proxy | `--proxy <url>` |
| Speed limit | `--limit-rate 2M` |
| Progress | `--newline --progress-template "download:%(progress.downloaded_bytes)s\|%(progress.total_bytes)s\|%(progress.speed)s\|%(progress.eta)s"` |

Run each command through `ProcessBuilder` on `Dispatchers.IO`, read stdout line by line, and parse into `DownloadEvent` (Progress, Merging, Done, Error). Redirect stderr to a bounded buffer for error mapping.

### 4.3 Errors
`ErrorMapper` converts yt-dlp stderr into typed errors shown in the UI:

| Pattern | `DownloadError` | User message |
|---|---|---|
| "Sign in to confirm you're not a bot", HTTP 403 | `Blocked` | YouTube blocked this request. Retry, or enable browser cookies in Settings. |
| "Private video", "unavailable" | `Unavailable` | This video is private or unavailable. |
| "age" restricted | `AgeRestricted` | Needs a signed-in browser (cookies). |
| network, timeout | `Network` | Check your connection and retry (auto-retry once). |
| disk full / permission | `Disk` | Cannot write to the save folder. |
| anything else | `Unknown` | Show short message plus "Copy details". |

### 4.4 Engine updates
1. On launch (at most once per 24 h) and on "Check for updates", query `https://api.github.com/repos/yt-dlp/yt-dlp/releases/latest`.
2. Compare tag with the installed version (`yt-dlp --version`).
3. Download `yt-dlp.exe` and `SHA2-256SUMS` to a temp file, verify the checksum, then atomically replace the working copy (keep the previous one as `yt-dlp.exe.bak` for rollback).
4. If the new binary fails `--version`, restore the backup and surface an error.
Downloads in flight keep the old process; the new binary is used for the next job.

## 5. Download manager
- `DownloadManager` owns a `Channel` of jobs and a worker pool limited by the "simultaneous downloads" setting.
- State machine: `QUEUED → DOWNLOADING → (MERGING) → COMPLETED`; branches `PAUSED`, `FAILED`, `CANCELED`.
- **Pause:** terminate the process; yt-dlp keeps the `.part` file. **Resume:** start again with the same arguments (yt-dlp continues by default).
- **Cancel:** terminate and delete partial files.
- **Restart safety:** on startup, items left in `DOWNLOADING` become `PAUSED`; the user (or "resume on launch" option) restarts them.
- Progress is throttled to 4 updates per second per item before reaching the DB and UI.
- A single `StateFlow<List<DownloadItem>>` is the source of truth for the Downloads screen and the sidebar badge.

## 6. Data model (SQLDelight)
```sql
CREATE TABLE download (
  id            INTEGER PRIMARY KEY AUTOINCREMENT,
  video_id      TEXT NOT NULL,
  url           TEXT NOT NULL,
  title         TEXT NOT NULL,
  channel       TEXT,
  thumbnail_url TEXT,
  duration_sec  INTEGER,
  kind          TEXT NOT NULL,      -- VIDEO | AUDIO
  quality       TEXT NOT NULL,      -- e.g. 1080p, MP3-320
  container     TEXT NOT NULL,      -- mp4, webm, mkv, mp3, m4a, opus
  options_json  TEXT NOT NULL,      -- subtitles, thumbnail, cookies mode…
  state         TEXT NOT NULL,
  progress      REAL NOT NULL DEFAULT 0,
  total_bytes   INTEGER,
  file_path     TEXT,
  error_code    TEXT,
  error_detail  TEXT,
  favorite      INTEGER NOT NULL DEFAULT 0,
  created_at    INTEGER NOT NULL,
  completed_at  INTEGER
);
CREATE INDEX download_state ON download(state);
CREATE INDEX download_completed ON download(completed_at);
```
Library = `state = 'COMPLETED'`. DB file: `%LOCALAPPDATA%\Netly\netly.db`. Migrations through SQLDelight `.sqm` files.

## 7. Settings
`settings.json` in `%LOCALAPPDATA%\Netly\` (kotlinx.serialization, atomic write). Keys: language, theme, saveDir, defaultQuality, maxParallel, speedLimit, filenameTemplate, startWithWindows, minimizeToTray, clipboardWatcher, notifications, cookiesBrowser, proxy, engineAutoUpdate, crashReports. Exposed as `StateFlow<Settings>`.

## 8. Platform services
| Service | Implementation |
|---|---|
| Clipboard watcher | Poll AWT clipboard every 1 s while the app is open, ignore repeats, match YouTube URL regex |
| Notifications | Tray `sendNotification` |
| Start with Windows | Registry `HKCU\Software\Microsoft\Windows\CurrentVersion\Run` |
| Open / show in folder | `Desktop.open`, `explorer.exe /select,"<file>"` |
| Single instance | Lock file or local socket; a second launch forwards its link to the first |
| Self-update check | GitHub Releases API, show a banner with a download button |

## 9. Security and privacy
- Execute only the binaries in the engine folder; verify checksums after every download.
- Build process arguments as lists, never one shell string; reject URLs that are not `http(s)` YouTube hosts.
- Cookies are read by yt-dlp from the user's browser at run time and never stored or logged by Netly.
- Logs strip URLs' query strings and never contain cookies.
- No telemetry. Crash reports are opt-in and only contain the stack trace and app version.

## 10. Testing strategy
| Level | What |
|---|---|
| Unit | ProgressParser, ErrorMapper, filename templating, format selection, state machine |
| Integration | YtDlpClient against a fake yt-dlp script that prints recorded output |
| UI | Compose tests for Home states and Downloads rows (light and dark screenshots) |
| Manual | Real downloads on Windows 10 and 11: video, Shorts, playlist, audio, blocked video |

## 11. Decisions and trade-offs
- **yt-dlp over NewPipe Extractor:** more reliable and updated within hours of YouTube changes; costs ~40 MB of installer size and a child process.
- **Koin over Hilt:** Hilt is Android-only.
- **SQLDelight over Room:** Room is Android-first; SQLDelight is stable on Desktop.
- **Local engine, no backend:** desktop can run yt-dlp itself, so no Cloud Functions are needed (that idea stays specific to Android).
