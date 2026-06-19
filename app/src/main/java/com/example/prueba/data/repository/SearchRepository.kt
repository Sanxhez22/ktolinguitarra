package com.example.prueba.data.repository

import com.example.prueba.api.ApiClient
import com.example.prueba.api.SongsterrMeta
import com.example.prueba.api.SongsterrSong

class SearchRepository {
    private val api = ApiClient.songsterrService

    suspend fun searchSongs(query: String): Result<List<SongsterrSong>> = runCatching {
        val response = api.searchSongs(query)
        val body = response.body()
        if (response.isSuccessful && body != null) {
            body
        } else {
            throw Exception("Error al buscar canciones")
        }
    }

    suspend fun getSongMeta(songId: Long): Result<SongsterrMeta> = runCatching {
        val response = api.getSongMeta(songId)
        val body = response.body()
        if (response.isSuccessful && body != null) {
            body
        } else {
            throw Exception("Error al cargar la canción")
        }
    }
}
