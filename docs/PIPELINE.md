# Netly for PC — Build and Release Pipeline

Everything is built on GitHub Actions (`windows-latest`), so no PC is needed to produce the installer. Workflow file: `.github/workflows/build.yml`.

## 1. What the pipeline does
| Step | Result |
|---|---|
| Checkout, JDK 21, Gradle 8.10.2 | Clean toolchain. Gradle is installed by the action, so no `gradlew` or `gradle-wrapper.jar` is needed in the repo |
| Compute version | Tag `v1.2.0` → `1.2.0`. Non-tag builds use `0.0.1` |
| Download yt-dlp | From the official release; SHA-256 checked against the published `SHA2-256SUMS`; build fails on mismatch |
| Download FFmpeg | LGPL build from BtbN FFmpeg-Builds, `ffmpeg.exe` only |
| `gradle test` | Unit tests must pass |
| `gradle packageExe packageMsi createDistributable` | Installer `.exe`, installer `.msi`, and a portable app folder |
| Collect | Copies files to `dist/`, zips the portable app, writes `SHA256SUMS.txt` |
| Upload artifact | Always: downloadable from the Actions run for 14 days |
| Publish release | Only on tags `v*`: creates a GitHub Release with all files and auto-generated notes. Tags with `-` (for example `v1.0.0-beta1`) become pre-releases |

## 2. Triggers
- Push to `main`: build and upload artifact (no release).
- Pull request: build and test only.
- Tag `v*`: build, test, and release.
- Manual: Actions tab → "Build Windows installer" → Run workflow.

## 3. Releasing from your phone
1. Commit and push to `main` with MGit; wait for the green build.
2. Open the repo on GitHub → Releases → Draft a new release → type a new tag such as `v1.0.0` ("Create new tag on publish") → target `main` → Publish.
3. The tag triggers the workflow; after about 10–15 minutes the Release shows the installer, MSI, portable zip and checksums.
4. Test the installer on a Windows PC before sharing the link.

Use `v1.0.0-beta1` style tags for test releases.

## 4. Required project files
Gradle module is named `app`, so build outputs are under `app/build/compose/binaries/main/`. Add to `app/build.gradle.kts`:

```kotlin
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

val appVersion = (findProperty("appVersion") as String?) ?: "0.0.1"

compose.desktop {
    application {
        mainClass = "com.skt.netly.pc.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Exe, TargetFormat.Msi)
            packageName = "Netly"
            packageVersion = appVersion
            vendor = "Netly"
            description = "YouTube video and audio downloader for Windows"
            modules("java.sql", "jdk.unsupported")

            // Folder holding windows-x64/yt-dlp.exe and windows-x64/ffmpeg.exe
            appResourcesRootDir.set(project.layout.projectDirectory.dir("resources"))

            windows {
                menuGroup = "Netly"
                shortcut = true
                dirChooser = true
                perUserInstall = true
                upgradeUuid = "0030b034-9aa5-433a-b27e-fdaaaf0a92c1"
                iconFile.set(project.file("icons/netly.ico"))
            }
        }
    }
}
```
Notes:
- **`upgradeUuid` must never change.** It lets a new installer upgrade an old install instead of installing side by side. The value above was generated for this project.
- `perUserInstall = true` installs under the user's profile without admin rights, which also keeps the app folder writable-friendly.
- At runtime, read bundled files from `System.getProperty("compose.application.resources.dir")`.
- Provide `icons/netly.ico` (multi-size: 16, 32, 48, 256 px) before the first release.
- `.gitignore` must include `app/resources/windows-x64/` so the downloaded binaries are never committed.

## 5. Versioning
Semantic versions: `MAJOR.MINOR.PATCH`.
- Patch: bug fixes. Minor: new features. Major: breaking changes or big redesigns.
- The release tag is the single source of truth; do not edit the version in Gradle by hand.
- Keep a `CHANGELOG.md` (Keep a Changelog format) updated before each tag.

## 6. Secrets and permissions
- No secrets are needed for v1: the workflow uses the built-in `GITHUB_TOKEN` with `contents: write` to publish releases.
- Later, for code signing: store the certificate as an encrypted secret (`WINDOWS_CERT_BASE64`, `WINDOWS_CERT_PASSWORD`) and add a `signtool` step after packaging, before checksums.

## 7. Supply-chain safety
- yt-dlp is verified by checksum on every build; the in-app updater does the same at runtime.
- Pin third-party actions to major versions (done) and review before upgrading.
- Do not run installers from unknown forks; only the repo owner's tags publish releases.

## 8. Telling users about SmartScreen
Because v1 is unsigned, Windows may show "Windows protected your PC". The README must say: click **More info → Run anyway**, and show the SHA-256 from `SHA256SUMS.txt` so users can verify the download:

```powershell
Get-FileHash .\Netly-1.0.0.exe -Algorithm SHA256
```

## 9. Troubleshooting
| Symptom | Likely cause and fix |
|---|---|
| "Cannot find WiX" in `packageExe` | The jpackage EXE/MSI step needs WiX 3.x. It is preinstalled on `windows-latest`; if a runner image drops it, add a step `choco install wixtoolset -y` before building |
| `packageExe` fails on version | `packageVersion` must be `MAJOR.MINOR.PATCH` numbers only; the workflow strips `-beta1` |
| `NoClassDefFoundError: java/sql/...` at runtime | Add the missing JDK module to `modules(...)` in `nativeDistributions` |
| yt-dlp or FFmpeg not found at runtime | Check `app/resources/windows-x64/` exists during the build and `appResourcesRootDir` points to `resources` |
| yt-dlp checksum step fails | The release is mid-upload or the sums file format changed; re-run, then inspect `sums.txt` |
| Release step skipped | The run was not triggered by a tag starting with `v` |
| Build takes over 20 min | Check the Gradle cache step ran; avoid `--no-daemon` removal conflicts and heavy tests |

## 10. Checklist before tagging
- [ ] Latest `main` build is green
- [ ] `CHANGELOG.md` updated
- [ ] Installer tested on a clean Windows 10 or 11 machine
- [ ] Install over an older version to confirm the upgrade works
- [ ] Uninstall leaves no running processes (yt-dlp, ffmpeg)
- [ ] README links and SmartScreen note are current
