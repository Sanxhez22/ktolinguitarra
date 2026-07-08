package com.example.prueba.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.prueba.api.BibliotecaItemDto
import com.example.prueba.api.CancionDetalleDto
import com.example.prueba.api.PlanCancionDto
import com.example.prueba.data.repository.AuthRepository
import com.example.prueba.data.repository.SongRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SongDetailViewModel : ViewModel() {
    private val songRepository = SongRepository()
    private val authRepository = AuthRepository

    private val _detailState = MutableStateFlow<UiState<CancionDetalleDto>>(UiState.Idle)
    val detailState: StateFlow<UiState<CancionDetalleDto>> = _detailState.asStateFlow()

    // Estado de los botones Guardar/Favorito (biblioteca del usuario).
    private val _bibliotecaState = MutableStateFlow<BibliotecaItemDto?>(null)
    val bibliotecaState: StateFlow<BibliotecaItemDto?> = _bibliotecaState.asStateFlow()

    // Resultado del botón Practicar: plan de Wilfredo listo para navegar.
    private val _practicarState = MutableStateFlow<UiState<PlanCancionDto>>(UiState.Idle)
    val practicarState: StateFlow<UiState<PlanCancionDto>> = _practicarState.asStateFlow()

    fun loadSong(songId: Long) {
        viewModelScope.launch {
            _detailState.value = UiState.Loading
            songRepository.detalle(songId)
                .onSuccess { _detailState.value = UiState.Success(it) }
                .onFailure { e -> _detailState.value = UiState.Error(e.message ?: "Error al cargar la canción") }

            // Estado de biblioteca (si hay sesión); si falla, botones en neutro.
            val session = authRepository.restoreSession().getOrNull() ?: return@launch
            songRepository.estadoBiblioteca(session.id, songId)
                .onSuccess { _bibliotecaState.value = it }
        }
    }

    fun toggleGuardar(songId: Long) {
        viewModelScope.launch {
            val session = authRepository.restoreSession().getOrNull() ?: return@launch
            val actual = _bibliotecaState.value?.guardada ?: false
            songRepository.actualizarBiblioteca(session.id, songId, guardada = !actual)
                .onSuccess { _bibliotecaState.value = it }
        }
    }

    fun toggleFavorito(songId: Long) {
        viewModelScope.launch {
            val session = authRepository.restoreSession().getOrNull() ?: return@launch
            val actual = _bibliotecaState.value?.favorita ?: false
            songRepository.actualizarBiblioteca(session.id, songId, favorita = !actual)
                .onSuccess { _bibliotecaState.value = it }
        }
    }

    /**
     * Botón Practicar: registra la canción en la biblioteca (backend) y
     * obtiene el plan de Wilfredo. La pantalla observa practicarState y
     * navega al flujo de práctica con el ejercicio del plan.
     */
    fun practicar(songId: Long) {
        viewModelScope.launch {
            _practicarState.value = UiState.Loading
            val session = authRepository.restoreSession().getOrNull()
            if (session == null) {
                _practicarState.value = UiState.Error("Inicia sesión para practicar.")
                return@launch
            }
            songRepository.practicar(songId, session.id)
                .onSuccess {
                    _practicarState.value = UiState.Success(it)
                    // Practicar también guarda la canción: refresca los botones.
                    songRepository.estadoBiblioteca(session.id, songId)
                        .onSuccess { e -> _bibliotecaState.value = e }
                }
                .onFailure { e -> _practicarState.value = UiState.Error(e.message ?: "No se pudo iniciar la práctica") }
        }
    }

    fun resetPracticar() {
        _practicarState.value = UiState.Idle
    }
}
