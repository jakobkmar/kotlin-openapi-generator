version = "0.0.2"

plugins {
    kotlin("multiplatform")
    `maven-publish`
    `publish-script`
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

val stubJavadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
}

publishing {
    publications {
        register<MavenPublication>(project.name) {
            this.groupId = project.group.toString()
            this.artifactId = "openapigenerator"
            this.version = project.version.toString()
        }

        withType<MavenPublication> {
            artifact(stubJavadocJar.get())

            pom {
                name.set("kotlin-openapi-generator")
            }
        }
    }
}
