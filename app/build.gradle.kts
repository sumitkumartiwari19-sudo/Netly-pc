import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
}

val appVersion = (findProperty("appVersion") as String?) ?: "0.0.1"

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.foundation)
    implementation(compose.animation)
    implementation(compose.runtime)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    runtimeOnly("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.9.0")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

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

            // Folder that contains windows-x64/yt-dlp.exe and windows-x64/ffmpeg.exe (filled by CI)
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
