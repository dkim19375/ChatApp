import me.dkim19375.dkimgradle.util.setupJava

plugins {
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

setupJava(
    javaVersion = null,
    textEncoding = null,
    artifactClassifier = null,
    mainClassName = "me.dkim19375.chatapp.server.ServerManagerKt"
)


dependencies {
    implementation("io.ktor:ktor-server-core:2.3.6")
    implementation("io.ktor:ktor-server-netty:2.3.6")
    implementation("io.ktor:ktor-server-websockets:2.3.6")
}