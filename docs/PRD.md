# Netly for PC — Product Requirements Document

Version 1.0 · Status: Draft for build · Platform: Windows 10/11 (x64)

## 1. Summary
Netly for PC is a native Windows desktop app that downloads YouTube videos, Shorts, playlists and audio to the user's computer. It has a neumorphic UI (light and dark), a download queue, and an offline library. The engine (yt-dlp) is updated separately from the app so extraction keeps working when YouTube changes.

It is the desktop sibling of the Android app Netly (com.skt.netly), built for Indian users.

## 2. Problem
- Existing downloaders are ad-heavy, bundle unwanted software, or look dated.
- Extractors break often because YouTube changes; users then wait for an app update.
- Many tools make users choose confusing formats and then merge audio and video by hand.

## 3. Goals
1. Paste a link, pick a quality, download — in three steps.
2. Keep working when YouTube changes, via in-app engine updates.
3. A calm, premium look (neumorphic) with light and dark themes.
4. Small, safe, ad-free installer for Windows.

### Non-goals (v1)
- macOS and Linux builds.
- Browser extension, mobile companion, cloud sync.
- Downloading DRM-protected content or other sites than YouTube.
- Built-in video editor.
- Monetisation / ads (revisit after v1).

## 4. Target users
| Persona | Need |
|---|---|
| Student / learner | Save lectures and tutorials for offline study, low data usage |
| Music listener | Audio-only (MP3/M4A) from music videos and playlists |
| Creator / editor | Highest quality video (up to 4K) for reference footage |

Primary market: India. UI languages v1: English, हिन्दी.

## 5. Functional requirements
Priority: **P0** must ship in v1.0, **P1** v1.1, **P2** later.

### 5.1 Fetch
| ID | Requirement | P |
|---|---|---|
| F-01 | Paste a YouTube link (video, Shorts, playlist, channel, youtu.be) and fetch metadata (title, channel, duration, thumbnail, formats) | P0 |
| F-02 | Validate links and show a clear error for invalid, private, age-gated or unavailable videos | P0 |
| F-03 | Paste button and Ctrl+V auto-detect | P0 |
| F-04 | Playlist view: select all/none, pick items before download | P1 |

### 5.2 Format and quality
| ID | Requirement | P |
|---|---|---|
| Q-01 | Video qualities 360p–2160p, shown with estimated size | P0 |
| Q-02 | Audio-only: MP3 (192/320k), M4A, Opus | P0 |
| Q-03 | Container choice: MP4, WEBM, MKV | P0 |
| Q-04 | Embed subtitles and thumbnail/metadata (toggles) | P1 |
| Q-05 | Audio FLAC/WAV | P2 |

### 5.3 Download queue
| ID | Requirement | P |
|---|---|---|
| D-01 | Queue with states: queued, downloading, merging, completed, failed, paused | P0 |
| D-02 | Pause, resume, cancel, retry per item; Pause all | P0 |
| D-03 | Progress %, speed, ETA per item; total speed | P0 |
| D-04 | Parallel downloads (1–5, default 3) | P0 |
| D-05 | Resume partial downloads after app restart | P1 |
| D-06 | Speed limit | P1 |
| D-07 | Auto-retry once on network error | P1 |

### 5.4 Library
| ID | Requirement | P |
|---|---|---|
| L-01 | List completed downloads (grid/list) with thumbnail, duration, quality, size | P0 |
| L-02 | Search, filter (All/Video/Audio/Playlists), sort | P0 |
| L-03 | Open file, show in folder, delete (record only or file too) | P0 |
| L-04 | Favorites | P2 |

### 5.5 Settings
| ID | Requirement | P |
|---|---|---|
| S-01 | Language: English, हिन्दी | P0 |
| S-02 | Theme: Light, Dark, System | P0 |
| S-03 | Save location, default quality, filename template | P0 |
| S-04 | Simultaneous downloads, speed limit | P0 |
| S-05 | Start with Windows, minimise to tray, clipboard watcher, notifications | P1 |
| S-06 | Proxy (HTTP/SOCKS5) | P1 |

### 5.6 YouTube access and engine
| ID | Requirement | P |
|---|---|---|
| E-01 | Bundled yt-dlp and FFmpeg work offline from first launch | P0 |
| E-02 | Engine auto-update (stable channel) with manual "Check for updates" | P0 |
| E-03 | Browser cookies option (Chrome/Edge/Firefox) to avoid "request blocked" errors | P1 |
| E-04 | Google sign-in via embedded WebView (cookie login) | P2 |
| E-05 | Connection test | P1 |
| E-06 | App self-update check against GitHub Releases | P1 |

## 6. Non-functional requirements
- **Performance:** cold start under 2 s on a mid-range PC; UI stays responsive at 5 parallel downloads; idle RAM under 300 MB.
- **Reliability:** a failed download never crashes the app; every failure shows a human-readable reason and a retry.
- **Size:** installer under 150 MB.
- **Privacy:** no accounts, no analytics by default; crash reports opt-in and never contain links or file names; cookies stay on the device.
- **Security:** only run bundled, hash-checked yt-dlp/FFmpeg; engine updates downloaded over HTTPS from the official GitHub release and verified against its published checksum.
- **Accessibility:** keyboard navigable, visible focus, text contrast at least 4.5:1, scalable text.
- **Compatibility:** Windows 10 (1809+) and 11, x64, 100–200% display scaling.

## 7. Key user flows
1. **Download a video:** paste link → Fetch → choose Video/Audio, quality, format → Download → watch progress → open from Library.
2. **Fix a blocked download:** download fails → "request blocked" message → Retry, or Settings → YouTube access → browser cookies → retry succeeds.
3. **Update engine:** Settings → Engine → Check for updates → new yt-dlp installed → no app restart needed.

## 8. Success metrics
- At least 90% of fetch attempts succeed on public videos (measured locally in testing; no telemetry by default).
- Median time from paste to download start under 5 s.
- Crash-free sessions above 99% in beta feedback.
- Installer reaches 1,000 downloads from GitHub Releases / Orion Store in the first 3 months.

## 9. Distribution
- GitHub Releases (primary): signed-off installer `.exe` and portable `.zip`.
- Orion Store listing.
- Code signing is out of scope for v1; users see a SmartScreen warning ("More info → Run anyway"). Document this in the README.

## 10. Risks and mitigations
| Risk | Mitigation |
|---|---|
| YouTube bot detection blocks extraction | Bundled yt-dlp with auto-update, cookies option, clear error and retry |
| SmartScreen warning on unsigned exe | Explain in README; evaluate signing later |
| Installer size from FFmpeg | Use a trimmed LGPL FFmpeg build; compress |
| YouTube Terms of Service restrict downloading | Show a short notice on first launch: download only your own or permitted content |
| Antivirus false positives on bundled exes | Ship official yt-dlp and FFmpeg binaries untouched; publish checksums |

## 11. Open questions
- Name of the installer publisher shown in Windows (individual vs studio name).
- Whether to offer a portable (no-install) build in v1.0 or v1.1.
- Whether the Google sign-in flow (E-04) is worth the maintenance cost versus browser cookies only.
