package com.example.prueba.data.repository

import com.example.prueba.api.ApiClient
import com.example.prueba.api.BibliotecaItemDto
import com.example.prueba.api.BibliotecaResponse
import com.example.prueba.api.BibliotecaUpdateRequest
import com.example.prueba.api.BusquedaCancionesResponse
import com.example.prueba.api.CancionDetalleDto
import com.example.prueba.api.PlanCancionDto
import com.example.prueba.api.PracticaGuiadaCancionDto
import retrofit2.Response

/**
 * Repositorio de canciones y biblioteca (Song Detail).
 *
 * Consume exclusivamente el backend de FretMind (/canciones, /biblioteca):
 * la integración con Songsterr/iTunes vive en FastAPI con caché en Mongo.
 */
class SongRepository {
    private val api = ApiClient.fastApiService

    private fun <T> Response<T>.orThrow(msg: String): T {
        val b = body()
        if (isSuccessful && b != null) return b
        throw Exception("$msg (${code()})")
    }

    suspend fun buscar(query: String, size: Int = 15, desde: Int = 0): Result<BusquedaCancionesResponse> =
        runCatching { api.buscarCanciones(query, size, desde).orThrow("Error al buscar canciones") }

    suspend fun detalle(songId: Long): Result<CancionDetalleDto> =
        runCatching { api.cancionDetalle(songId).orThrow("Error al cargar la canción") }

    suspend fun plan(songId: Long, userId: String): Result<PlanCancionDto> =
        runCatching { api.cancionPlan(songId, userId).orThrow("Error al generar el plan") }

    suspend fun practicar(songId: Long, userId: String): Result<PlanCancionDto> =
        runCatching { api.cancionPracticar(songId, userId).orThrow("Error al iniciar la práctica") }

    suspend fun practicaGuiada(songId: Long, userId: String): Result<PracticaGuiadaCancionDto> =
        runCatching {
            api.cancionPracticaGuiada(songId, userId).orThrow("Error al cargar la práctica guiada")
        }

    suspend fun biblioteca(userId: String, tipo: String? = null): Result<BibliotecaResponse> =
        runCatching { api.biblioteca(userId, tipo).orThrow("Error al cargar la biblioteca") }

    suspend fun estadoBiblioteca(userId: String, songId: Long): Result<BibliotecaItemDto> =
        runCatching { api.bibliotecaEstado(userId, songId).orThrow("Error al consultar la biblioteca") }

    suspend fun actualizarBiblioteca(
        userId: String,
        songId: Long,
        guardada: Boolean? = null,
        favorita: Boolean? = null
    ): Result<BibliotecaItemDto> = runCatching {
        api.bibliotecaActualizar(userId, songId, BibliotecaUpdateRequest(guardada, favorita))
            .orThrow("Error al actualizar la biblioteca")
    }
}
