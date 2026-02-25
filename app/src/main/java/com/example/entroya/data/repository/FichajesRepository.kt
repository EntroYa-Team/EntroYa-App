package com.example.entroya.data.repository

import android.util.Log
import com.example.entroya.BuildConfig
import com.example.entroya.data.model.Fichaje
import com.example.entroya.data.model.Usuario
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.GoTrue
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

object FichajesRepository {

    private val client: SupabaseClient = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_ANON_KEY
    ) {
        install(GoTrue)
        install(Postgrest)
    }

    suspend fun getUsers(): List<Usuario> = withContext(Dispatchers.IO) {
        return@withContext try {
            val response = client.postgrest["usuarios"]
                .select()
                .decodeList<Usuario>()
            Log.d("FichajesRepository", "Usuarios obtenidos: ${response.size}")
            response
        } catch (e: Exception) {
            Log.e("FichajesRepository", "Error getting users: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun insertFichaje(fichaje: Fichaje): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            client.postgrest["fichajes"]
                .insert(fichaje)
            Log.d("FichajesRepository", "Fichaje insertado correctamente")
            true
        } catch (e: Exception) {
            Log.e("FichajesRepository", "Error inserting fichaje: ${e.message}", e)
            false
        }
    }
}