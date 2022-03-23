package net.axay.openapigenerator

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
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
            val fileBuilder = FileSpec.builder(packageName, schemaName)
            val classBuilder = TypeSpec.serializableDataClassBuilder(schemaName)

            val objectDefinitions = mutableListOf<JsonObject>()
            if ("type" in schemaObject) {
                if (schemaObject["type"]!!.jsonPrimitive.content == "object") {
                    objectDefinitions += schemaObject
                } else {

                }
            } else if ("allOf" in schemaObject) {
                schemaObject["allOf"]!!.jsonArray.forEach {
                    objectDefinitions += it.jsonObject
                }
            }

            for (definition in objectDefinitions) {
                handleObject(classBuilder, schemaName, definition)
            }

            fileBuilder.addType(classBuilder.build())
            fileBuilder.build().writeTo(File(".").resolve("gen"))
        }

        schemaObjects.forEach { (schemaName, schemaObject) ->
            generate(schemaName, schemaObject)
        }
    }

    private fun handleObject(builder: TypeSpec.Builder, objectName: String, schemaObject: JsonObject) {
        if ("allOf" in schemaObject) {
            return
        }

        fun typeFrom(typeObject: JsonObject, propName: String): TypeName {
            return when (val propTypeName = typeObject["type"]?.jsonPrimitive?.content) {
                "boolean" -> Boolean::class.asTypeName()
                "string" -> String::class.asTypeName()
                "integer" -> Int::class.asTypeName()
                "number" -> Double::class.asTypeName()
                "null" -> Any::class.asTypeName()
                "object" -> {
                    val propClassName = propName.toUpperCamelCase()
                    val classBuilder = TypeSpec.serializableDataClassBuilder(propClassName)
                    handleObject(classBuilder, propName.toUpperCamelCase(), typeObject)
                    builder.addType(classBuilder.build())

                    ClassName(packageName, "$objectName.$propClassName")
                }
                "array" -> {
                    List::class.asClassName().parameterizedBy(typeFrom(typeObject["items"]!!.jsonObject, propName))
                }
                else -> {
                    if ("\$ref" in typeObject) {
                        if (typeObject.size == 1) {
                            ClassName(
                                packageName,
                                typeObject["\$ref"]!!.jsonPrimitive.content.withoutSchemaPrefix()
                            )
                        } else {
                            error("Unexpected additional values (only \$ref expected) in $typeObject")
                        }
                    } else {
                        error("Unknown type '$propTypeName' defined for '$propName' in object '$objectName'. Full property: $typeObject")
                    }
                }
            }
        }

        val constructorBuilder = FunSpec.constructorBuilder()

        val requiredProps = schemaObject["required"]?.jsonArray
            ?.map { it.jsonPrimitive.content } ?: emptyList()

        val propObjects = schemaObject["properties"]?.jsonObject?.mapValues { it.value.jsonObject }
        propObjects?.forEach { (propName, propObject) ->

            val propType = typeFrom(propObject, propName)
                .copy(nullable = propName !in requiredProps || propObject["nullable"]?.jsonPrimitive?.boolean == true)

            val camelCasePropName = propName.toCamelCase()

            constructorBuilder.addParameter(
                ParameterSpec.builder(camelCasePropName, propType)
                    .apply {
                        if (camelCasePropName != propName)
                            addAnnotation(
                                AnnotationSpec.builder(ClassName("kotlinx.serialization", "SerialName"))
                                    .addMember("\"$propName\"")
                                    .build()
                            )
                    }
                    .apply {
                        if (propName !in requiredProps)
                            defaultValue("null")
                    }
                    .build()
            )
            builder.addProperty(
                PropertySpec.builder(camelCasePropName, propType)
                    .initializer(camelCasePropName)
                    .build()
            )
        }

        builder.primaryConstructor(constructorBuilder.build())
    }
}
