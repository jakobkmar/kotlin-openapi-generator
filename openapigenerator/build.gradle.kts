import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

version = "0.0.1"

plugins {
    kotlin("multiplatform") version "1.6.10"
    `maven-publish`
    signing
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

val githubRepo = "jakobkmar/kotlin-openapi-generator"

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

            pom {
                name.set("kotlin-openapi-generator")
                description.set(project.description)
                url.set("https://github.com/${githubRepo}")

                developers {
                    developer { name.set("jakobkmar") }
                }

                licenses {
                    license {
                        name.set("GNU Affero General Public License, Version 3")
                        url.set("https://www.gnu.org/licenses/agpl-3.0.txt")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/${githubRepo}.git")
                    url.set("https://github.com/${githubRepo}/tree/main")
                }
            }
        }
    }
}

signing {
    sign(publishing.publications)
}
