## kotlin-openapi-generator

A not yet feature complete client generator.

Features:

- generates 100% Kotlin multiplatform code
- does not generate any useless classes
- tries to be as clean as possible

### Dependency

This is a personal project for my own use cases, however if you think it is useful for you too, you can use it as a
library or via the Gradle plugin:

**Gradle plugin:**

```kotlin
plugins {
    id("net.axay.openapigenerator")
}
```

**Library:**

````kotlin
dependencies {
    implementatin("net.axay:openapigenerator:$version")
}
````

Both are available on `mavenCentral()`, and the Gradle plugin is also available on the `gradlePluginPortal()`.

### Usage

**Gradle plugin**

Example for how to register a generation task:

````kotlin
tasks {
    register<OpenApiGenerateTask>("generateFromYourSpec") {
        specUrl.set("https://urltoyourspec.json") // you can also use 'specFile'
        outputDirectory.set(file("src/commonMain/kotlin/"))
        packageName.set("your.package.name")
        // optionally: deleteOldOutput.set(true)
        // this requires extra care, because it recursively deletes the output directory
    }
}
````

All properties for the `OpenApiGenerateTask` can be
[found here](https://github.com/jakobkmar/kotlin-openapi-generator/blob/main/generator-gradle-plugin/src/main/kotlin/net/axay/openapigenerator/OpenApiGenerateTask.kt).

###

**Library**

Have a look at
[how the Gradle plugin uses the library](https://github.com/jakobkmar/kotlin-openapi-generator/blob/main/generator-gradle-plugin/src/main/kotlin/net/axay/openapigenerator/OpenApiGenerateTask.kt#L56).

___

If you modify this project, please respect the AGPL-3.0 License.
