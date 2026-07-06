package com.example.prueba.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.prueba.api.CancionResumenDto
import com.example.prueba.data.repository.SongRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Resultados acumulados de búsqueda con paginación (backend /canciones/buscar). */
data class SearchData(
    val canciones: List<CancionResumenDto> = emptyList(),
    val hayMas: Boolean = false,
    val cargandoMas: Boolean = false
)

class SearchViewModel : ViewModel() {
    private val songRepository = SongRepository()

    private val _searchState = MutableStateFlow<UiState<SearchData>>(UiState.Idle)
    val searchState: StateFlow<UiState<SearchData>> = _searchState.asStateFlow()

    private var searchJob: Job? = null
    private var queryActual: String = ""
    private val pageSize = 15

    fun searchSongs(query: String) {
        searchJob?.cancel()

        if (query.isBlank()) {
            _searchState.value = UiState.Idle
            return
        }

        searchJob = viewModelScope.launch {
            delay(300)
            queryActual = query
            _searchState.value = UiState.Loading
            songRepository.buscar(query, size = pageSize, desde = 0)
                .onSuccess { r ->
                    _searchState.value = UiState.Success(
                        SearchData(canciones = r.canciones, hayMas = r.hayMas)
                    )
                }
                .onFailure { e ->
                    _searchState.value = UiState.Error(e.message ?: "Error al buscar canciones")
                }
        }
    }

    /** Carga la siguiente página y la anexa a los resultados actuales. */
    fun cargarMas() {
        val actual = (_searchState.value as? UiState.Success)?.data ?: return
        if (!actual.hayMas || actual.cargandoMas) return

        viewModelScope.launch {
            _searchState.value = UiState.Success(actual.copy(cargandoMas = true))
            songRepository.buscar(queryActual, size = pageSize, desde = actual.canciones.size)
                .onSuccess { r ->
                    _searchState.value = UiState.Success(
                        SearchData(
                            canciones = actual.canciones + r.canciones,
                            hayMas = r.hayMas
                        )
                    )
                }
                .onFailure {
                    // Falla silenciosa del "cargar más": se mantienen los resultados.
                    _searchState.value = UiState.Success(actual.copy(cargandoMas = false))
                }
        }
    }

    fun resetSearch() {
        searchJob?.cancel()
        _searchState.value = UiState.Idle
    }
}
