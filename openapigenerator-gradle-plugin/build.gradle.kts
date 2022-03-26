version = "0.0.1"

plugins {
    `java-gradle-plugin`
    kotlin("jvm") version "1.6.10"
    `maven-publish`
    id("com.gradle.plugin-publish") version "0.20.0"
}

dependencies {
    implementation(gradleKotlinDsl())

    api(project(":openapigenerator"))
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
            displayName = "Kotlin OpenAPI Generator"
            description = project.description
        }
    }
}
