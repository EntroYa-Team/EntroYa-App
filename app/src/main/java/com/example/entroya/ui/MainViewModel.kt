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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    fun loginManualAndClock(emailInput: String, passwordInput: String) {
        val mail = emailInput.trim()
        val pass = passwordInput.trim()

        if (mail.isEmpty() || pass.isEmpty()) return

        viewModelScope.launch {
            try {
                val usuario = FichajesRepository.getUsuarioByCredentials(mail, pass)
                if (usuario != null) {
                    _foundUser.value = usuario
                    realizarFichaje(usuario, null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error en login automático: ${e.message}")
            }
        }
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
            val now = Date()
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

            // 🛡️ CONTROL ANTIDOUBLE-TAP (5 MINUTOS)
            if (ultimo != null) {
                try {
                    // Intentamos parsear la fecha. Supabase puede devolverla con T o espacio.
                    val fechaLimpia = ultimo.fechaHora.replace("T", " ").split(".")[0]
                    val lastDate = sdf.parse(fechaLimpia)
                    
                    if (lastDate != null) {
                        val diffMs = now.time - lastDate.time
                        val diffMin = diffMs / 60000
                        val diffSec = (diffMs / 1000) % 60

                        if (diffMin < 5) {
                            val minRestantes = 4 - diffMin
                            val secRestantes = 59 - diffSec
                            _uiEvents.emit("⏳ Espera $minRestantes min $secRestantes seg para volver a fichar")
                            return
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error comparando fechas: ${e.message}")
                }
            }

            // Si pasa el filtro, procedemos a fichar
            val fechaHoraString = sdf.format(now)
            val tipo = tipoManual ?: if (ultimo == null || ultimo.tipo == "SALIDA") "ENTRADA" else "SALIDA"

            val fichaje = Fichaje(
                usuarioId = usuario.id, 
                tipo = tipo, 
                fechaHora = fechaHoraString
            )
            
            val ok = FichajesRepository.insertFichaje(fichaje)

            if (ok) {
                _uiEvents.emit("✅ $tipo registrada: ${usuario.nombre}")
                _foundUser.value = null 
            } else {
                _uiEvents.emit("❌ Error al guardar el fichaje")
            }
        } catch (e: Exception) {
            _uiEvents.emit("❌ Error: ${e.message}")
            Log.e(TAG, "Error en realizarFichaje: ${e.message}")
        }
    }

    suspend fun asignarNfcAUsuario(usuarioId: Int, nfcId: String): Boolean {
        return FichajesRepository.asignarNfcAUsuario(usuarioId, nfcId)
    }
}
