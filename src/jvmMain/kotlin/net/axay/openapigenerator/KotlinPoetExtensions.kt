package net.axay.openapigenerator

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec

fun TypeSpec.Companion.serializableDataClassBuilder(name: String): TypeSpec.Builder {
    return classBuilder(name)
        .addModifiers(KModifier.DATA)
        .addAnnotation(
            AnnotationSpec.builder(ClassName("kotlinx.serialization", "Serializable"))
                .build()
        )
}
