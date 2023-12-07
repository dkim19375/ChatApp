import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    id("org.jetbrains.compose") version "1.5.11"
}

repositories {
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    // Note, if you develop a library, you should use compose.desktop.common.
    // compose.desktop.currentOs should be used in launcher-sourceSet
    // (in a separate module for demo project and in testMain).
    // With compose.desktop.common you will also lose @Preview functionality
    implementation(compose.desktop.currentOs)

    implementation("io.ktor:ktor-client-cio:2.3.7")
    implementation("io.ktor:ktor-client-core:2.3.7")
    implementation("io.ktor:ktor-client-websockets:2.3.7")
}

license {
    exclude("me/dkim19375/chatapp/client/navigation/Nav**")
}

tasks {
    jar {
        archiveBaseName.set("ChatAppClient")
    }
}

compose.desktop {
    application {
        mainClass = "me.dkim19375.chatapp.client.ChatAppClientMainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "ChatApp"
            packageVersion = "1.0.0"
        }

        buildTypes.release.proguard.configurationFiles.from(
            File(projectDir, "proguard-rules.pro")
        )
    }
}