version = "0.1.1"

plugins {
    `java-gradle-plugin`
    kotlin("jvm")
    `maven-publish`
    id("com.gradle.plugin-publish") version "0.20.0"
    `publish-script`
}

dependencies {
    implementation(gradleKotlinDsl())

    implementation("net.axay:openapigenerator-jvm:0.0.1")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.13.2")
}

pluginBundle {
    website = "https://github.com/jakobkmar/kotlin-openapi-generator"
    vcsUrl = "https://github.com/jakobkmar/kotlin-openapi-generator"
    tags = listOf("openapi", "generator", "kotlin", "kotlin-multiplatform")
}

gradlePlugin {
    plugins {
        create("kotlinOpenapiGenerator") {
            id = "net.axay.openapigenerator"
            implementationClass = "net.axay.openapigenerator.GeneratorPlugin"
            displayName = "Kotlin OpenAPI Generator Gradle Plugin"
            description = project.description
        }
    }
}

publishing {
    repositories {
        maven {
            name = "ossrh"
            credentials(PasswordCredentials::class)
            setUrl("https://oss.sonatype.org/service/local/staging/deploy/maven2")
        }
    }

    publications.withType<MavenPublication> {
        pom {
            name.set("Kotlin OpenAPI Generator Gradle Plugin")
        }
    }
}

java {
    withSourcesJar()
    withJavadocJar()
}
