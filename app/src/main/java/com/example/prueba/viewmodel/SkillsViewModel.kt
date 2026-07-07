package com.example.prueba.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.prueba.api.HabilidadesResponse
import com.example.prueba.data.repository.AuthRepository
import com.example.prueba.data.repository.TrainerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel de las 12 habilidades del usuario (P1).
 * Alimenta la sección de habilidades en Progreso.
 */
class SkillsViewModel : ViewModel() {
    private val trainerRepository = TrainerRepository()
    private val authRepository = AuthRepository

    private val _skillsState = MutableStateFlow<UiState<HabilidadesResponse>>(UiState.Idle)
    val skillsState: StateFlow<UiState<HabilidadesResponse>> = _skillsState.asStateFlow()

    // Hitos del Motor Cognitivo (logros, evolución, estancamiento, regresos).
    private val _hitosState = MutableStateFlow<List<com.example.prueba.api.HitoDto>>(emptyList())
    val hitosState: StateFlow<List<com.example.prueba.api.HitoDto>> = _hitosState.asStateFlow()

    fun loadSkills() {
        viewModelScope.launch {
            _skillsState.value = UiState.Loading
            val session = authRepository.restoreSession().getOrNull()
            if (session == null) {
                _skillsState.value = UiState.Error("Inicia sesión para ver tus habilidades.")
                return@launch
            }
            trainerRepository.getHabilidades(session.id)
                .onSuccess { _skillsState.value = UiState.Success(it) }
                .onFailure { e -> _skillsState.value = UiState.Error(e.message ?: "Error al cargar habilidades.") }
            // Best-effort: la pantalla funciona sin hitos.
            trainerRepository.getHitos(session.id)
                .onSuccess { _hitosState.value = it.hitos }
        }
    }
}
