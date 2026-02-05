package com.example.entroya.data.model

import java.util.Date

data class Fichaje(
    val id: Int,
    val usuario_id: Int,
    val tipo: String,
    val fecha_hora: Date,
    val dispositivo: String = "NFC"
)
