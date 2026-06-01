package com.example.prueba.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.prueba.data.repository.AuthRepository
import com.example.prueba.data.repository.ChatRepository
import com.example.prueba.data.repository.ProgressRepository
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
    private val authRepository = AuthRepository()
    private val progressRepository = ProgressRepository()
    private val chatRepository = ChatRepository()

    private val _homeState = MutableStateFlow<UiState<HomeData>>(UiState.Idle)
    val homeState: StateFlow<UiState<HomeData>> = _homeState.asStateFlow()

    fun loadHomeData(userId: String = "") {
        viewModelScope.launch {
            _homeState.value = UiState.Loading

            val session = authRepository.getCurrentSession()
            val userName = session?.nombre ?: "Guitarrista"
            val aiLevel = session?.nivel ?: "Principiante"

            var streak = 0
            var accuracy = 0
            var completedSessions = 0

            progressRepository.getUserProgress(userId).onSuccess { result ->
                streak = (result.metrics.precision * 10).toInt()
                accuracy = (result.metrics.consistencia * 100).toInt()
                completedSessions = (result.metrics.error * 5).toInt()
            }

            _homeState.value = UiState.Success(
                HomeData(
                    userName = userName,
                    aiLevel = aiLevel,
                    streak = streak,
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
