package com.example.prueba.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.prueba.data.repository.AuthRepository
import com.example.prueba.data.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeData(
    val userName: String = "Guitarrista",
    val aiLevel: String = "Principiante",
    val streak: Int = 0,
    val accuracy: Int = 0,
    val completedSessions: Int = 0,
    val wilfredoTip: String = "",
    val isTipLoading: Boolean = false
)

class HomeViewModel : ViewModel() {
    private val authRepository = AuthRepository
    private val chatRepository = ChatRepository()

    private val _homeState = MutableStateFlow<UiState<HomeData>>(UiState.Idle)
    val homeState: StateFlow<UiState<HomeData>> = _homeState.asStateFlow()

    fun loadHomeData() {
        viewModelScope.launch {
            _homeState.value = UiState.Loading

            val session = authRepository.restoreSession().getOrNull()
            var userName = session?.nombre ?: "Guitarrista"
            var aiLevel = session?.nivel ?: "principiante"
            var accuracy = 0
            var completedSessions = 0

            // Datos reales del perfil en MongoDB (vía /auth/perfil/{id}).
            if (session != null) {
                authRepository.getProfile(session.id).onSuccess { perfil ->
                    userName = perfil.nombre ?: userName
                    aiLevel = perfil.nivel
                    accuracy = ((perfil.estadisticas?.precisionPromedio ?: 0.0) * 100).toInt()
                    completedSessions = perfil.estadisticas?.sesiones ?: 0
                }
            }

            _homeState.value = UiState.Success(
                HomeData(
                    userName = userName,
                    aiLevel = aiLevel,
                    streak = 0, // No hay tracking de racha en backend aún.
                    accuracy = accuracy,
                    completedSessions = completedSessions
                )
            )

            loadWilfredoTip()
        }
    }

    private fun loadWilfredoTip() {
        viewModelScope.launch {
            val current = _homeState.value
            if (current is UiState.Success) {
                _homeState.value = UiState.Success(current.data.copy(isTipLoading = true))

                chatRepository.sendMessage(
                    message = "Dame un consejo rápido para hoy",
                    level = current.data.aiLevel.lowercase()
                ).onSuccess { info ->
                    val updated = (_homeState.value as? UiState.Success)?.data
                    if (updated != null) {
                        _homeState.value = UiState.Success(
                            updated.copy(wilfredoTip = info.respuesta, isTipLoading = false)
                        )
                    }
                }.onFailure {
                    val updated = (_homeState.value as? UiState.Success)?.data
                    if (updated != null) {
                        _homeState.value = UiState.Success(updated.copy(isTipLoading = false))
                    }
                }
            }
        }
    }
}
