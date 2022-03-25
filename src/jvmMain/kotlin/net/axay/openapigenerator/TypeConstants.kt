package net.axay.openapigenerator

import com.squareup.kotlinpoet.ClassName

object TypeConstants {
    val kotlinxSerializationSerializable = ClassName("kotlinx.serialization", "Serializable")
    val kotlinxSerializationSerialName = ClassName("kotlinx.serialization", "SerialName")

    val kotlinxDatetimeInstant = ClassName("kotlinx.datetime", "Instant")
    val kotlinxDatetimeLocalDate = ClassName("kotlinx.datetime", "LocalDate")
}
