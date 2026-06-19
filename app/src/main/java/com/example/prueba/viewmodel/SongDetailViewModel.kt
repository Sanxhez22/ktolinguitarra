package com.example.prueba.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.prueba.api.SongsterrMeta
import com.example.prueba.data.repository.SearchRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SongDetailViewModel : ViewModel() {
    private val searchRepository = SearchRepository()

    private val _detailState = MutableStateFlow<UiState<SongsterrMeta>>(UiState.Idle)
    val detailState: StateFlow<UiState<SongsterrMeta>> = _detailState.asStateFlow()

    fun loadSong(songId: Long) {
        viewModelScope.launch {
            _detailState.value = UiState.Loading
            searchRepository.getSongMeta(songId)
                .onSuccess { meta -> _detailState.value = UiState.Success(meta) }
                .onFailure { e -> _detailState.value = UiState.Error(e.message ?: "Error al cargar la canción") }
        }
    }
}
