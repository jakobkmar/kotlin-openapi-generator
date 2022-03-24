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
    companion object {
        private const val ref = "\$ref"
    }

    private val openApiSpec = Json.parseToJsonElement(openApiSpecJson).jsonObject

    private val schemaObjects = openApiSpec["components"]!!.jsonObject["schemas"]!!.jsonObject
        .mapValues { it.value.jsonObject }

    fun generateSchemas() {
        schemaObjects.forEach { (schemaName, schemaObject) ->
            handleTopLevelSchema(schemaName, schemaObject)
        }
    }

    private fun handleTopLevelSchema(schemaName: String, schemaObject: JsonObject) {
        val fileBuilder = FileSpec.builder(packageName, schemaName)

        fun typeFrom(typeName: String, typeObject: JsonObject): Pair<TypeName, Boolean> {
            val propTypeName = typeObject["type"]?.jsonPrimitive?.content

            return if (propTypeName == "object" || "allOf" in typeObject) {
                val classBuilder = TypeSpec.serializableDataClassBuilder(typeName)
                handleObject(classBuilder, typeName, typeObject)
                fileBuilder.addType(classBuilder.build())

                ClassName(packageName, typeName) to false
            } else when (propTypeName) {
                "array" -> {
                    List::class.asClassName().parameterizedBy(
                        typeFrom(typeName + "ArrayElement",
                            typeObject["items"]!!.jsonObject).first
                    ) to true
                }
                else -> when (propTypeName) {
                    "boolean" -> Boolean::class.asTypeName()
                    "string" -> String::class.asTypeName()
                    "integer" -> Int::class.asTypeName()
                    "number" -> Double::class.asTypeName()
                    "null" -> Any::class.asTypeName()
                    else -> error("")
                } to true
            }
        }

        val typeResult = typeFrom(schemaName, schemaObject)
        if (typeResult.second) {
            fileBuilder.addTypeAlias(TypeAliasSpec.builder(schemaName, typeResult.first).build())
        }

        fileBuilder.build().writeTo(File(".").resolve("gen"))
    }

    private fun handleObject(builder: ClassBuilderHolder, objectName: String, schemaObject: JsonObject, recursive: Boolean = true) {
        if ("allOf" in schemaObject) {
            for (allOfObject in schemaObject["allOf"]!!.jsonArray.map { it.jsonObject }) {
                if (ref in allOfObject) {
                    val refObjectName = allOfObject[ref]!!.jsonPrimitive.content.withoutSchemaPrefix()
                    handleObject(builder, refObjectName, schemaObjects[refObjectName]!!, recursive = false)
                } else {
                    handleObject(builder, objectName, allOfObject, recursive)
                }
            }
            return
        }

        fun typeFrom(typeObject: JsonObject, propName: String): TypeName {
            fun handleObjectType(): ClassName {
                val propClassName = propName.toUpperCamelCase()

                if (recursive) {
                    val classBuilder = TypeSpec.serializableDataClassBuilder(propClassName)
                    handleObject(classBuilder, propName.toUpperCamelCase(), typeObject)
                    builder.classBuilder.addType(classBuilder.build())
                }

                return ClassName(packageName, "$objectName.$propClassName")
            }

            return when (val propTypeName = typeObject["type"]?.jsonPrimitive?.content) {
                "boolean" -> Boolean::class.asTypeName()
                "string" -> String::class.asTypeName()
                "integer" -> Int::class.asTypeName()
                "number" -> Double::class.asTypeName()
                "null" -> Any::class.asTypeName()
                "object" -> handleObjectType()
                "array" -> {
                    List::class.asClassName().parameterizedBy(typeFrom(typeObject["items"]!!.jsonObject, propName))
                }
                else -> when {
                    "allOf" in typeObject -> handleObjectType()
                    ref in typeObject -> {
                        if (typeObject.size == 1) {
                            ClassName(
                                packageName,
                                typeObject[ref]!!.jsonPrimitive.content.withoutSchemaPrefix()
                            )
                        } else {
                            error("Unexpected additional values (only $ref expected) in $typeObject")
                        }
                    }
                    else -> error("Unknown type '$propTypeName' defined for '$propName' in object '$objectName'. Full property: $typeObject")
                }
            }
        }

        val requiredProps = schemaObject["required"]?.jsonArray
            ?.map { it.jsonPrimitive.content } ?: emptyList()

        val propObjects = schemaObject["properties"]?.jsonObject?.mapValues { it.value.jsonObject }
        propObjects?.forEach { (propName, propObject) ->

            val propType = typeFrom(propObject, propName)
                .copy(nullable = propName !in requiredProps || propObject["nullable"]?.jsonPrimitive?.boolean == true)

            val camelCasePropName = propName.toCamelCase()

            builder.constructorBuilder.addParameter(
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
            builder.classBuilder.addProperty(
                PropertySpec.builder(camelCasePropName, propType)
                    .initializer(camelCasePropName)
                    .build()
            )
        }
    }
}
