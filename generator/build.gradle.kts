import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

version = "0.0.1"

plugins {
    kotlin("multiplatform") version "1.6.10"
}

repositories {
    mavenCentral()
}

kotlin {
    jvm()

    sourceSets {
        named("commonMain") {
            dependencies {
                implementation(kotlin("stdlib-common"))
                implementation(libs.kotlinx.serialization.json)
            }
        }

        named("jvmMain") {
            dependencies {
                implementation(libs.kotlinpoet)
            }
        }
    }
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "11"
    }
}
