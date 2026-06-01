package com.example.prueba.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.prueba.api.SongsterrSong
import com.example.prueba.data.repository.SearchRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SearchViewModel : ViewModel() {
    private val searchRepository = SearchRepository()

    private val _searchState = MutableStateFlow<UiState<List<SongsterrSong>>>(UiState.Idle)
    val searchState: StateFlow<UiState<List<SongsterrSong>>> = _searchState.asStateFlow()

    private var searchJob: Job? = null

    fun searchSongs(query: String) {
        searchJob?.cancel()

        if (query.isBlank()) {
            _searchState.value = UiState.Idle
            return
        }

        searchJob = viewModelScope.launch {
            delay(300)
            _searchState.value = UiState.Loading
            searchRepository.searchSongs(query)
                .onSuccess { songs -> _searchState.value = UiState.Success(songs) }
                .onFailure { e -> _searchState.value = UiState.Error(e.message ?: "Error al buscar canciones") }
        }
    }

    fun resetSearch() {
        searchJob?.cancel()
        _searchState.value = UiState.Idle
    }
}
