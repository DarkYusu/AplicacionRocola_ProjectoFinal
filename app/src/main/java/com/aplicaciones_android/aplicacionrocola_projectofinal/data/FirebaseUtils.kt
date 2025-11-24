package com.aplicaciones_android.aplicacionrocola_projectofinal.data

import android.content.Context
import com.google.firebase.FirebaseApp

object FirebaseUtils {
    fun ensureInitialized(context: Context) {
        try {
            FirebaseApp.initializeApp(context)
        } catch (e: Exception) {
            // ya inicializado o error no crítico aquí
        }
    }
}
