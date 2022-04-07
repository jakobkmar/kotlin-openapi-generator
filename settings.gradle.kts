rootProject.name = "kotlin-openapi-generator"

dependencyResolutionManagement {
    versionCatalogs {
        register("libs") {
            library("kotlinx-serialization-json", "org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")

            library("okio", "com.squareup.okio:okio:3.0.0")
            library("kotlinpoet", "com.squareup:kotlinpoet:1.11.0")
        }
    }
}

// modules
include("openapigenerator")
include("openapigenerator-gradle-plugin")

// local testing
include("openapigenerator-test")
