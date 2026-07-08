package com.example.prueba.data.repository

import com.example.prueba.FretMindApp
import com.example.prueba.api.ApiClient
import com.example.prueba.api.GoogleLoginRequest
import com.example.prueba.api.OnboardingRequest
import com.example.prueba.api.UserProfileDto
import com.example.prueba.data.local.SessionDataStore
import com.example.prueba.data.model.UserSession

/**
 * Repositorio de autenticación real (singleton).
 *
 * - signInWithGoogle: verifica el idToken contra el backend (/auth/google),
 *   persiste la sesión en DataStore y la devuelve.
 * - restoreSession / getProfile: lectura de sesión persistida y perfil remoto.
 * - signOut: limpia la sesión local.
 *
 * Es `object` (singleton) a propósito: antes cada ViewModel creaba su propia
 * instancia y la sesión no se compartía entre pantallas.
 */
object AuthRepository {
    private val api = ApiClient.authService
    private val store by lazy { SessionDataStore(FretMindApp.instance) }

    suspend fun signInWithGoogle(idToken: String): Result<UserSession> = runCatching {
        val response = api.loginGoogle(GoogleLoginRequest(idToken))
        val body = response.body()
        if (response.isSuccessful && body != null) {
            val session = body.toSession(idToken)
            store.save(session)
            session
        } else {
            throw Exception("Error de autenticación (${response.code()})")
        }
    }

    suspend fun restoreSession(): Result<UserSession?> = runCatching { store.read() }

    suspend fun getProfile(userId: String): Result<UserProfileDto> = runCatching {
        val response = api.getPerfil(userId)
        val body = response.body()
        if (response.isSuccessful && body != null) {
            body
        } else {
            throw Exception("No se pudo cargar el perfil (${response.code()})")
        }
    }

    /**
     * Envía las respuestas del onboarding (o la marca de finalización) al
     * backend y sincroniza la sesión local con el perfil resultante
     * (el nivel puede cambiar según la experiencia declarada).
     */
    suspend fun saveOnboarding(
        userId: String,
        experiencia: String? = null,
        objetivo: String? = null,
        completado: Boolean? = null,
        afinacionOmitida: Boolean? = null
    ): Result<UserSession> = runCatching {
        val response = api.saveOnboarding(
            userId,
            OnboardingRequest(
                experiencia = experiencia,
                objetivo = objetivo,
                completado = completado,
                afinacionOmitida = afinacionOmitida
            )
        )
        val body = response.body()
        if (response.isSuccessful && body != null) {
            val token = store.read()?.token ?: ""
            val session = body.toSession(token)
            store.save(session)
            session
        } else {
            throw Exception("No se pudo guardar el onboarding (${response.code()})")
        }
    }

    /**
     * Refresca la sesión local con el perfil del backend (fuente de verdad).
     * Mantiene sincronizados flags que el servidor puede cambiar por su
     * cuenta (p. ej. afinacion_omitida se apaga al practicar afinación).
     */
    suspend fun refreshSession(): UserSession? {
        val actual = store.read() ?: return null
        getProfile(actual.id).onSuccess { perfil ->
            val session = perfil.toSession(actual.token)
            store.save(session)
            return session
        }
        return actual
    }

    /**
     * Primera afinación real detectada (una cuerda en tono en el afinador):
     * apaga el aviso de "guitarra sin afinar" si estaba activo. No-op en
     * cualquier otro caso; nunca falla hacia la UI.
     */
    suspend fun marcarAfinacionRealizada() {
        val session = store.read() ?: return
        if (!session.afinacionOmitida) return
        saveOnboarding(session.id, afinacionOmitida = false)
    }

    suspend fun signOut(): Result<Unit> = runCatching { store.clear() }
}

private fun UserProfileDto.toSession(token: String) = UserSession(
    id = id,
    nombre = nombre ?: "Guitarrista",
    email = email ?: "",
    foto = foto,
    nivel = nivel,
    token = token,
    onboardingCompletado = onboardingCompletado,
    afinacionOmitida = afinacionOmitida
)
