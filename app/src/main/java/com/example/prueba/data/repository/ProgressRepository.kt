package com.example.prueba.data.repository

import com.example.prueba.api.PracticaResult

/**
 * Repositorio de progreso del usuario.
 *
 * DEUDA TÉCNICA (auditoría A2): el backend aún NO expone un endpoint de
 * lectura de progreso (p. ej. GET /progreso/{user_id}). La versión anterior
 * posteaba un WAV vacío de 1KB a POST /practica para "obtener" progreso, lo
 * que: (a) ensuciaba MongoDB con sesiones basura en cada carga de pantalla y
 * (b) fallaba al intentar analizar un audio inválido.
 *
 * Hasta que exista ese endpoint, devolvemos un fallo controlado: las pantallas
 * (Home/Progress) ya manejan onFailure y muestran sus valores por defecto.
 */
class ProgressRepository {

    @Suppress("UNUSED_PARAMETER")
    suspend fun getUserProgress(userId: String): Result<PracticaResult> =
        Result.failure(
            UnsupportedOperationException(
                "Endpoint de progreso no disponible aún (ver auditoría A2)."
            )
        )
}
