package com.example.prueba.data.repository

import com.example.prueba.api.ApiClient
import com.example.prueba.api.WilfredoAnalyzeInfo
import com.example.prueba.api.WilfredoAnalyzeRequest
import com.example.prueba.api.WilfredoChatInfo
import com.example.prueba.api.WilfredoChatRequest
import com.example.prueba.api.WilfredoChordInfo
import com.example.prueba.api.WilfredoChordRequest
import com.example.prueba.api.WilfredoPlanInfo
import com.example.prueba.api.WilfredoPlanRequest
import com.example.prueba.api.WilfredoTunerInfo
import com.example.prueba.api.WilfredoTunerRequest

class ChatRepository {
    private val api = ApiClient.fastApiService

    suspend fun sendMessage(
        message: String,
        level: String = "principiante",
        history: List<String> = emptyList()
    ): Result<WilfredoChatInfo> = runCatching {
        val response = api.wilfredoChat(WilfredoChatRequest(message, level, history))
        val body = response.body()
        if (response.isSuccessful && body?.success == true && body.data != null) {
            body.data
        } else {
            throw Exception(body?.error ?: "Error al enviar mensaje")
        }
    }

    suspend fun analyzeMetrics(
        precision: Float,
        rhythm: Float = 0f,
        bpm: Int = 120,
        note: String? = null
    ): Result<WilfredoAnalyzeInfo> = runCatching {
        val response = api.wilfredoAnalyze(WilfredoAnalyzeRequest(precision, rhythm, bpm, note))
        val body = response.body()
        if (response.isSuccessful && body?.success == true && body.data != null) {
            body.data
        } else {
            throw Exception(body?.error ?: "Error al analizar métricas")
        }
    }

    suspend fun getPracticePlan(
        level: String = "principiante",
        goal: String? = null
    ): Result<WilfredoPlanInfo> = runCatching {
        val response = api.wilfredoPlan(WilfredoPlanRequest(level, goal))
        val body = response.body()
        if (response.isSuccessful && body?.success == true && body.data != null) {
            body.data
        } else {
            throw Exception(body?.error ?: "Error al generar plan")
        }
    }

    suspend fun getTunerFeedback(
        frequency: Float,
        note: String,
        cents: Float
    ): Result<WilfredoTunerInfo> = runCatching {
        val response = api.wilfredoTuner(WilfredoTunerRequest(frequency, note, cents))
        val body = response.body()
        if (response.isSuccessful && body?.success == true && body.data != null) {
            body.data
        } else {
            throw Exception(body?.error ?: "Error al obtener feedback")
        }
    }

    suspend fun getChordFeedback(notes: List<String>): Result<WilfredoChordInfo> = runCatching {
        val response = api.wilfredoChord(WilfredoChordRequest(notes))
        val body = response.body()
        if (response.isSuccessful && body?.success == true && body.data != null) {
            body.data
        } else {
            throw Exception(body?.error ?: "Error al obtener feedback de acorde")
        }
    }
}
