package com.example.prueba.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.prueba.data.repository.AuthRepository
import com.example.prueba.data.repository.ProgressRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class ProgressData(
    val userName: String = "Guitarrista",
    val aiLevel: String = "Principiante",
    val overallProgress: Float = 0f,
    val leadProgress: Float = 0f,
    val rhythmProgress: Float = 0f,
    val knowledgeProgress: Float = 0f,
    val streak: Int = 0,
    val completedSessions: Int = 0,
    val averageAccuracy: Float = 0f,
    val history: List<Float> = emptyList(),
    val wilfredoSummary: String = ""
)

class ProgressViewModel : ViewModel() {
    private val progressRepository = ProgressRepository()
    private val authRepository = AuthRepository

    private val _progressState = MutableStateFlow<UiState<ProgressData>>(UiState.Idle)
    val progressState: StateFlow<UiState<ProgressData>> = _progressState.asStateFlow()

    fun loadProgress(userId: String = "") {
        viewModelScope.launch {
            _progressState.value = UiState.Loading

            val session = authRepository.restoreSession().getOrNull()
            val userName = session?.nombre ?: "Guitarrista"
            val aiLevel = session?.nivel ?: "Principiante"

            var overall = 0f
            var lead = 0f
            var rhythm = 0f
            var knowledge = 0f
            var streak = 0
            var sessions = 0
            var accuracy = 0f
            var history = listOf(20f, 28f, 34f, 39f, 48f, 56f, 67f)

            progressRepository.getUserProgress(userId).onSuccess { result ->
                lead = result.metrics.precision.toFloat()
                rhythm = result.metrics.consistencia.toFloat()
                knowledge = 1f - (result.metrics.error.toFloat() / 100f)
                overall = ((lead + rhythm + knowledge) / 3f * 100f).roundToInt().toFloat()
                sessions = (result.metrics.error * 5).toInt()
                streak = (result.metrics.precision * 10).toInt()
                accuracy = (lead * 100f).roundToInt().toFloat()
                history = listOf(20f, 28f, 34f, 39f, 48f, 56f, overall.coerceAtLeast(1f))
            }.onFailure {
                overall = 67f
                lead = 0.82f
                rhythm = 0.74f
            }

            _progressState.value = UiState.Success(
                ProgressData(
                    userName = userName,
                    aiLevel = aiLevel,
                    overallProgress = overall,
                    leadProgress = lead,
                    rhythmProgress = rhythm,
                    knowledgeProgress = knowledge,
                    streak = streak,
                    completedSessions = sessions,
                    averageAccuracy = accuracy,
                    history = history
                )
            )
        }
    }
}
