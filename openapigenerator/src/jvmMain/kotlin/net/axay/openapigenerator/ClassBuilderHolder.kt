package net.axay.openapigenerator

import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec

class ClassBuilderHolder(
    val name: String,
    val classBuilder: TypeSpec.Builder,
    val constructorBuilder: FunSpec.Builder,
) {
    val requiredProps = HashSet<String>()
    val handledSuperTypes = HashSet<String>()

    fun build() = classBuilder
        .primaryConstructor(constructorBuilder.build())
        .apply {
            if (constructorBuilder.parameters.isEmpty())
                modifiers.remove(KModifier.DATA)
        }
        .build()
}
