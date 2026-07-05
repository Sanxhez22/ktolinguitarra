package com.example.prueba.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.prueba.data.repository.AuthRepository
import com.example.prueba.data.repository.ProgressRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProgressData(
    val userName: String = "Guitarrista",
    val aiLevel: String = "principiante",
    val sessions: Int = 0,
    val precisionAvg: Float = 0f,        // 0..1
    val consistencyAvg: Float = 0f,      // 0..1
    val streak: Int = 0,
    val minutesToday: Float = 0f,
    val minutesTotal: Float = 0f,
    val exercisesCompleted: Int = 0,
    val precisionHistory: List<Float> = emptyList(),    // % por sesión
    val consistencyHistory: List<Float> = emptyList(),  // % por sesión
    val wilfredoSummary: String = ""
)

class ProgressViewModel : ViewModel() {
    private val progressRepository = ProgressRepository()
    private val authRepository = AuthRepository

    private val _progressState = MutableStateFlow<UiState<ProgressData>>(UiState.Idle)
    val progressState: StateFlow<UiState<ProgressData>> = _progressState.asStateFlow()

    fun loadProgress() {
        viewModelScope.launch {
            _progressState.value = UiState.Loading

            val session = authRepository.restoreSession().getOrNull()
            if (session == null) {
                _progressState.value = UiState.Error("Inicia sesión para ver tu progreso.")
                return@launch
            }

            progressRepository.getUserProgress(session.id)
                .onSuccess { p ->
                    val data = ProgressData(
                        userName = session.nombre,
                        aiLevel = p.nivel,
                        sessions = p.sesiones,
                        precisionAvg = p.precisionPromedio.toFloat().coerceIn(0f, 1f),
                        consistencyAvg = p.consistenciaPromedio.toFloat().coerceIn(0f, 1f),
                        streak = p.rachaDias,
                        minutesToday = p.minutosHoy.toFloat(),
                        minutesTotal = p.minutosTotales.toFloat(),
                        exercisesCompleted = p.ejerciciosCompletados,
                        precisionHistory = p.historial.map { (it.precision * 100).toFloat() },
                        consistencyHistory = p.historial.map { (it.consistencia * 100).toFloat() },
                        wilfredoSummary = buildSummary(
                            sessions = p.sesiones,
                            precision = p.precisionPromedio,
                            streak = p.rachaDias
                        )
                    )
                    _progressState.value = UiState.Success(data)
                }
                .onFailure { e ->
                    _progressState.value = UiState.Error(e.message ?: "No se pudo cargar tu progreso.")
                }
        }
    }

    /** Resumen pedagógico basado en reglas sobre los datos reales del usuario. */
    private fun buildSummary(sessions: Int, precision: Double, streak: Int): String {
        if (sessions == 0) {
            return "Aún no registras prácticas. Completa tu primera sesión y aquí verás el análisis de Wilfredo sobre tu evolución. 🎸"
        }
        val precisionPct = (precision * 100).toInt()
        val base = when {
            precisionPct >= 85 -> "Tu precisión promedio es de $precisionPct%: excelente afinación y control."
            precisionPct >= 65 -> "Tu precisión promedio es de $precisionPct%: vas por buen camino, sigue puliendo la digitación."
            else -> "Tu precisión promedio es de $precisionPct%: practica despacio y con metrónomo para mejorar."
        }
        val racha = when {
            streak >= 3 -> " Llevas $streak días seguidos practicando, ¡esa constancia es la clave!"
            streak >= 1 -> " Llevas $streak día(s) de racha, no la pierdas hoy."
            else -> " Retoma la práctica hoy para iniciar una nueva racha."
        }
        return base + racha
    }
}
