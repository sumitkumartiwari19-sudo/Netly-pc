# Netly for PC — Design Spec

Source of truth for the UI. Matches the approved canvas "Netly PC – UI Design" (6 screens: Home, Downloads, Library, Settings in light; Home and Downloads in dark).

## 1. Principles
1. **Soft and tactile:** neumorphism — surfaces are raised from or pressed into one background colour using paired light and dark shadows.
2. **One accent:** indigo is the only chromatic colour, used for primary actions, progress and active states.
3. **Calm density:** large radii, generous spacing, one primary action per screen.
4. **Readable first:** neumorphism can have weak contrast, so text and active states must always pass 4.5:1.

## 2. Window
- Default size 1440×900, minimum 1100×700. Custom title bar (48 px): logo, "Netly", Minimize, Maximize, Close.
- Layout: title bar → [sidebar 232 px | content]. Outer padding 24 px, gap 24 px.
- Sidebar is a raised panel (radius 28). Content is a column with 20 px gaps.

## 3. Colour tokens
| Token | Light | Dark | Use |
|---|---|---|---|
| `bg` | `#E6EAF1` | `#23272F` | App and surface background |
| `shadowDark` | `#BCC4D3` | `#15181D` | Bottom-right shadow |
| `shadowLight` | `#FFFFFF` | `#31373F` | Top-left highlight |
| `text` | `#1F2433` | `#ECEFF6` | Primary text |
| `textMuted` | `#586177` | `#A5AEC2` | Secondary text |
| `accent` | `#4F5BFF` | `#4F5BFF` | Primary button fill, progress fill |
| `accentText` | `#3A45E0` | `#9AA4FF` | Active labels and icons |
| `onAccent` | `#FFFFFF` | `#FFFFFF` | Text on accent |
| `success` | `#0E6B4C` | `#5FD3A2` | Ready, merging, reachable (light value darkened to pass 4.5:1) |
| `error` | `#B3321F` | `#FF8A7A` | Failed state |

Contrast: `textMuted` on `bg` is above 5:1 in both themes; `accentText` on `bg` is above 5:1; white on `accent` is about 4.9:1.

## 4. Typography
- UI font: **Manrope** (400, 500, 600, 700, 800). Mono font: **JetBrains Mono** (500) for sizes, speeds, paths.
- Bundle both as TTF in app resources; never rely on a network font.

| Style | Size / weight | Use |
|---|---|---|
| Display | 34 / 800, letter-spacing -0.02em | Screen title |
| Title | 22 / 800 | Video title, card title |
| Body | 15 / 600 | Main text, buttons |
| Label | 14 / 700 | Rows, tabs |
| Caption | 12–13 / 600 | Meta, helper text |
| Overline | 12 / 800, uppercase, +0.06em | Section labels |
| Mono | 11–13 / 500 | Speeds, sizes, paths |

## 5. Shape, spacing, elevation
- Radii: window panels 28, cards 22–24, buttons 14–18, chips 10–14, circles for icon buttons.
- Spacing scale: 4, 6, 8, 10, 12, 14, 16, 20, 24.
- Elevation (shadow offset / blur):

| Level | Offset | Blur | Use |
|---|---|---|---|
| Raised L | 8 | 18 | Panels, cards |
| Raised M | 5 | 10 | Buttons, chips |
| Raised S | 3 | 6 | Small pills |
| Inset M | 5 | 10 | Inputs, tracks, active nav, selected chip |
| Inset S | 4 | 8 | Pressed button |

Raised = `shadowDark` at (+x, +y) and `shadowLight` at (-x, -y). Inset = same pair, inverted and drawn inside.

## 6. Components
| Component | Spec |
|---|---|
| Button | Raised M, radius 16, height 44–54, label 15/700. Pressed = Inset S. |
| Primary button | Fill `accent`, white label, Raised shadow. Used once per area (Fetch, Download). |
| Selected chip / tab | Inset, label in `accentText`. |
| Input | Inset, height 72 (link bar) or 40–48, transparent field, visible focus ring. |
| Segmented control | Inset track (padding 4–5) containing buttons; selected one is Inset with `accentText`. |
| Switch | Inset track 56×30, knob 24; on = knob `accent` at right, off = knob `textMuted` at left. |
| Progress bar | Inset track, height 10, fill `accent` (success green when merging, error red when failed). |
| Slider | Inset track, raised knob 26. |
| Nav item | 48 high, icon 20 + label. Active = Inset + `accentText`. Optional badge (accent pill). |
| Thumbnail | 16:9, radius 12–20, duration pill bottom-right (black 78%, white mono text). |
| Status dot | 9 px circle, `success` or `error`, always paired with text. |

