package com.example.prueba.data.repository

import com.example.prueba.data.model.UserSession

class AuthRepository {

    // Stub: sesión en memoria hasta implementar
    // Google Sign-In + DataStore + backend auth
    private var currentSession: UserSession? = null

    suspend fun signInWithGoogle(idToken: String): Result<UserSession> {
        return try {
            val session = UserSession(
                id = "stub_${System.currentTimeMillis()}",
                nombre = "Usuario",
                email = "usuario@email.com",
                nivel = "Principiante",
                token = idToken
            )
            currentSession = session
            Result.success(session)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreSession(): Result<UserSession?> {
        return Result.success(currentSession)
    }

    suspend fun signOut(): Result<Unit> {
        currentSession = null
        return Result.success(Unit)
    }

    fun isLoggedIn(): Boolean = currentSession != null

    fun getCurrentSession(): UserSession? = currentSession
}
