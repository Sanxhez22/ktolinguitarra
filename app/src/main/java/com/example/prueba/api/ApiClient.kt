package com.example.prueba.api

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object  ApiClient {
    const val BASE_URL_FASTAPI = "https://tesisguitar-production.up.railway.app/"
    const val BASE_URL_SONGSTERR = "https://www.songsterr.com/"

    private val gson = GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .create()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    private val fastApiRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL_FASTAPI)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    private val songsterrRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL_SONGSTERR)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    val fastApiService: FastApiService by lazy {
        fastApiRetrofit.create(FastApiService::class.java)
    }

    val songsterrService: SongsterrService by lazy {
        songsterrRetrofit.create(SongsterrService::class.java)
    }
}
