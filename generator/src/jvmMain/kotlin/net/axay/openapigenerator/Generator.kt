package net.axay.openapigenerator

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import java.io.File

class Generator(
    openApiSpecJson: String,
    val packageName: String,
    val targetDirectory: File,
) {
    private val openApiSpec = Json.parseToJsonElement(openApiSpecJson).jsonObject

    internal val schemaObjects = openApiSpec["components"]!!.jsonObject["schemas"]!!.jsonObject
        .mapValues { it.value.jsonObject }

    fun generateSchemas() {
        schemaObjects.forEach { (schemaName, schemaObject) ->
            handleTopLevelSchema(schemaName, schemaObject)
        }
    }
}
