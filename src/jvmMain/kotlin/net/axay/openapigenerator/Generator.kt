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
            val propTypeName = propObject["type"]?.jsonPrimitive?.content

            val primitiveType = when (propTypeName) {
                "boolean" -> Boolean::class.asTypeName()
                "string" -> String::class.asTypeName()
                "integer" -> Int::class.asTypeName()
                "number" -> Double::class.asTypeName()
                "null" -> Any::class.asTypeName()
                else -> {
                    if ("\$ref" in propObject) {
                        if (propObject.size == 1) {
                            ClassName(
                                packageName,
                                propObject["\$ref"]!!.jsonPrimitive.content.withoutSchemaPrefix()
                            )
                        } else {
                            error("Unexpected additional values (only \$ref expected) in $propObject")
                        }
                    } else null
                }
            }?.copy(nullable = propName !in requiredProps)

            if (primitiveType != null) {
                val camelCasePropName = propName.toCamelCase()

                constructorBuilder.addParameter(
                    ParameterSpec.builder(camelCasePropName, primitiveType)
                        .run {
                            if (camelCasePropName != propName) {
                                addAnnotation(
                                    AnnotationSpec.builder(ClassName("kotlinx.serialization", "SerialName"))
                                        .addMember("\"$propName\"")
                                        .build()
                                )
                            } else this
                        }
                        .build()
                )
                builder.addProperty(
                    PropertySpec.builder(camelCasePropName, primitiveType)
                        .initializer(camelCasePropName)
                        .build()
                )
            } else if (propTypeName == "object") {

            } else if (propTypeName == "array") {

            } else {
                error("Unknown type '$propTypeName' defined for $propName in object $objectName")
            }
        }

        builder.primaryConstructor(constructorBuilder.build())
        return builder.build()
    }
}
