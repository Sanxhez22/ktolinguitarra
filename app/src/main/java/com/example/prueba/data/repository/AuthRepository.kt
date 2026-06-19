package com.example.prueba.data.repository

import com.example.prueba.FretMindApp
import com.example.prueba.api.ApiClient
import com.example.prueba.api.GoogleLoginRequest
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

    suspend fun signOut(): Result<Unit> = runCatching { store.clear() }
}

private fun UserProfileDto.toSession(token: String) = UserSession(
    id = id,
    nombre = nombre ?: "Guitarrista",
    email = email ?: "",
    foto = foto,
    nivel = nivel,
    token = token
)
