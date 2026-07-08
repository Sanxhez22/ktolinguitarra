package com.example.prueba.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.prueba.api.CaminoResponse
import com.example.prueba.data.repository.AuthRepository
import com.example.prueba.data.repository.TrainerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel del camino de aprendizaje (P1).
 * Alimenta la pantalla Camino (ruta estilo Duolingo).
 */
class CaminoViewModel : ViewModel() {
    private val trainerRepository = TrainerRepository()
    private val authRepository = AuthRepository

    private val _caminoState = MutableStateFlow<UiState<CaminoResponse>>(UiState.Idle)
    val caminoState: StateFlow<UiState<CaminoResponse>> = _caminoState.asStateFlow()

    fun loadCamino() {
        viewModelScope.launch {
            _caminoState.value = UiState.Loading
            val session = authRepository.restoreSession().getOrNull()
            if (session == null) {
                _caminoState.value = UiState.Error("Inicia sesión para ver tu camino.")
                return@launch
            }
            trainerRepository.getCamino(session.id)
                .onSuccess { _caminoState.value = UiState.Success(it) }
                .onFailure { e -> _caminoState.value = UiState.Error(e.message ?: "Error al cargar el camino.") }
        }
    }
}
