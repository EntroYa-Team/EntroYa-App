package com.example.entroya.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.InternalSerializationApi

@OptIn(InternalSerializationApi::class)
@Serializable
data class FichajeResponse(
    @SerialName("id")
    val id: Int,
    @SerialName("usuario_id")
    val usuarioId: Int,
    @SerialName("tipo")
    val tipo: String,
    @SerialName("fecha_hora")
    val fechaHora: String
)