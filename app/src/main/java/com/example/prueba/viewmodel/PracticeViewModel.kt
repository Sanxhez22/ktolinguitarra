package com.example.prueba.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.prueba.api.PracticaAnalyzeInfo
import com.example.prueba.api.PracticaResult
import com.example.prueba.api.WilfredoAnalyzeInfo
import com.example.prueba.data.repository.AuthRepository
import com.example.prueba.data.repository.ChatRepository
import com.example.prueba.data.repository.PracticeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class PracticeViewModel : ViewModel() {
    private val practiceRepository = PracticeRepository()
    private val chatRepository = ChatRepository()
    private val authRepository = AuthRepository

    private val _audioState = MutableStateFlow<UiState<PracticaAnalyzeInfo>>(UiState.Idle)
    val audioState: StateFlow<UiState<PracticaAnalyzeInfo>> = _audioState.asStateFlow()

    private val _practiceState = MutableStateFlow<UiState<PracticaResult>>(UiState.Idle)
    val practiceState: StateFlow<UiState<PracticaResult>> = _practiceState.asStateFlow()

    private val _feedbackState = MutableStateFlow<UiState<WilfredoAnalyzeInfo>>(UiState.Idle)
    val feedbackState: StateFlow<UiState<WilfredoAnalyzeInfo>> = _feedbackState.asStateFlow()

    // Última sesión pendiente, para poder reintentar si falla la subida.
    private var lastFile: File? = null
    private var lastDuracionSeg: Int = 0
    private var lastEjercicio: String = "practica_general"

    fun analyzeAudio(file: File) {
        viewModelScope.launch {
            _audioState.value = UiState.Loading
            practiceRepository.analyzeAudio(file)
                .onSuccess { info -> _audioState.value = UiState.Success(info) }
                .onFailure { e -> _audioState.value = UiState.Error(e.message ?: "Error al analizar audio") }
        }
    }

    /**
     * Registra la sesión real en el backend: sube el WAV grabado a
     * POST /practica (que calcula precisión/consistencia y persiste en
     * MongoDB) y luego pide a Wilfredo el feedback pedagógico con esas
     * métricas reales.
     */
    fun submitSession(file: File?, duracionSeg: Int, ejercicio: String) {
        if (file == null) {
            _practiceState.value = UiState.Error(
                "No se capturó audio de la sesión. Verifica el permiso de micrófono."
            )
            return
        }
        lastFile = file
        lastDuracionSeg = duracionSeg
        lastEjercicio = ejercicio
        upload()
    }

    /** Reintenta la subida de la última sesión grabada. */
    fun retrySubmit() {
        if (lastFile != null) upload()
    }

    private fun upload() {
        val file = lastFile ?: return
        viewModelScope.launch {
            _practiceState.value = UiState.Loading
            _feedbackState.value = UiState.Idle

            val session = authRepository.restoreSession().getOrNull()
            if (session == null) {
                _practiceState.value = UiState.Error("Inicia sesión para guardar tu práctica.")
                return@launch
            }

            practiceRepository.submitPractice(session.id, file, lastDuracionSeg, lastEjercicio)
                .onSuccess { result ->
                    _practiceState.value = UiState.Success(result)
                    file.delete()
                    lastFile = null
                    loadFeedback(result)
                }
                .onFailure { e ->
                    _practiceState.value = UiState.Error(e.message ?: "Error al enviar práctica")
                }
        }
    }

    private fun loadFeedback(result: PracticaResult) {
        viewModelScope.launch {
            _feedbackState.value = UiState.Loading
            chatRepository.analyzeMetrics(
                precision = result.metrics.precision.toFloat().coerceIn(0f, 1f),
                rhythm = result.metrics.consistencia.toFloat().coerceIn(0f, 1f)
            )
                .onSuccess { info -> _feedbackState.value = UiState.Success(info) }
                .onFailure { e -> _feedbackState.value = UiState.Error(e.message ?: "Error al obtener feedback") }
        }
    }

    fun resetPractice() {
        _practiceState.value = UiState.Idle
        _feedbackState.value = UiState.Idle
        lastFile?.delete()
        lastFile = null
    }
}
