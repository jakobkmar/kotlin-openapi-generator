package net.axay.openapigenerator

@Suppress("DEPRECATION")
fun String.toCamelCase() =
    toUpperCamelCase().decapitalize()

@Suppress("DEPRECATION")
fun String.toUpperCamelCase() =
    split("_").joinToString("") { it.capitalize() }

fun String.withoutSchemaPrefix() =
    if (startsWith("#/components/schemas/"))
        removePrefix("#/components/schemas/")
    else
        error("The reference '$this' does not have a valid schema prefix.")
