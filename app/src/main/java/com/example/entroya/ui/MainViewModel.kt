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

    private val _userListLiveData = MutableLiveData<List<Usuario>>()
    val userListLiveData: LiveData<List<Usuario>> = _userListLiveData

    init {
        loadUsers()
    }

    private fun loadUsers() {
        viewModelScope.launch {
            try {
                val userList = FichajesRepository.getUsers()
                _userListLiveData.postValue(userList)
            } catch (e: Exception) {
                Log.e(TAG, "Error cargando usuarios: ${e.message}")
            }
        }
    }

    fun loginManual(emailInput: String, passwordInput: String) {
        val mail = emailInput.trim()
        val pass = passwordInput.trim()

        viewModelScope.launch {
            try {
                Log.d(TAG, "🔍 Buscando credenciales en la tabla 'usuarios'...")
                
                // Buscamos directamente en la tabla por email y contraseña
                val usuario = FichajesRepository.getUsuarioByCredentials(mail, pass)
                
                if (usuario != null) {
                    _foundUser.value = usuario
                    _uiEvents.emit("✅ Bienvenido, ${usuario.nombre}")
                    Log.d(TAG, "👤 Login exitoso: ${usuario.nombre}")
                } else {
                    Log.e(TAG, "❌ Credenciales no encontradas en la tabla")
                    _uiEvents.emit("❌ Email o contraseña incorrectos")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error en login: ${e.message}")
                _uiEvents.emit("❌ Error al conectar con la base de datos")
            }
        }
    }

    private fun formatTimestamp(instant: kotlinx.datetime.Instant): String {
        val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        return "${localDateTime.dayOfMonth.toString().padStart(2, '0')}/${
            localDateTime.monthNumber.toString().padStart(2, '0')}/${
            localDateTime.year} ${
            localDateTime.hour.toString().padStart(2, '0')}:${
            localDateTime.minute.toString().padStart(2, '0')}"
    }

    fun procesarNfcTag(nfcId: String) {
        viewModelScope.launch {
            val usuario = FichajesRepository.getUsuarioByNfcId(nfcId)
            if (usuario == null) {
                _uiEvents.emit("❌ Tarjeta no registrada")
                return@launch
            }
            realizarFichaje(usuario, null)
        }
    }

    private suspend fun realizarFichaje(usuario: Usuario, tipoManual: String?) {
        try {
            val ultimo = FichajesRepository.getUltimoFichaje(usuario.id)
            val now = Clock.System.now()
            
            val tipo = tipoManual ?: if (ultimo == null || ultimo.tipo == "SALIDA") "ENTRADA" else "SALIDA"

            val fichaje = Fichaje(usuarioId = usuario.id, tipo = tipo, fechaHora = now)
            val ok = FichajesRepository.insertFichaje(fichaje)

            if (ok) {
                _uiEvents.emit("✅ $tipo registrada: ${usuario.nombre}")
            } else {
                _uiEvents.emit("❌ Error al guardar el fichaje")
            }
        } catch (e: Exception) {
            _uiEvents.emit("❌ Error: ${e.message}")
        }
    }

    fun onClockInClicked() {
        viewModelScope.launch {
            val user = _foundUser.value
            if (user == null) {
                _uiEvents.emit("❌ Identifícate primero")
                return@launch
            }
            realizarFichaje(user, "ENTRADA")
        }
    }

    fun onClockOutClicked() {
        viewModelScope.launch {
            val user = _foundUser.value
            if (user == null) {
                _uiEvents.emit("❌ Identifícate primero")
                return@launch
            }
            realizarFichaje(user, "SALIDA")
        }
    }

    suspend fun asignarNfcAUsuario(usuarioId: Int, nfcId: String): Boolean {
        return FichajesRepository.asignarNfcAUsuario(usuarioId, nfcId)
    }
}