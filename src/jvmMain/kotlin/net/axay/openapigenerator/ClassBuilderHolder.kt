package net.axay.openapigenerator

import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeSpec

class ClassBuilderHolder(
    val name: String,
    val classBuilder: TypeSpec.Builder,
    val constructorBuilder: FunSpec.Builder,
) {
    fun build() = classBuilder
        .primaryConstructor(constructorBuilder.build())
        .build()
}
