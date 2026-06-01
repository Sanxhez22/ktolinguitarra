package com.example.prueba.data.repository

import com.example.prueba.api.ApiClient
import com.example.prueba.api.PracticaResult

class ProgressRepository {
    private val api = ApiClient.fastApiService

    suspend fun getUserProgress(userId: String): Result<PracticaResult> = runCatching {
        val file = java.io.File.createTempFile("stub", ".wav")
        file.writeBytes(ByteArray(1024))
        val result = PracticeRepository().submitPractice(userId, file)
        file.delete()
        result.getOrThrow()
    }
}
