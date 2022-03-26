plugins {
    `java-gradle-plugin`
    kotlin("jvm") version "1.6.10"
}

gradlePlugin {
    plugins {
        create("kotlinOpenapiGenerator") {
            id = "net.axay.openapigenerator"
            implementationClass = "net.axay.openapigenerator.GeneratorPlugin"
        }
    }
}

dependencies {
    implementation(gradleKotlinDsl())

    api(project(":generator"))
}
