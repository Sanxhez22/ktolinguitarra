package com.example.prueba.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.prueba.api.EntrenadorResponse
import com.example.prueba.data.repository.AuthRepository
import com.example.prueba.data.repository.TrainerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Estado de la Home-entrenador (P1): el nombre del usuario + el payload
 * completo del entrenador (recomendación, objetivo del día, habilidades,
 * próximo logro, consejo).
 */
data class HomeData(
    val userName: String = "Guitarrista",
    val entrenador: EntrenadorResponse,
    // True mientras el usuario omitió la afinación del onboarding y aún no
    // realiza una afinación real (el backend apaga el flag).
    val afinacionPendiente: Boolean = false
)

class HomeViewModel : ViewModel() {
    private val authRepository = AuthRepository
    private val trainerRepository = TrainerRepository()

    private val _homeState = MutableStateFlow<UiState<HomeData>>(UiState.Idle)
    val homeState: StateFlow<UiState<HomeData>> = _homeState.asStateFlow()

    fun loadHomeData() {
        viewModelScope.launch {
            _homeState.value = UiState.Loading

            // refreshSession sincroniza flags que el backend puede cambiar
            // (p. ej. afinacion_omitida se apaga tras una afinación real).
            val session = authRepository.refreshSession()
            if (session == null) {
                _homeState.value = UiState.Error("Inicia sesión para ver tu entrenador.")
                return@launch
            }

            trainerRepository.getEntrenador(session.id)
                .onSuccess { entrenador ->
                    _homeState.value = UiState.Success(
                        HomeData(
                            userName = session.nombre,
                            entrenador = entrenador,
                            afinacionPendiente = session.afinacionOmitida
                        )
                    )
                }
                .onFailure { e ->
                    _homeState.value = UiState.Error(e.message ?: "No se pudo cargar tu entrenador.")
                }
        }
    }
}
