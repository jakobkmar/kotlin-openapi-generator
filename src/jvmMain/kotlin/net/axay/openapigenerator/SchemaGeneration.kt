package net.axay.openapigenerator

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import kotlinx.serialization.json.*
import java.io.File

private const val ref = "\$ref"

internal fun Generator.handleTopLevelSchema(schemaName: String, schemaObject: JsonObject) {
    val fileBuilder = FileSpec.builder(packageName, schemaName)

    fun typeFrom(typeName: String, typeObject: JsonObject): Pair<TypeName, Boolean> {
        val propTypeName = typeObject["type"]?.jsonPrimitive?.content

        return when {
            propTypeName == "object" || "allOf" in typeObject -> {
                val classBuilder = TypeSpec.serializableDataClassBuilder(typeName)
                handleObject(classBuilder, typeName, typeObject)
                fileBuilder.addType(classBuilder.build())

                ClassName(packageName, typeName) to false
            }
            propTypeName == "array" -> {
                List::class.asClassName().parameterizedBy(
                    typeFrom(typeName + "ArrayElement",
                        typeObject["items"]!!.jsonObject).first
                ) to true
            }
            else -> typeObject.getSimpleType() to true
        }
    }

    val typeResult = typeFrom(schemaName, schemaObject)
    if (typeResult.second) {
        fileBuilder.addTypeAlias(TypeAliasSpec.builder(schemaName, typeResult.first).build())
    }

    fileBuilder.build().writeTo(File(".").resolve("gen/src/commonMain/kotlin"))
}

internal fun Generator.handleObject(builder: ClassBuilderHolder, objectName: String, schemaObject: JsonObject, recursive: Boolean = true) {
    if ("allOf" in schemaObject) {
        for (allOfObject in schemaObject["allOf"]!!.jsonArray.map { it.jsonObject }) {
            if (ref in allOfObject) {
                val refObjectName = allOfObject[ref]!!.jsonPrimitive.content.withoutSchemaPrefix()
                if (refObjectName !in builder.handledSuperTypes) {
                    handleObject(builder, refObjectName, schemaObjects[refObjectName]!!, recursive = false)
                    builder.handledSuperTypes += refObjectName
                }
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

        kotlin.runCatching { typeObject.getSimpleType() }.onSuccess { return it }

        return when (val propTypeName = typeObject["type"]?.jsonPrimitive?.content) {
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

fun JsonObject.getSimpleType(): TypeName {
    val typeName = this["type"]?.jsonPrimitive?.content
        ?: error("Missing type field in the following object: $this")
    val formatName = this["format"]?.jsonPrimitive?.content

    fun unknownFormat() =
        logWarning("Unknown format '$formatName' for the following object: $this")

    return when (typeName) {
        "boolean" -> Boolean::class.asTypeName()
        "integer" -> {
            when (formatName) {
                null, "int32" -> Int::class
                "int64" -> Long::class
                else -> {
                    unknownFormat()
                    Int::class
                }
            }.asTypeName()
        }
        "number" -> {
            when (formatName) {
                null, "double" -> Double::class
                "float" -> Float::class
                else -> {
                    unknownFormat()
                    Double::class
                }
            }.asTypeName()
        }
        "string" -> {
            when (formatName) {
                null, "byte", "password", "email" -> String::class.asTypeName()
                "binary" -> ByteArray::class.asTypeName()
                "date" -> ClassName("kotlinx.datetime", "LocalDate")
                "date-time" -> ClassName("kotlinx.datetime", "Instant")
                else -> {
                    unknownFormat()
                    String::class.asTypeName()
                }
            }
        }
        "null" -> Any::class.asTypeName()
        else -> error("Unknown type '$typeName' in the following object: $this")
    }
}
