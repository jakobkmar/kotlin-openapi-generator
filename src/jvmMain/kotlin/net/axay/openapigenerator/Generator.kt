package net.axay.openapigenerator

import com.squareup.kotlinpoet.*
import kotlinx.serialization.json.*
import java.io.File

fun main() {
    Generator(File("modrinth-openapi.json").readText(), "net.axay.openapitest")
        .generateSchemas()
}

class Generator(
    openApiSpecJson: String,
    private val packageName: String,
) {
    private val openApiSpec = Json.parseToJsonElement(openApiSpecJson).jsonObject

    private val schemaObjects = openApiSpec["components"]!!.jsonObject["schemas"]!!.jsonObject
        .mapValues { it.value.jsonObject }

    fun generateSchemas() {

        fun generate(schemaName: String, schemaObject: JsonObject) {
            val builder = FileSpec.builder(packageName, schemaName)

            if ("type" in schemaObject) {
                if (schemaObject["type"]!!.jsonPrimitive.content == "object") {
                    builder.addType(handleObject(schemaName, schemaObject))
                } else {

                }
            } else if ("allOf" in schemaObject) {

            }

            builder.build().writeTo(System.out)
            println("---")
            println()
        }

        schemaObjects.forEach { (schemaName, schemaObject) ->
            generate(schemaName, schemaObject)
        }
    }

    private fun handleObject(objectName: String, schemaObject: JsonObject): TypeSpec {
        val builder = TypeSpec.classBuilder(objectName)

        builder.addAnnotation(
            AnnotationSpec.builder(ClassName("kotlinx.serialization", "Serializable"))
                .build()
        )

        val constructorBuilder = FunSpec.constructorBuilder()

        val requiredProps = schemaObject["required"]?.jsonArray
            ?.map { it.jsonPrimitive.content } ?: emptyList()

        val propObjects = schemaObject["properties"]?.jsonObject?.mapValues { it.value.jsonObject }
        propObjects?.forEach { (propName, propObject) ->
            if ("type" !in propObject) {
                print(propObject)
            }

            val propType = propObject["type"]!!.jsonPrimitive.content

            if (propType == "object") {

            } else if (propType == "array") {

            } else {
                val primitiveType = when (val typeName = propObject["type"]!!.jsonPrimitive.content) {
                    "boolean" -> Boolean::class
                    "string" -> String::class
                    "integer" -> Int::class
                    "number" -> Double::class
                    "null" -> Any::class
                    else -> error("Unknown type '$typeName' defined for $propName in object $objectName")
                }.asTypeName().copy(nullable = propName !in requiredProps)

                constructorBuilder.addParameter(propName, primitiveType)
                builder.addProperty(
                    PropertySpec.builder(propName, primitiveType)
                        .initializer(propName)
                        .build()
                )
            }
        }

        builder.primaryConstructor(constructorBuilder.build())
        return builder.build()
    }
}
