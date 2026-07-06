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
        @Query("ejercicio") ejercicio: String = "practica_general",
        @Query("cancion_id") cancionId: Long? = null
    ): Response<PracticaResult>

    // ==========================================
    // PROGRESO ENDPOINTS
    // ==========================================

    @GET("/progreso/{userId}")
    suspend fun progreso(@Path("userId") userId: String): Response<ProgresoDto>

    // ==========================================
    // P1 - ENTRENADOR / HABILIDADES / CAMINO / EJERCICIOS
    // ==========================================

    @GET("/entrenador/{userId}")
    suspend fun entrenador(
        @Path("userId") userId: String,
        @Query("tz_offset_min") tzOffsetMin: Int = 0
    ): Response<EntrenadorResponse>

    @GET("/habilidades/{userId}")
    suspend fun habilidades(@Path("userId") userId: String): Response<HabilidadesResponse>

    @GET("/camino/{userId}")
    suspend fun camino(@Path("userId") userId: String): Response<CaminoResponse>

    @GET("/ejercicios")
    suspend fun ejercicios(@Query("habilidad") habilidad: String? = null): Response<EjerciciosResponse>

    @GET("/ejercicios/{id}")
    suspend fun ejercicio(@Path("id") id: String): Response<EjercicioDto>

    // ==========================================
    // CANCIONES / BIBLIOTECA (Song Detail)
    // ==========================================

    @GET("/canciones/buscar")
    suspend fun buscarCanciones(
        @Query("q") q: String,
        @Query("size") size: Int = 10,
        @Query("desde") desde: Int = 0
    ): Response<BusquedaCancionesResponse>

    @GET("/canciones/{songId}")
    suspend fun cancionDetalle(@Path("songId") songId: Long): Response<CancionDetalleDto>

    @GET("/canciones/{songId}/plan")
    suspend fun cancionPlan(
        @Path("songId") songId: Long,
        @Query("user_id") userId: String
    ): Response<PlanCancionDto>

    @POST("/canciones/{songId}/practicar")
    suspend fun cancionPracticar(
        @Path("songId") songId: Long,
        @Query("user_id") userId: String
    ): Response<PlanCancionDto>

    @GET("/biblioteca/{userId}")
    suspend fun biblioteca(
        @Path("userId") userId: String,
        @Query("tipo") tipo: String? = null
    ): Response<BibliotecaResponse>

    @GET("/biblioteca/{userId}/{songId}")
    suspend fun bibliotecaEstado(
        @Path("userId") userId: String,
        @Path("songId") songId: Long
    ): Response<BibliotecaItemDto>

    @POST("/biblioteca/{userId}/{songId}")
    suspend fun bibliotecaActualizar(
        @Path("userId") userId: String,
        @Path("songId") songId: Long,
        @Body cambios: BibliotecaUpdateRequest
    ): Response<BibliotecaItemDto>
}

data class EjerciciosResponse(
    val ejercicios: List<EjercicioDto> = emptyList(),
    val total: Int = 0
)
