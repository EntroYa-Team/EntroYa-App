package com.example.entroya.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.entroya.data.model.Fichaje
import com.example.entroya.data.model.Usuario
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainViewModel : ViewModel() {

    private val _uiEvents = MutableSharedFlow<String>()
    val uiEvents = _uiEvents.asSharedFlow()

    // 1. Estado para el usuario encontrado
    private val _foundUser = MutableStateFlow<Usuario?>(null)
    val foundUser = _foundUser.asStateFlow()

    // Lista privada de usuarios para simulación
    private val users = listOf(
        Usuario(id = 1, email = "carlos@email.com", nombre = "Carlos", rol = "TRABAJADOR", fecha_creacion = Date()),
        Usuario(id = 2, email = "ana@email.com", nombre = "Ana", rol = "TRABAJADOR", fecha_creacion = Date()),
        Usuario(id = 3, email = "juan@email.com", nombre = "Juan", rol = "ADMIN", fecha_creacion = Date())
    )

    // 2. Función para buscar usuario por ID
    fun findUserById(userId: String) {
        val id = userId.toIntOrNull()
        _foundUser.value = users.find { it.id == id }
    }

    private fun formatTimestamp(date: Date): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        return sdf.format(date)
    }

    fun onClockInClicked() {
        // 3. Comprobar si se ha encontrado un usuario
        val user = _foundUser.value
        if (user == null) {
            viewModelScope.launch { _uiEvents.emit("Usuario no encontrado. Introduce un ID válido.") }
            return
        }

        val nuevoFichaje = Fichaje(
            id = 0,
            usuario_id = user.id,
            tipo = "ENTRADA",
            fecha_hora = Date(),
            dispositivo = "Móvil"
        )

        val timestampString = formatTimestamp(nuevoFichaje.fecha_hora)
        viewModelScope.launch {
            _uiEvents.emit("Fichaje de entrada de ${user.nombre} registrado a las $timestampString")
        }
    }

    fun onClockOutClicked() {
        val user = _foundUser.value
        if (user == null) {
            viewModelScope.launch { _uiEvents.emit("Usuario no encontrado. Introduce un ID válido.") }
            return
        }

        val nuevoFichaje = Fichaje(
            id = 0,
            usuario_id = user.id,
            tipo = "SALIDA",
            fecha_hora = Date(),
            dispositivo = "Móvil"
        )

        val timestampString = formatTimestamp(nuevoFichaje.fecha_hora)
        viewModelScope.launch {
            _uiEvents.emit("Fichaje de salida de ${user.nombre} registrado a las $timestampString")
        }
    }
}
