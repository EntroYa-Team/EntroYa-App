package com.example.entroya.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class Fichaje(
    @SerialName("id")
    val id: Int? = null,
    @SerialName("usuario_id")
    val usuarioId: Int,
    @SerialName("tipo")
    val tipo: String,
    @SerialName("fecha_hora")
    val fechaHora: String, // Cambiado a String para enviar hora local exacta
    @SerialName("dispositivo")
    val dispositivo: String = "Móvil"
)
