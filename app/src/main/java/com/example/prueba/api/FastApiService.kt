package com.example.prueba.api

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface FastApiService {

    // ==========================================
    // TUNER ENDPOINTS
    // ==========================================

    @POST("/tuner/pitch")
    suspend fun detectPitch(@Body request: PitchRequest): Response<ApiResponse<PitchInfo>>

    @POST("/tuner/verify")
    suspend fun verifyPitch(@Body request: VerifyRequest): Response<ApiResponse<VerifyInfo>>

    @POST("/tuner/identify")
    suspend fun identifyChord(@Body request: ChordRequest): Response<ApiResponse<ChordInfo>>

    @Multipart
    @POST("/tuner/pitch/file")
    suspend fun detectPitchFromFile(@Part file: MultipartBody.Part): Response<ApiResponse<PitchFileInfo>>

    @Multipart
    @POST("/tuner/chord/file")
    suspend fun detectChordFromFile(@Part file: MultipartBody.Part): Response<ApiResponse<ChordFileInfo>>

    @POST("/tuner/analyze")
    suspend fun tunerAnalyze(@Body request: PitchRequest): Response<ApiResponse<AnalyzeInfo>>

    @Multipart
    @POST("/tuner/analyze/file")
    suspend fun tunerAnalyzeFile(@Part file: MultipartBody.Part): Response<ApiResponse<AnalyzeFileInfo>>

    @Multipart
    @POST("/tuner/analyze/raw")
    suspend fun tunerAnalyzeRaw(@Part file: MultipartBody.Part): Response<ApiResponse<AnalyzeRawInfo>>

    @Multipart
    @POST("/tuner/calibrate")
    suspend fun tunerCalibrate(@Part file: MultipartBody.Part): Response<ApiResponse<CalibrateInfo>>

    @GET("/tuner/pipeline")
    suspend fun tunerPipeline(): Response<ApiResponse<PipelineInfo>>

    // ==========================================
    // WILFREDO ENDPOINTS
    // ==========================================

    @POST("/wilfredo/chat")
    suspend fun wilfredoChat(@Body request: WilfredoChatRequest): Response<ApiResponse<WilfredoChatInfo>>

    @POST("/wilfredo/analyze")
    suspend fun wilfredoAnalyze(@Body request: WilfredoAnalyzeRequest): Response<ApiResponse<WilfredoAnalyzeInfo>>

    @POST("/wilfredo/plan")
    suspend fun wilfredoPlan(@Body request: WilfredoPlanRequest): Response<ApiResponse<WilfredoPlanInfo>>

    @POST("/wilfredo/tuner")
    suspend fun wilfredoTuner(@Body request: WilfredoTunerRequest): Response<ApiResponse<WilfredoTunerInfo>>

    @POST("/wilfredo/chord")
    suspend fun wilfredoChord(@Body request: WilfredoChordRequest): Response<ApiResponse<WilfredoChordInfo>>

    // ==========================================
    // PRACTICA ENDPOINTS
    // ==========================================

    @Multipart
    @POST("/practica/analyze")
    suspend fun practicaAnalyze(@Part audio: MultipartBody.Part): Response<PracticaAnalyzeInfo>

    @Multipart
    @POST("/practica")
    suspend fun practica(
        @Query("user_id") userId: String,
        @Part file: MultipartBody.Part,
        @Query("duracion_seg") duracionSeg: Int = 0,
        @Query("ejercicio") ejercicio: String = "practica_general"
    ): Response<PracticaResult>

    // ==========================================
    // PROGRESO ENDPOINTS
    // ==========================================

    @GET("/progreso/{userId}")
    suspend fun progreso(@Path("userId") userId: String): Response<ProgresoDto>
}
