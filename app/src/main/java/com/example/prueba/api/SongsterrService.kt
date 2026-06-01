package com.example.prueba.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface SongsterrService {

    @GET("/api/songs")
    suspend fun searchSongs(@Query("pattern") query: String): Response<List<SongsterrSong>>
}
