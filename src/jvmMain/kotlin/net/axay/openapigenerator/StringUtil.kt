package net.axay.openapigenerator

@Suppress("DEPRECATION")
fun String.toCamelCase() =
    split("_").joinToString("") { it.capitalize() }.decapitalize()

fun String.withoutSchemaPrefix() =
    if (startsWith("#/components/schemas/"))
        removePrefix("#/components/schemas/")
    else
        error("The reference '$this' does not have a valid schema prefix.")
