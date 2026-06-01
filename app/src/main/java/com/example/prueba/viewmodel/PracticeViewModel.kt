package com.example.prueba.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.prueba.api.PracticaAnalyzeInfo
import com.example.prueba.api.PracticaResult
import com.example.prueba.api.WilfredoAnalyzeInfo
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

    private val _audioState = MutableStateFlow<UiState<PracticaAnalyzeInfo>>(UiState.Idle)
    val audioState: StateFlow<UiState<PracticaAnalyzeInfo>> = _audioState.asStateFlow()

    private val _practiceState = MutableStateFlow<UiState<PracticaResult>>(UiState.Idle)
    val practiceState: StateFlow<UiState<PracticaResult>> = _practiceState.asStateFlow()

    private val _feedbackState = MutableStateFlow<UiState<WilfredoAnalyzeInfo>>(UiState.Idle)
    val feedbackState: StateFlow<UiState<WilfredoAnalyzeInfo>> = _feedbackState.asStateFlow()

    fun analyzeAudio(file: File) {
        viewModelScope.launch {
            _audioState.value = UiState.Loading
            practiceRepository.analyzeAudio(file)
                .onSuccess { info -> _audioState.value = UiState.Success(info) }
                .onFailure { e -> _audioState.value = UiState.Error(e.message ?: "Error al analizar audio") }
        }
    }

    fun submitPractice(userId: String, file: File) {
        viewModelScope.launch {
            _practiceState.value = UiState.Loading
            practiceRepository.submitPractice(userId, file)
                .onSuccess { result -> _practiceState.value = UiState.Success(result) }
                .onFailure { e -> _practiceState.value = UiState.Error(e.message ?: "Error al enviar práctica") }
        }
    }

    fun getFeedback(precision: Float, rhythm: Float = 0f, bpm: Int = 120, note: String? = null) {
        viewModelScope.launch {
            _feedbackState.value = UiState.Loading
            chatRepository.analyzeMetrics(precision, rhythm, bpm, note)
                .onSuccess { info -> _feedbackState.value = UiState.Success(info) }
                .onFailure { e -> _feedbackState.value = UiState.Error(e.message ?: "Error al obtener feedback") }
        }
    }

    fun resetPractice() {
        _practiceState.value = UiState.Idle
    }
}
