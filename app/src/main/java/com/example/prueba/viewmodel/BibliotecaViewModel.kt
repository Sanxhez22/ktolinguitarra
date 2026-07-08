package com.example.prueba.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.prueba.api.BibliotecaItemDto
import com.example.prueba.data.repository.AuthRepository
import com.example.prueba.data.repository.SongRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Biblioteca del usuario (canciones guardadas y favoritas), leída de
 * MongoDB vía GET /biblioteca. Sin datos simulados: usuario nuevo ve vacío.
 */
class BibliotecaViewModel : ViewModel() {
    private val songRepository = SongRepository()
    private val authRepository = AuthRepository

    private val _state = MutableStateFlow<UiState<List<BibliotecaItemDto>>>(UiState.Idle)
    val state: StateFlow<UiState<List<BibliotecaItemDto>>> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            val session = authRepository.restoreSession().getOrNull()
            if (session == null) {
                _state.value = UiState.Error("Inicia sesión para ver tu biblioteca.")
                return@launch
            }
            songRepository.biblioteca(session.id)
                .onSuccess { _state.value = UiState.Success(it.items) }
                .onFailure { e -> _state.value = UiState.Error(e.message ?: "No se pudo cargar tu biblioteca.") }
        }
    }

    /** Alterna favorito y refresca la lista local sin recargar todo. */
    fun toggleFavorito(item: BibliotecaItemDto) {
        viewModelScope.launch {
            val session = authRepository.restoreSession().getOrNull() ?: return@launch
            songRepository.actualizarBiblioteca(session.id, item.songId, favorita = !item.favorita)
                .onSuccess { actualizado ->
                    val actual = (_state.value as? UiState.Success)?.data ?: return@onSuccess
                    // Si quedó sin flags, el backend la elimina de la biblioteca.
                    val nueva = if (!actualizado.guardada && !actualizado.favorita) {
                        actual.filterNot { it.songId == item.songId }
                    } else {
                        actual.map { if (it.songId == item.songId) actualizado else it }
                    }
                    _state.value = UiState.Success(nueva)
                }
        }
    }

    /** Quita la canción de la biblioteca por completo. */
    fun quitar(item: BibliotecaItemDto) {
        viewModelScope.launch {
            val session = authRepository.restoreSession().getOrNull() ?: return@launch
            songRepository.actualizarBiblioteca(
                session.id, item.songId, guardada = false, favorita = false
            ).onSuccess {
                val actual = (_state.value as? UiState.Success)?.data ?: return@onSuccess
                _state.value = UiState.Success(actual.filterNot { it.songId == item.songId })
            }
        }
    }
}
