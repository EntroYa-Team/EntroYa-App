package com.example.entroya.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.entroya.data.model.Fichaje
import com.example.entroya.data.model.Usuario
import com.example.entroya.data.repository.FichajesRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime


class MainViewModel : ViewModel() {

    private val _uiEvents = MutableSharedFlow<String>()
    val uiEvents = _uiEvents.asSharedFlow()

    private val _foundUser = MutableStateFlow<Usuario?>(null)
    val foundUser = _foundUser.asStateFlow()

    private var userList: List<Usuario> = emptyList()

    init {
        loadUsers()
    }

    private fun loadUsers() {
        viewModelScope.launch {
            userList = FichajesRepository.getUsers()
        }
    }

    fun findUserById(userId: String) {
        val id = userId.toIntOrNull()
        _foundUser.value = userList.find { it.id == id }
    }

    // Función corregida para formatear la fecha y hora
    private fun formatTimestamp(instant: Instant): String {
        val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        val day = localDateTime.dayOfMonth.toString().padStart(2, '0')
        val month = localDateTime.monthNumber.toString().padStart(2, '0')
        val year = localDateTime.year
        val hour = localDateTime.hour.toString().padStart(2, '0')
        val minute = localDateTime.minute.toString().padStart(2, '0')
        val second = localDateTime.second.toString().padStart(2, '0')
        return "$day/$month/$year $hour:$minute:$second"
    }

    fun onClockInClicked() {
        viewModelScope.launch {
            val user = _foundUser.value
            if (user == null) {
                _uiEvents.emit("Usuario no encontrado. Introduce un ID válido.")
                return@launch
            }

            val now = Clock.System.now()
            val nuevoFichaje = Fichaje(
                usuarioId = user.id,
                tipo = "ENTRADA",
                fechaHora = now
            )

            FichajesRepository.insertFichaje(nuevoFichaje)

            val timestampString = formatTimestamp(now)
            _uiEvents.emit("Fichaje de entrada de ${user.nombre} guardado a las $timestampString")
        }
    }

    fun onClockOutClicked() {
        viewModelScope.launch {
            val user = _foundUser.value
            if (user == null) {
                _uiEvents.emit("Usuario no encontrado. Introduce un ID válido.")
                return@launch
            }

            val now = Clock.System.now()
            val nuevoFichaje = Fichaje(
                usuarioId = user.id,
                tipo = "SALIDA",
                fechaHora = now
            )

            FichajesRepository.insertFichaje(nuevoFichaje)

            val timestampString = formatTimestamp(now)
            _uiEvents.emit("Fichaje de salida de ${user.nombre} guardado a las $timestampString")
        }
    }
}
