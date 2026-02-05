package com.example.entroya.data.model

import java.util.Date

data class Usuario(
    val id: Int,
    val email: String,
    val nombre: String,
    val rol: String,
    val departamento: String? = null,
    val telefono: String? = null,
    val activo: Boolean = true,
    val fecha_contratacion: Date? = null,
    val fecha_creacion: Date
)
