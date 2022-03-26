package net.axay.openapigenerator

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.net.URL

abstract class OpenApiGenerateTask : DefaultTask() {

    /**
     * The URL from which the OpenAPI spec can be downloaded.
     */
    @get:Optional
    @get:Input
    abstract val specUrl: Property<String>
    /**
     * The file which contains the OpenAPI spec.
     */
    @get:Optional
    @get:Input
    abstract val specFile: RegularFileProperty

    /**
     * The name of the package where all files will be generated in.
     * The generator might add sub packages inside this package.
     */
    @get:Input
    abstract val packageName: Property<String>

    /**
     * The directory which is the root of package structure which will be
     * generated.
     */
    @get:OutputDirectory
    abstract val outputDirectory: RegularFileProperty
    /**
     * If true, old generated files will be deleted before writing the new ones.
     */
    @get:Optional
    @get:Input
    abstract val deleteOldOutput: Property<Boolean>

    @TaskAction
    fun generate() {
        val openApiJson = when {
            specFile.isPresent -> specFile.get().asFile.readText()
            specUrl.isPresent -> URL(specUrl.get()).readText()
            else -> error("Both 'specFile' and 'specUrl' have not been set, but one them is required for resolving the OpenAPI spec!")
        }

        val outputDirectoryFile = outputDirectory.get().asFile
        if (deleteOldOutput.getOrElse(false)) {
            outputDirectoryFile.deleteRecursively()
        }

        val generator = Generator(openApiJson, packageName.get(), outputDirectoryFile)
        generator.generateSchemas()
    }
}
