package com.example.entroya

import android.app.Application
import android.util.Log

class EntroyaApplication : Application() {

    companion object {
        const val TAG = "EntroYa"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "🚀 Aplicación EntroYa iniciada")

        // Verificar configuración
        val url = BuildConfig.SUPABASE_URL
        val key = BuildConfig.SUPABASE_ANON_KEY

        if (url.isBlank() || key.isBlank() ||
            url.contains("tu-proyecto") || key.contains("tu-anon-key")) {
            Log.w(TAG, "⚠️ ADVERTENCIA: Credenciales de Supabase no configuradas")
            Log.w(TAG, "Configura local.properties con tus datos reales")
            Log.w(TAG, "URL actual: ${if (url.length > 30) "${url.take(30)}..." else url}")
            Log.w(TAG, "Key actual: ${if (key.length > 10) "${key.take(10)}..." else key}")
        } else {
            Log.d(TAG, "✅ Supabase configurado")
            Log.d(TAG, "URL: ${url.take(20)}...")
        }
    }
}