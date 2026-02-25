package com.example.entroya.ui

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
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
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class MainViewModel : ViewModel() {

    private val TAG = "MainViewModel"

    private val _uiEvents = MutableSharedFlow<String>()
    val uiEvents = _uiEvents.asSharedFlow()

    private val _foundUser = MutableStateFlow<Usuario?>(null)
    val foundUser = _foundUser.asStateFlow()

    private var userList: List<Usuario> = emptyList()

    // LiveData para el spinner de usuarios
    private val _userListLiveData = MutableLiveData<List<Usuario>>()
    val userListLiveData: LiveData<List<Usuario>> = _userListLiveData

    init {
        Log.d(TAG, "🚀 ViewModel iniciado")
        loadUsers()
    }

    private fun loadUsers() {
        viewModelScope.launch {
            Log.d(TAG, "🔄 Cargando usuarios...")
            try {
                userList = FichajesRepository.getUsers()
                _userListLiveData.postValue(userList)  // Actualizar LiveData
                if (userList.isEmpty()) {
                    Log.e(TAG, "⚠️ No hay usuarios en la base de datos")
                    _uiEvents.emit("⚠️ No hay usuarios en la base de datos")
                } else {
                    Log.d(TAG, "✅ Usuarios cargados: ${userList.size}")
                    _uiEvents.emit("✅ Usuarios cargados correctamente: ${userList.size} usuarios")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error al cargar usuarios: ${e.message}")
                _uiEvents.emit("❌ Error al cargar usuarios: ${e.message}")
            }
        }
    }

    fun findUserById(userId: String) {
        Log.d(TAG, "🔍 Buscando usuario con ID: $userId")
        val id = userId.toIntOrNull()

        if (id == null) {
            Log.d(TAG, "❌ ID inválido: no es un número")
            _foundUser.value = null
            return
        }

        val user = userList.find { it.id == id }
        if (user != null) {
            Log.d(TAG, "✅ Usuario encontrado: ${user.nombre}")
        } else {
            Log.d(TAG, "❌ Usuario no encontrado con ID: $id")
            Log.d(TAG, "📋 IDs disponibles: ${userList.map { it.id }}")
        }
        _foundUser.value = user
    }

    private fun formatTimestamp(instant: kotlinx.datetime.Instant): String {
        val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        return "${localDateTime.dayOfMonth.toString().padStart(2, '0')}/${
            localDateTime.monthNumber.toString().padStart(2, '0')}/${
            localDateTime.year} ${
            localDateTime.hour.toString().padStart(2, '0')}:${
            localDateTime.minute.toString().padStart(2, '0')}:${
            localDateTime.second.toString().padStart(2, '0')}"
    }

    // Procesar tarjeta NFC para fichaje automático
    fun procesarNfcTag(nfcId: String) {
        viewModelScope.launch {
            Log.d(TAG, "📱 Procesando tarjeta NFC: $nfcId")

            // Buscar usuario por NFC ID
            val usuario = FichajesRepository.getUsuarioByNfcId(nfcId)

            if (usuario == null) {
                _uiEvents.emit("❌ Tarjeta no asociada a ningún usuario")
                return@launch
            }

            // Buscar último fichaje del usuario
            val ultimoFichaje = FichajesRepository.getUltimoFichaje(usuario.id)

            val now = Clock.System.now()
            val tipoFichaje = if (ultimoFichaje == null || ultimoFichaje.tipo == "SALIDA") {
                "ENTRADA"
            } else {
                "SALIDA"
            }

            val nuevoFichaje = Fichaje(
                usuarioId = usuario.id,
                tipo = tipoFichaje,
                fechaHora = now
            )

            val success = FichajesRepository.insertFichaje(nuevoFichaje)

            if (success) {
                val timestampString = formatTimestamp(now)
                val mensaje = when (tipoFichaje) {
                    "ENTRADA" -> "✅ ENTRADA de ${usuario.nombre} a las $timestampString"
                    else -> "✅ SALIDA de ${usuario.nombre} a las $timestampString"
                }
                _uiEvents.emit(mensaje)
            } else {
                _uiEvents.emit("❌ Error al guardar el fichaje")
            }
        }
    }

    // Asignar tarjeta NFC a un usuario
    suspend fun asignarNfcAUsuario(usuarioId: Int, nfcId: String): Boolean {
        Log.d(TAG, "📝 Asignando NFC $nfcId al usuario $usuarioId")
        return FichajesRepository.asignarNfcAUsuario(usuarioId, nfcId)
    }

    fun onClockInClicked() {
        viewModelScope.launch {
            val user = _foundUser.value
            if (user == null) {
                _uiEvents.emit("❌ Usuario no encontrado. Introduce un ID válido.")
                return@launch
            }

            Log.d(TAG, "🔄 Procesando fichaje de ENTRADA para ${user.nombre}")
            val now = Clock.System.now()
            val nuevoFichaje = Fichaje(
                usuarioId = user.id,
                tipo = "ENTRADA",
                fechaHora = now
            )

            val success = FichajesRepository.insertFichaje(nuevoFichaje)

            if (success) {
                val timestampString = formatTimestamp(now)
                _uiEvents.emit("✅ Fichaje de ENTRADA de ${user.nombre} guardado a las $timestampString")
            } else {
                _uiEvents.emit("❌ Error al guardar el fichaje")
            }
        }
    }

    fun onClockOutClicked() {
        viewModelScope.launch {
            val user = _foundUser.value
            if (user == null) {
                _uiEvents.emit("❌ Usuario no encontrado. Introduce un ID válido.")
                return@launch
            }

            Log.d(TAG, "🔄 Procesando fichaje de SALIDA para ${user.nombre}")
            val now = Clock.System.now()
            val nuevoFichaje = Fichaje(
                usuarioId = user.id,
                tipo = "SALIDA",
                fechaHora = now
            )

            val success = FichajesRepository.insertFichaje(nuevoFichaje)

            if (success) {
                val timestampString = formatTimestamp(now)
                _uiEvents.emit("✅ Fichaje de SALIDA de ${user.nombre} guardado a las $timestampString")
            } else {
                _uiEvents.emit("❌ Error al guardar el fichaje")
            }
        }
    }
}