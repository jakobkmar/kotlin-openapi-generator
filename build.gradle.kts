import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `maven-publish`
    kotlin("multiplatform") version "1.6.20" apply false
}

allprojects {
    group = "net.axay"
    description = "A clean OpenAPI client generator for Kotlin multiplatform"

    repositories {
        mavenCentral()
    }

    tasks {
        withType<KotlinCompile> {
            kotlinOptions.jvmTarget = "11"
        }
    }
}
