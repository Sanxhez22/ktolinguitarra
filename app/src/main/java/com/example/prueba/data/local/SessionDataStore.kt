package com.example.prueba.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.prueba.data.model.UserSession
import kotlinx.coroutines.flow.first

private val Context.dataStore by preferencesDataStore(name = "fretmind_session")

/**
 * Persistencia de la sesión del usuario con DataStore Preferences.
 * Guarda la identidad devuelta por /auth/google para sobrevivir reinicios.
 */
class SessionDataStore(private val context: Context) {

    private object Keys {
        val ID = stringPreferencesKey("id")
        val NOMBRE = stringPreferencesKey("nombre")
        val EMAIL = stringPreferencesKey("email")
        val FOTO = stringPreferencesKey("foto")
        val NIVEL = stringPreferencesKey("nivel")
        val TOKEN = stringPreferencesKey("token")
    }

    suspend fun save(session: UserSession) {
        context.dataStore.edit { p ->
            p[Keys.ID] = session.id
            p[Keys.NOMBRE] = session.nombre
            p[Keys.EMAIL] = session.email
            if (session.foto != null) p[Keys.FOTO] = session.foto else p.remove(Keys.FOTO)
            p[Keys.NIVEL] = session.nivel
            p[Keys.TOKEN] = session.token
        }
    }

    suspend fun read(): UserSession? {
        val p = context.dataStore.data.first()
        val id = p[Keys.ID] ?: return null
        return UserSession(
            id = id,
            nombre = p[Keys.NOMBRE] ?: "",
            email = p[Keys.EMAIL] ?: "",
            foto = p[Keys.FOTO],
            nivel = p[Keys.NIVEL] ?: "principiante",
            token = p[Keys.TOKEN] ?: ""
        )
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
