package net.axay.openapigenerator

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.withType

class GeneratorPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.tasks.withType<OpenApiGenerateTask> {
            
        }
    }
}
