plugins {
    `maven-publish`
    kotlin("multiplatform") version "1.6.10" apply false
}

allprojects {
    group = "net.axay"
    description = "A clean OpenAPI client generator for Kotlin multiplatform"

    repositories {
        mavenCentral()
    }
}
