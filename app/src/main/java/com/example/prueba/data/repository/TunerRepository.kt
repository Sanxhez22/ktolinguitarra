package com.example.prueba.data.repository

import com.example.prueba.api.AnalyzeFileInfo
import com.example.prueba.api.AnalyzeInfo
import com.example.prueba.api.AnalyzeRawInfo
import com.example.prueba.api.ApiClient
import com.example.prueba.api.CalibrateInfo
import com.example.prueba.api.ChordInfo
import com.example.prueba.api.ChordRequest
import com.example.prueba.api.PitchInfo
import com.example.prueba.api.PitchRequest
import com.example.prueba.api.PipelineInfo
import com.example.prueba.api.VerifyInfo
import com.example.prueba.api.VerifyRequest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class TunerRepository {
    private val api = ApiClient.fastApiService

    suspend fun detectPitch(frequency: Float): Result<PitchInfo> = runCatching {
        val response = api.detectPitch(PitchRequest(frequency))
        val body = response.body()
        if (response.isSuccessful && body?.success == true && body.data != null) {
            body.data
        } else {
            throw Exception(body?.error ?: "Error al detectar nota")
        }
    }

    suspend fun verifyPitch(frequency: Float, expectedNote: String): Result<VerifyInfo> = runCatching {
        val response = api.verifyPitch(VerifyRequest(frequency, expectedNote))
        val body = response.body()
        if (response.isSuccessful && body?.success == true && body.data != null) {
            body.data
        } else {
            throw Exception(body?.error ?: "Error al verificar nota")
        }
    }

    suspend fun identifyChord(notes: String): Result<ChordInfo> = runCatching {
        val response = api.identifyChord(ChordRequest(notes))
        val body = response.body()
        if (response.isSuccessful && body?.success == true && body.data != null) {
            body.data
        } else {
            throw Exception(body?.error ?: "Error al identificar acorde")
        }
    }

    suspend fun analyzeFrequency(frequency: Float): Result<AnalyzeInfo> = runCatching {
        val response = api.tunerAnalyze(PitchRequest(frequency))
        val body = response.body()
        if (response.isSuccessful && body?.success == true && body.data != null) {
            body.data
        } else {
            throw Exception(body?.error ?: "Error en análisis de frecuencia")
        }
    }

    suspend fun analyzeAudioFile(file: File): Result<AnalyzeFileInfo> = runCatching {
        val part = file.toMultipartPart("file")
        val response = api.tunerAnalyzeFile(part)
        val body = response.body()
        if (response.isSuccessful && body?.success == true && body.data != null) {
            body.data
        } else {
            throw Exception(body?.error ?: "Error al analizar archivo")
        }
    }

    suspend fun analyzeRawAudio(file: File): Result<AnalyzeRawInfo> = runCatching {
        val part = file.toMultipartPart("file")
        val response = api.tunerAnalyzeRaw(part)
        val body = response.body()
        if (response.isSuccessful && body?.success == true && body.data != null) {
            body.data
        } else {
            throw Exception(body?.error ?: "Error en análisis raw")
        }
    }

    suspend fun calibrate(file: File): Result<CalibrateInfo> = runCatching {
        val part = file.toMultipartPart("file")
        val response = api.tunerCalibrate(part)
        val body = response.body()
        if (response.isSuccessful && body?.success == true && body.data != null) {
            body.data
        } else {
            throw Exception(body?.error ?: "Error al calibrar")
        }
    }

    suspend fun getPipelineInfo(): Result<PipelineInfo> = runCatching {
        val response = api.tunerPipeline()
        val body = response.body()
        if (response.isSuccessful && body?.success == true && body.data != null) {
            body.data
        } else {
            throw Exception(body?.error ?: "Error al obtener pipeline")
        }
    }

    private fun File.toMultipartPart(fieldName: String): MultipartBody.Part {
        val mediaType = extensionToMediaType()
        val requestBody = readBytes().toRequestBody(mediaType)
        return MultipartBody.Part.createFormData(fieldName, name, requestBody)
    }

    private fun File.extensionToMediaType() = when (extension.lowercase()) {
        "wav" -> "audio/wav".toMediaTypeOrNull()
        "mp3" -> "audio/mpeg".toMediaTypeOrNull()
        "m4a" -> "audio/mp4".toMediaTypeOrNull()
        "ogg" -> "audio/ogg".toMediaTypeOrNull()
        "webm" -> "audio/webm".toMediaTypeOrNull()
        else -> "audio/wav".toMediaTypeOrNull()
    }
}
