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
        val requestBody = file.readBytes().toRequestBody("audio/wav".toMediaTypeOrNull())
        // El backend espera un UploadFile en el campo "audio": se envía como
        // parte multipart CON filename (no como campo de formulario plano).
        val part = MultipartBody.Part.createFormData("audio", file.name, requestBody)
        val response = api.practicaAnalyze(part)
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
