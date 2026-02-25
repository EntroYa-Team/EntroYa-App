package com.example.entroya.data.model

import kotlinx.datetime.Instant
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlin.OptIn

@OptIn(InternalSerializationApi::class)
@Serializable
data class Fichaje(

    @SerialName("id")
    val id: Int? = null,
    @SerialName("usuario_id")
    val usuarioId: Int,
    @SerialName("tipo")
    val tipo: String,
    @Serializable(with = InstantSerializer::class)
    val fechaHora: Instant,
    @SerialName("dispositivo")
    val dispositivo: String = "Móvil"


)

