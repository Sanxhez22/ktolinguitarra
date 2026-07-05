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

/** Nombres legibles de los ejercicios registrados desde la pantalla de práctica. */
private val NOMBRES_EJERCICIO = mapOf(
    "escalas" to "Escalas",
    "acordes" to "Acordes",
    "fingerpicking" to "Fingerpicking",
    "rasgueo" to "Rasgueo",
    "ritmo" to "Ritmo",
    "calentamiento" to "Calentamiento",
    "practica_general" to "Práctica general"
)

fun nombreEjercicio(id: String?): String? = id?.let { NOMBRES_EJERCICIO[it] ?: it }

data class HomeData(
    val userName: String = "Guitarrista",
    val aiLevel: String = "principiante",
    val streak: Int = 0,
    val accuracy: Int = 0,                  // % promedio real
    val completedSessions: Int = 0,
    val minutesToday: Float = 0f,
    val lastExercise: String? = null,       // nombre legible de la última práctica
    val nextExercise: String? = null,       // recomendación del plan de Wilfredo
    val routineExercises: List<String> = emptyList(),
    val routineDuration: String = "",
    val routineAdvice: String = "",
    val wilfredoTip: String = "",
    val isTipLoading: Boolean = false
)

class HomeViewModel : ViewModel() {
    private val authRepository = AuthRepository
    private val chatRepository = ChatRepository()
    private val progressRepository = ProgressRepository()

    private val _homeState = MutableStateFlow<UiState<HomeData>>(UiState.Idle)
    val homeState: StateFlow<UiState<HomeData>> = _homeState.asStateFlow()

    fun loadHomeData() {
        viewModelScope.launch {
            _homeState.value = UiState.Loading

            val session = authRepository.restoreSession().getOrNull()
            if (session == null) {
                _homeState.value = UiState.Error("Inicia sesión para ver tu inicio.")
                return@launch
            }

            var data = HomeData(userName = session.nombre, aiLevel = session.nivel)

            // Progreso real agregado en MongoDB. Si falla, el Home se muestra
            // igualmente con los datos de la sesión local (todo en cero).
            progressRepository.getUserProgress(session.id).onSuccess { p ->
                data = data.copy(
                    aiLevel = p.nivel,
                    streak = p.rachaDias,
                    accuracy = (p.precisionPromedio * 100).toInt(),
                    completedSessions = p.sesiones,
                    minutesToday = p.minutosHoy.toFloat(),
                    lastExercise = nombreEjercicio(p.historial.lastOrNull()?.ejercicio)
                )
            }

            _homeState.value = UiState.Success(data)

            loadRoutine(data.aiLevel)
            loadWilfredoTip(data.aiLevel)
        }
    }

    /** Plan de práctica de Wilfredo: rutina y siguiente ejercicio recomendado. */
    private fun loadRoutine(level: String) {
        viewModelScope.launch {
            chatRepository.getPracticePlan(level = level.lowercase()).onSuccess { plan ->
                val current = (_homeState.value as? UiState.Success)?.data ?: return@onSuccess
                _homeState.value = UiState.Success(
                    current.copy(
                        nextExercise = plan.ejercicios.firstOrNull(),
                        routineExercises = plan.ejercicios,
                        routineDuration = plan.duracion,
                        routineAdvice = plan.consejo
                    )
                )
            }
        }
    }

    private fun loadWilfredoTip(level: String) {
        viewModelScope.launch {
            val current = _homeState.value
            if (current is UiState.Success) {
                _homeState.value = UiState.Success(current.data.copy(isTipLoading = true))

                chatRepository.sendMessage(
                    message = "Dame un consejo rápido para hoy",
                    level = level.lowercase()
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
