package com.example.entroya.data.repository

import android.util.Log
import com.example.entroya.BuildConfig
import com.example.entroya.data.model.Fichaje
import com.example.entroya.data.model.Usuario
import com.example.entroya.data.model.FichajeResponse
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.booleanOrNull
import kotlinx.datetime.Clock

object FichajesRepository {

    private val TAG = "FichajesRepository"
    private val json = Json { ignoreUnknownKeys = true }

    private val client = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_ANON_KEY
    ) {
        install(Postgrest)
        install(Auth) 
    }

    val auth = client.auth

    suspend fun getUsers(): List<Usuario> = withContext(Dispatchers.IO) {
        return@withContext try {
            val jsonElement = client.postgrest["usuarios"].select().decodeAs<JsonElement>()
            val jsonArray = jsonElement.jsonArray
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
                        activo = obj["activo"]?.jsonPrimitive?.booleanOrNull ?: true
                    )
                } catch (e: Exception) { null }
            }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun getUsuarioByEmail(email: String): Usuario? = withContext(Dispatchers.IO) {
        return@withContext try {
            val jsonElement = client.postgrest["usuarios"]
                .select { filter { eq("email", email) } }
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
                    activo = obj["activo"]?.jsonPrimitive?.booleanOrNull ?: true
                )
            } else null
        } catch (e: Exception) { null }
    }

    suspend fun getUsuarioByCredentials(email: String, passwordText: String): Usuario? = withContext(Dispatchers.IO) {
        return@withContext try {
            val jsonElement = client.postgrest["usuarios"]
                .select { 
                    filter { 
                        eq("email", email)
                        eq("password", passwordText)
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
                    activo = obj["activo"]?.jsonPrimitive?.booleanOrNull ?: true
                )
            } else null
        } catch (e: Exception) { 
            Log.e(TAG, "Error en getUsuarioByCredentials: ${e.message}")
            null 
        }
    }

    suspend fun getUsuarioByNfcId(nfcId: String): Usuario? = withContext(Dispatchers.IO) {
        return@withContext try {
            val jsonElement = client.postgrest["usuarios"]
                .select { filter { eq("nfc_id", nfcId) } }
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
                    activo = obj["activo"]?.jsonPrimitive?.booleanOrNull ?: true
                )
            } else null
        } catch (e: Exception) { null }
    }

    suspend fun getUltimoFichaje(usuarioId: Int): FichajeResponse? = withContext(Dispatchers.IO) {
        return@withContext try {
            val jsonElement = client.postgrest["fichajes"]
                .select {
                    filter { eq("usuario_id", usuarioId) }
                    order("fecha_hora", Order.DESCENDING)
                    limit(1)
                }
                .decodeAs<JsonElement>()
            val jsonArray = jsonElement.jsonArray
            if (jsonArray.isNotEmpty()) {
                json.decodeFromString<FichajeResponse>(jsonArray[0].toString())
            } else null
        } catch (e: Exception) { null }
    }

    suspend fun insertFichaje(fichaje: Fichaje): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            client.postgrest["fichajes"].insert(fichaje)
            true
        } catch (e: Exception) { false }
    }

    suspend fun asignarNfcAUsuario(usuarioId: Int, nfcId: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val usuarioExistente = getUsuarioByNfcId(nfcId)
            if (usuarioExistente != null && usuarioExistente.id != usuarioId) return@withContext false
            client.postgrest["usuarios"].update(
                mapOf("nfc_id" to nfcId),
                { filter { eq("id", usuarioId) } }
            )
            true
        } catch (e: Exception) { false }
    }
}