Focus: 2 px `accentText` outline with 3 px offset on every interactive element. Disabled: 45% opacity, no shadow change.

## 7. Icons
Stroke icons, 24 px grid, stroke 1.8–2.2, round caps and joins, `currentColor`. Set used: home, download, folder (library), sliders (settings), link, clipboard, play, pause, close, minimize, maximize, search, grid, list, refresh, trash. Use Material Symbols Rounded or Lucide equivalents in Compose.

## 8. Screens
### 8.1 Home
Header (title, engine status pill) → link bar (input, Paste, Fetch) → "Supports" chips and hint → two cards:
- **Preview card:** thumbnail with play and duration, title, channel · views · age, info pills (4K available, subtitles, chapters).
- **Options card (440 wide):** Video / Audio-only segmented control → quality grid (3×2, label plus size) → Format segmented (MP4/WEBM/MKV or MP3/M4A/OPUS) → Embed subtitles, Embed thumbnail and tags switches → save-to row → "Add to queue" and primary "Download · size".

States: empty (no link yet: only the link bar and a short how-to), loading (skeleton for both cards), error (message under the link bar with Retry), ready (above).

### 8.2 Downloads
Header (Pause all, Clear completed) → three summary cards (total speed with sparkline, Active, Today) → tabs (All, Active, Queued, Completed, Failed with counts) → list of rows: thumbnail 112×63, title, meta, progress bar, status text and ETA, pause/start/retry and cancel buttons. Empty state: "Nothing downloading" with a link to Home.

### 8.3 Library
Header with search, sort and grid/list toggle → filter chips (All, Video, Audio, Playlists, Favorites) → 4-column card grid (thumbnail, 2-line title, quality chip, size, Play, Show in folder, Delete). Empty state: folder illustration and "Your downloads will show up here".

### 8.4 Settings
2×2 grid of raised sections: **General** (language, theme, start with Windows, clipboard watcher, notifications), **Downloads** (location, default quality, simultaneous downloads, speed limit, file name template), **YouTube access** (sign-in, browser cookies, proxy, connection test), **Engine and about** (yt-dlp version, update check, FFmpeg status, auto-update, crash reports, version).

## 9. Motion
- Press: shadows swap raised → inset in 120 ms.
- Progress: smooth animate to new value over 300 ms.
- Screen change: 180 ms cross-fade, no sliding.
- Respect "reduce motion" (Windows animation setting): disable all except state changes.

## 10. Accessibility
- Every control reachable by Tab, activated by Enter/Space; Esc closes dialogs.
- Icon-only buttons need a content description (Pause, Cancel, Show in folder…).
- Never rely on colour alone: status always has text.
- Support Windows display scaling 100–200% and text scale up to 125%.
- Shortcuts: Ctrl+V paste and fetch, Ctrl+1/2/3/4 switch screens, Ctrl+, open Settings, Space pause/resume selected download.

## 11. Compose implementation notes
Neumorphic shadows are not built into Compose, and Android's `setShadowLayer` does not exist on Desktop (Skia). Draw them with blurred shapes through one reusable modifier pair (implemented in `ui/theme/NeuModifiers.kt`).

- **Raised:** draw a blurred rounded rect in `shadowDark` offset (+o, +o), another in `shadowLight` offset (-o, -o), then the solid `bg` shape on top. Blur uses a Skia `MaskFilter.makeBlur(FilterBlurMode.NORMAL, blur / 2)` set through `paint.asFrameworkPaint().maskFilter`.
- **Inset:** clip to the rounded shape, fill `bg`, then draw the "ring" (a huge rect minus the shape) blurred: `shadowDark` shifted (+o, +o) and `shadowLight` shifted (-o, -o).
- Elevation values live in `NeuElevation` (Large 8/18, Medium 5/10, Small 3/6, InsetMedium 5/10, InsetSmall 4/8).
- Theme: `NetlyTheme(mode)` provides `NeuColors` and `NeuTypography`; read them through `Neu.colors` and `Neu.type`. No hard-coded colours in screens.
- Shared components: `NeuSurface`, `NeuButton` / `NeuTextButton`, `NeuSegmented`, `NeuSwitch`, `NeuProgress`, `NeuIcon`, `NeuText`. Do not copy shadow code into screens.
- Window: M1 uses the native Windows title bar. Undecorated windows lose edge-resize and snap layouts on Windows, so a custom title bar is a later, optional polish step.
- Fonts: Manrope and JetBrains Mono are bundled from M2; until then the system font is used.
- Test both themes; `ContrastTest` enforces 4.5:1 for text and state colours.
