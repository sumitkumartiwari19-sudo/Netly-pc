# Security

## Reporting a vulnerability
Please do **not** open a public issue for a security problem. Email **mr.sumitkumartiwari19@gmail.com** with the subject "Netly security", with steps to reproduce. I will reply as soon as I can and credit you in the release notes if you want.

## Supported versions
Only the latest release receives fixes.

## How Netly protects you
- Downloads and playback run through bundled `yt-dlp.exe` and `ffmpeg.exe`; arguments are passed as lists, never built into shell commands.
- Only YouTube links are accepted; file names from the web are cleaned before saving.
- The app opens only known https links (GitHub, Instagram, the developer's portfolio).
- Update checks are read-only (HTTPS to GitHub). Nothing is downloaded or installed automatically.
- Installs per user, so no administrator rights are needed.
- Every release lists SHA-256 checksums in `SHA256SUMS.txt`. Check with: `Get-FileHash .\Netly-<version>.exe -Algorithm SHA256`.
- The installer is not code-signed yet, so Windows SmartScreen may warn on first run. Download only from the official Releases page: https://github.com/sumitkumartiwari19-sudo/Netly-pc/releases
