package com.example.prueba.data.repository

import com.example.prueba.api.ApiClient
import com.example.prueba.api.PracticaAnalyzeInfo
import com.example.prueba.api.PracticaResult
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class PracticeRepository {
    private val api = ApiClient.fastApiService

    suspend fun analyzeAudio(file: File): Result<PracticaAnalyzeInfo> = runCatching {
        val audioBytes = file.readBytes()
        val requestBody = audioBytes.toRequestBody("audio/wav".toMediaTypeOrNull())
        val response = api.practicaAnalyze(requestBody)
        val body = response.body()
        if (response.isSuccessful && body != null) {
            body
        } else {
            throw Exception("Error al analizar práctica")
        }
    }

    suspend fun submitPractice(userId: String, file: File): Result<PracticaResult> = runCatching {
        val mediaType = "audio/wav".toMediaTypeOrNull()
        val requestBody = file.readBytes().toRequestBody(mediaType)
        val part = MultipartBody.Part.createFormData("file", file.name, requestBody)
        val response = api.practica(userId, part)
        val body = response.body()
        if (response.isSuccessful && body != null) {
            body
        } else {
            throw Exception("Error al enviar práctica")
        }
    }
}
