package com.example.prueba.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.prueba.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel del onboarding de usuarios nuevos.
 *
 * Persiste en MongoDB (vía POST /auth/onboarding/{user_id}):
 * - las respuestas de experiencia y objetivo (que derivan el nivel inicial)
 * - la marca de finalización cuando el usuario completa afinador + 1ª práctica
 */
class OnboardingViewModel : ViewModel() {
    private val authRepository = AuthRepository

    private val _saveState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val saveState: StateFlow<UiState<Unit>> = _saveState.asStateFlow()

    private val _completeState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val completeState: StateFlow<UiState<Unit>> = _completeState.asStateFlow()

    /** Guarda experiencia y objetivo; deriva el nivel inicial en el backend. */
    fun saveAnswers(experiencia: String, objetivo: String, onSaved: () -> Unit) {
        viewModelScope.launch {
            _saveState.value = UiState.Loading
            val session = authRepository.restoreSession().getOrNull()
            if (session == null) {
                _saveState.value = UiState.Error("Sesión no válida. Vuelve a iniciar sesión.")
                return@launch
            }
            authRepository.saveOnboarding(session.id, experiencia = experiencia, objetivo = objetivo)
                .onSuccess {
                    _saveState.value = UiState.Success(Unit)
                    onSaved()
                }
                .onFailure { e ->
                    _saveState.value = UiState.Error(e.message ?: "No se pudieron guardar tus respuestas.")
                }
        }
    }

    /** Marca el onboarding como completado (tras afinador + primera práctica). */
    fun completeOnboarding(onCompleted: () -> Unit) {
        viewModelScope.launch {
            _completeState.value = UiState.Loading
            val session = authRepository.restoreSession().getOrNull()
            if (session == null) {
                _completeState.value = UiState.Error("Sesión no válida.")
                return@launch
            }
            authRepository.saveOnboarding(session.id, completado = true)
                .onSuccess {
                    _completeState.value = UiState.Success(Unit)
                    onCompleted()
                }
                .onFailure { e ->
                    _completeState.value = UiState.Error(e.message ?: "No se pudo completar el onboarding.")
                }
        }
    }

    fun resetSaveError() {
        if (_saveState.value is UiState.Error) _saveState.value = UiState.Idle
    }
}
