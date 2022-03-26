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

    val githubRepo = "jakobkmar/kotlin-openapi-generator"

    if (plugins.hasPlugin("maven-publish")) {
        publishing {
            publications.withType<MavenPublication> {
                pom {
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
}
