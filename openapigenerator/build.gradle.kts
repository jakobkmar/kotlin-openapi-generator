import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

version = "0.0.1"

plugins {
    kotlin("multiplatform")
    `maven-publish`
    signing
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

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "11"
    }
}

val stubJavadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
}

publishing {
    repositories {
        maven {
            name = "ossrh"
            credentials(PasswordCredentials::class)
            setUrl("https://oss.sonatype.org/service/local/staging/deploy/maven2")
        }
    }

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

signing {
    sign(publishing.publications)
}
