package net.axay.openapigenerator

@Suppress("DEPRECATION")
fun String.toCamelCase() =
    split("_").joinToString("") { it.capitalize() }.decapitalize()
