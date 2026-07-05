package com.example.prueba.data.repository

import com.example.prueba.api.ApiClient
import com.example.prueba.api.CaminoResponse
import com.example.prueba.api.EjercicioDto
import com.example.prueba.api.EntrenadorResponse
import com.example.prueba.api.HabilidadesResponse
import java.util.TimeZone

/**
 * Repositorio del sistema adaptativo (P1).
 *
 * Consume los endpoints del entrenador, habilidades, camino y catálogo de
 * ejercicios. Todo el estado se calcula en el backend; aquí solo se lee.
 */
class TrainerRepository {
    private val api = ApiClient.fastApiService

    /** Offset de zona horaria del dispositivo en minutos (para objetivo del día/racha). */
    private fun tzOffsetMin(): Int =
        TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 60000

    suspend fun getEntrenador(userId: String): Result<EntrenadorResponse> = runCatching {
        val response = api.entrenador(userId, tzOffsetMin())
        val body = response.body()
        if (response.isSuccessful && body != null) body
        else throw Exception("No se pudo cargar tu entrenador (${response.code()})")
    }

    suspend fun getHabilidades(userId: String): Result<HabilidadesResponse> = runCatching {
        val response = api.habilidades(userId)
        val body = response.body()
        if (response.isSuccessful && body != null) body
        else throw Exception("No se pudieron cargar tus habilidades (${response.code()})")
    }

    suspend fun getCamino(userId: String): Result<CaminoResponse> = runCatching {
        val response = api.camino(userId)
        val body = response.body()
        if (response.isSuccessful && body != null) body
        else throw Exception("No se pudo cargar tu camino (${response.code()})")
    }

    suspend fun getEjercicio(id: String): Result<EjercicioDto> = runCatching {
        val response = api.ejercicio(id)
        val body = response.body()
        if (response.isSuccessful && body != null) body
        else throw Exception("No se pudo cargar el ejercicio (${response.code()})")
    }
}
