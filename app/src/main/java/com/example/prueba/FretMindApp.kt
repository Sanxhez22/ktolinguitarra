package com.example.prueba

import android.app.Application

/**
 * Application personalizada. Expone el contexto de aplicación de forma global
 * para componentes singleton sin framework de DI (p. ej. SessionDataStore).
 */
class FretMindApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: FretMindApp
            private set
    }
}
