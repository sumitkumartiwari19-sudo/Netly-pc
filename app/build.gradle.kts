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
    // Built-in video player window (plays MP4 with H.264/AAC)
    for (module in listOf("base", "graphics", "controls", "media", "swing")) {
        implementation("org.openjfx:javafx-$module:21.0.5:win")
    }
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

compose.desktop {
    application {
        mainClass = "com.skt.netly.pc.MainKt"
        // Small heap and the single-threaded collector keep memory low on 4 GB PCs.
        jvmArgs += listOf("-Xms48m", "-Xmx640m", "-XX:+UseSerialGC", "-XX:MaxMetaspaceSize=256m")

        nativeDistributions {
            targetFormats(TargetFormat.Exe, TargetFormat.Msi)
            packageName = "Netly"
            packageVersion = appVersion
            vendor = "Netly"
            description = "YouTube video and audio downloader for Windows"
            modules("java.sql", "jdk.unsupported", "java.logging", "java.prefs", "java.xml", "java.naming")

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
