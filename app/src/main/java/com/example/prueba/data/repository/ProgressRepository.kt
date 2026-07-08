package com.example.prueba.data.repository

import com.example.prueba.api.ApiClient
import com.example.prueba.api.ProgresoDto

/**
 * Repositorio de progreso del usuario.
 *
 * Lee GET /progreso/{user_id}: agregados reales calculados en MongoDB
 * (sesiones, promedios, racha, minutos, historial). Un usuario nuevo
 * recibe todo en cero.
 */
class ProgressRepository {
    private val api = ApiClient.fastApiService

    suspend fun getUserProgress(userId: String): Result<ProgresoDto> = runCatching {
        val response = api.progreso(userId)
        val body = response.body()
        if (response.isSuccessful && body != null) {
            body
        } else {
            throw Exception("No se pudo cargar el progreso (${response.code()})")
        }
    }
}
