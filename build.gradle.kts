import me.dkim19375.dkimgradle.util.setupJava

plugins {
    kotlin("jvm") version "1.9.21"

    id("org.cadixdev.licenser") version "0.6.1"
    id("io.github.dkim19375.dkim-gradle") version "1.3.7"
}

group = "me.dkim19375"
version = "1.0.0"

setupJava(
    javaVersion = JavaVersion.VERSION_11
)

repositories {
    mavenCentral()
}

subprojects {
    apply(plugin = "org.cadixdev.licenser")
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "io.github.dkim19375.dkim-gradle")

    group = rootProject.group
    version = rootProject.version

    setupJava(
        javaVersion = JavaVersion.VERSION_11
    )

    repositories {
        mavenCentral()
        google()
    }

    tasks {
        compileKotlin {
            for (sub in allprojects) {
                dependsOn(sub.tasks.licenseFormat)
            }
        }
    }

    dependencies {
        if (project.name != "common") {
            implementation(project(":common"))
        }
    }
}