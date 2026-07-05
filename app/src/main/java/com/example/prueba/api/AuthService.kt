package com.example.prueba.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/** Web Client ID de Google (público). Usado por Credential Manager. */
const val GOOGLE_WEB_CLIENT_ID =
    "715955170207-lrnnkauikqnb8la60pci3jh64vs6gn6c.apps.googleusercontent.com"

interface AuthService {

    @POST("/auth/google")
    suspend fun loginGoogle(@Body req: GoogleLoginRequest): Response<UserProfileDto>

    @GET("/auth/perfil/{userId}")
    suspend fun getPerfil(@Path("userId") userId: String): Response<UserProfileDto>

    @POST("/auth/onboarding/{userId}")
    suspend fun saveOnboarding(
        @Path("userId") userId: String,
        @Body req: OnboardingRequest
    ): Response<UserProfileDto>
}
