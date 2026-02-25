package com.example.entroya.data.repository

import android.util.Log
import com.example.entroya.BuildConfig
import com.example.entroya.data.model.Fichaje
import com.example.entroya.data.model.Usuario
import com.example.entroya.data.model.FichajeResponse
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order  // 👈 IMPORT CORRECTO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.booleanOrNull
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

object FichajesRepository {

    private val TAG = "FichajesRepository"

    private val json = Json { ignoreUnknownKeys = true }

    private val client = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_ANON_KEY
    ) {
        install(Postgrest)
    }

    // Tu función existente para obtener usuarios
    suspend fun getUsers(): List<Usuario> = withContext(Dispatchers.IO) {
        Log.d(TAG, "🔍 Intentando obtener usuarios...")

        return@withContext try {
            val jsonElement = client.postgrest["usuarios"]
                .select()
                .decodeAs<JsonElement>()

            val jsonArray = jsonElement.jsonArray
            Log.d(TAG, "📄 JSON Array: ${jsonArray.size} elementos")

            jsonArray.mapNotNull { element ->
                try {
                    val obj = element as JsonObject
                    Usuario(
                        id = obj["id"]?.jsonPrimitive?.intOrNull ?: return@mapNotNull null,
                        email = obj["email"]?.jsonPrimitive?.contentOrNull ?: "",
                        nombre = obj["nombre"]?.jsonPrimitive?.contentOrNull ?: "",
                        rol = obj["rol"]?.jsonPrimitive?.contentOrNull ?: "",
                        departamento = obj["departamento"]?.jsonPrimitive?.contentOrNull,
                        telefono = obj["telefono"]?.jsonPrimitive?.contentOrNull,
                        activo = obj["activo"]?.jsonPrimitive?.booleanOrNull ?: true,
                        fechaContratacion = null,
                        fechaCreacion = null
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error parseando usuario: ${e.message}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error: ${e.message}")
            emptyList()
        }
    }

    // Función para obtener usuario por ID de tarjeta NFC
    suspend fun getUsuarioByNfcId(nfcId: String): Usuario? = withContext(Dispatchers.IO) {
        Log.d(TAG, "🔍 Buscando usuario con NFC ID: $nfcId")

        return@withContext try {
            val jsonElement = client.postgrest["usuarios"]
                .select {
                    filter {
                        eq("nfc_id", nfcId)  // Asumiendo que tienes una columna 'nfc_id' en la tabla
                    }
                }
                .decodeAs<JsonElement>()

            val jsonArray = jsonElement.jsonArray
            if (jsonArray.isNotEmpty()) {
                val obj = jsonArray[0] as JsonObject
                Usuario(
                    id = obj["id"]?.jsonPrimitive?.intOrNull ?: return@withContext null,
                    email = obj["email"]?.jsonPrimitive?.contentOrNull ?: "",
                    nombre = obj["nombre"]?.jsonPrimitive?.contentOrNull ?: "",
                    rol = obj["rol"]?.jsonPrimitive?.contentOrNull ?: "",
                    departamento = obj["departamento"]?.jsonPrimitive?.contentOrNull,
                    telefono = obj["telefono"]?.jsonPrimitive?.contentOrNull,
                    activo = obj["activo"]?.jsonPrimitive?.booleanOrNull ?: true,
                    fechaContratacion = null,
                    fechaCreacion = null
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error buscando usuario por NFC: ${e.message}")
            null
        }
    }

    // Función para obtener el último fichaje del usuario
    suspend fun getUltimoFichaje(usuarioId: Int): FichajeResponse? = withContext(Dispatchers.IO) {
        Log.d(TAG, "🔍 Buscando último fichaje del usuario $usuarioId")

        return@withContext try {
            val jsonElement = client.postgrest["fichajes"]
                .select {
                    filter {
                        eq("usuario_id", usuarioId)
                    }
                    order("fecha_hora", Order.DESCENDING)  // 👈 AHORA USA EL Order DE SUPABASE
                    limit(1)
                }
                .decodeAs<JsonElement>()

            val jsonArray = jsonElement.jsonArray
            if (jsonArray.isNotEmpty()) {
                val jsonString = jsonArray[0].toString()
                json.decodeFromString<FichajeResponse>(jsonString)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo último fichaje: ${e.message}")
            null
        }
    }

    // Función para insertar fichaje
    suspend fun insertFichaje(fichaje: Fichaje): Boolean = withContext(Dispatchers.IO) {
        Log.d(TAG, "📝 Insertando fichaje para usuario ${fichaje.usuarioId} - Tipo: ${fichaje.tipo}")

        return@withContext try {
            client.postgrest["fichajes"].insert(fichaje)
            Log.d(TAG, "✅ Fichaje insertado correctamente")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error insertando fichaje: ${e.message}")
            false
        }
    }
    suspend fun asignarNfcAUsuario(usuarioId: Int, nfcId: String): Boolean = withContext(Dispatchers.IO) {
        Log.d(TAG, "📝 Asignando NFC ID $nfcId al usuario $usuarioId")

        return@withContext try {
            // Primero verificamos si el NFC ID ya está asignado a otro usuario
            val usuarioExistente = getUsuarioByNfcId(nfcId)
            if (usuarioExistente != null && usuarioExistente.id != usuarioId) {
                Log.e(TAG, "❌ El NFC ID ya está asignado a otro usuario: ${usuarioExistente.nombre}")
                return@withContext false
            }

            // Actualizamos el usuario con el nuevo NFC ID
            client.postgrest["usuarios"]
                .update(
                    mapOf("nfc_id" to nfcId),
                    { filter { eq("id", usuarioId) } }
                )

            Log.d(TAG, "✅ NFC ID asignado correctamente")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error asignando NFC: ${e.message}")
            false
        }
    }
}