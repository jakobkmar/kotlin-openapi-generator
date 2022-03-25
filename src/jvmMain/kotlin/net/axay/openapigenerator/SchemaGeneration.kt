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
            propTypeName == "object" || "allOf" singleIn typeObject -> {
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
            "oneOf" singleIn typeObject || "anyOf" singleIn typeObject -> {
                oneOfAnyOfWarning(typeObject)
                Any::class.asTypeName() to true
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
        val propTypeName = typeObject["type"]?.jsonPrimitive?.content

        when {
            propTypeName == "array" -> {
                List::class.asClassName().parameterizedBy(typeFrom(typeObject["items"]!!.jsonObject, propName))
            }

            propTypeName == "string" && "enum" in typeObject -> {
                val propClassName = propName.toUpperCamelCase()

                if (recursive) {
                    val enumBuilder = TypeSpec.enumBuilder(propClassName)
                    enumBuilder.addAnnotation(
                        AnnotationSpec.builder(TypeConstants.kotlinxSerializationSerializable)
                            .build()
                    )

                    val enumConstants = typeObject["enum"]!!.jsonArray
                        .map { it.jsonPrimitive.content }.map { it to it }
                    val capitalizedEnumConstants = enumConstants
                        .map { it.first.toUpperCamelCase() to it.first }

                    (if (enumConstants.size == capitalizedEnumConstants.toSet().size) capitalizedEnumConstants else enumConstants)
                        .forEach {
                            enumBuilder.addEnumConstant(
                                it.first,
                                TypeSpec.anonymousClassBuilder()
                                    .addAnnotation(
                                        AnnotationSpec.builder(TypeConstants.kotlinxSerializationSerialName)
                                            .addMember("\"${it.second}\"")
                                            .build()
                                    )
                                    .build()
                            )
                        }

                    builder.classBuilder.addType(enumBuilder.build())
                }

                ClassName(packageName, "$objectName.$propClassName")
            }
            propTypeName == "object" || "allOf" singleIn typeObject -> {
                val propClassName = propName.toUpperCamelCase()

                if (recursive) {
                    val classBuilder = TypeSpec.serializableDataClassBuilder(propClassName)
                    handleObject(classBuilder, propName.toUpperCamelCase(), typeObject)
                    builder.classBuilder.addType(classBuilder.build())
                }

                ClassName(packageName, "$objectName.$propClassName")
            }

            ref singleIn typeObject -> {
                ClassName(
                    packageName,
                    typeObject[ref]!!.jsonPrimitive.content.withoutSchemaPrefix()
                )
            }

            propTypeName == null -> {
                if ("oneOf" singleIn typeObject || "anyOf" singleIn typeObject) {
                    oneOfAnyOfWarning(typeObject)
                } else {
                    logWarning("The type is null for '$propName' defined in '$objectName'. Full property: $typeObject")
                }
                Any::class.asTypeName()
            }
            else -> null
        }?.let { return it }

        kotlin.runCatching { typeObject.getSimpleType() }.onSuccess { return it }

        error("Unknown type '$propTypeName' defined for '$propName' in object '$objectName'. Full property: $typeObject")
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
                            AnnotationSpec.builder(TypeConstants.kotlinxSerializationSerialName)
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
                null, "byte", "password", "email", "uri" -> String::class.asTypeName()
                "binary" -> ByteArray::class.asTypeName()
                "date" -> TypeConstants.kotlinxDatetimeLocalDate
                "date-time" -> TypeConstants.kotlinxDatetimeInstant
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

private fun oneOfAnyOfWarning(typeObject: JsonObject) =
    logWarning("'oneOf' and 'anyOf' and cannot be supported in a good way at the moment, falling back to Any! Full object: $typeObject")
