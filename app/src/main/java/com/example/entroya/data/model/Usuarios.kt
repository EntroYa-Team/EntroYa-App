package com.example.entroya.data.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.InternalSerializationApi

@OptIn(InternalSerializationApi::class)
@Serializable
data class Usuario(
    val id: Int,
    val email: String,
    val nombre: String,
    val rol: String,
    val departamento: String? = null,
    val telefono: String? = null,
    val activo: Boolean = true,
    @SerialName("fecha_contratacion")
    @Serializable(with = InstantSerializer::class)
    val fechaContratacion: Instant? = null,
    @SerialName("fecha_creacion")
    @Serializable(with = InstantSerializer::class)
    val fechaCreacion: Instant? = null
)