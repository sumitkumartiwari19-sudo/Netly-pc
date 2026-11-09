# AGENTS.md — Rules for AI coding assistants (Netly for PC)

Read this file first, every session. Then read only the doc you need: `docs/DESIGN.md` for UI work, `docs/ARCHITECTURE.md` for logic, `docs/ROADMAP.md` to know the current milestone.

## Project
Native Windows desktop app: YouTube downloader. Kotlin + Compose Multiplatform Desktop, MVVM, Koin, SQLDelight, Coroutines/Flow. Extraction and downloads run through a bundled `yt-dlp.exe`; merging through `ffmpeg.exe`. Built and released by GitHub Actions. The owner develops from a phone, so everything must build in CI with no local setup.

## Working rules
1. **One milestone at a time.** Do only the milestone or task the user names. Do not start later milestones.
2. **Complete files.** Output whole files with their full repo path, ready to paste. No "rest unchanged" placeholders.
3. **Small, buildable steps.** Every change must compile in CI. Prefer several small commits over one huge one.
4. **Follow the docs.** If a request conflicts with `PRD.md`, `DESIGN.md` or `ARCHITECTURE.md`, say so and ask; do not silently deviate. When you change behaviour, update the matching doc in the same answer.
5. **Ask only when blocked.** Otherwise choose the sensible default, say what you assumed in one line, and continue.
6. **Be concise.** Short explanation, then code. List the files you changed at the end.

## Code conventions
- Kotlin official style, 4-space indent, no wildcard imports, no `!!`.
- Package root: `com.skt.netly.pc`. Layout as in `ARCHITECTURE.md` section 3.
- UI state: one immutable `UiState` data class per screen exposed as `StateFlow`; user actions are functions on the ViewModel.
- No business logic in composables. No hard-coded colours, sizes or strings in screens: use `NetlyTheme` tokens and string resources (English + हिन्दी).
- Use `NeuSurface` and the shared components for every neumorphic element; never copy shadow code.
- Blocking work (process, disk, network) only on `Dispatchers.IO`. Never block the UI thread.
- Errors: return typed results (`Result`/sealed `DownloadError`); never swallow exceptions silently, never show raw stack traces to the user.
- Public functions in `data/` and `domain/` get unit tests (kotlin.test). Parsers and mappers must have tests with real sample output.
- Comments explain why, not what. No dead code or TODO without an issue number.

## Hard rules (do not break)
- Never call YouTube endpoints directly or reimplement extraction; always use `YtDlpClient`.
- Build process arguments as a list; never concatenate user input into a shell string. Accept only `http(s)` YouTube URLs.
- Never log or store cookies, full URLs with query strings, or file paths outside the app folder unless the user is viewing them.
- Never commit binaries (`yt-dlp.exe`, `ffmpeg.exe`) or secrets. They are downloaded in CI.
- Do not change `upgradeUuid`, the package name, or the signing/release workflow without explicit approval.
- Do not add dependencies without naming them and the reason; prefer ones already in the stack.
- No telemetry, ads or analytics.

## Definition of done
- Builds in the GitHub Actions workflow and `gradle test` passes.
- Works in light and dark themes and at 100–200% scaling.
- Matches `DESIGN.md` (spacing, radii, colours, states: loading, empty, error).
- Keyboard accessible with visible focus; icon buttons have descriptions.
- Docs and `ROADMAP.md` checkboxes updated.

## Response template
```
Milestone/task: <name>
Assumptions: <one line or none>
Files:
  <path>  (new|changed)
<code blocks, one per file>
Next step: <one line>
```

## Prompt starter the owner can paste
"Read AGENTS.md. Task: Milestone M2, Home screen link bar and fetch. Use docs/DESIGN.md section 8.1 and docs/ARCHITECTURE.md sections 4.1–4.3. Give me full files."
