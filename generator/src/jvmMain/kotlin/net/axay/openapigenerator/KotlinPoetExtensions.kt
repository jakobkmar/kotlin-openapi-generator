package net.axay.openapigenerator

import com.squareup.kotlinpoet.*

fun TypeSpec.Companion.serializableDataClassBuilder(name: String): ClassBuilderHolder {
    return ClassBuilderHolder(
        name,
        classBuilder(name)
            .addModifiers(KModifier.DATA)
            .addAnnotation(
                AnnotationSpec.builder(TypeConstants.kotlinxSerializationSerializable)
                    .build()
            ),
        FunSpec.constructorBuilder(),
    )
}
