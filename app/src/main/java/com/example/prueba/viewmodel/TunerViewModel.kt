package com.example.prueba.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.prueba.api.AnalyzeInfo
import com.example.prueba.api.WilfredoTunerInfo
import com.example.prueba.data.repository.ChatRepository
import com.example.prueba.data.repository.TunerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TunerViewModel : ViewModel() {
    private val tunerRepository = TunerRepository()
    private val chatRepository = ChatRepository()

    private val _analysisState = MutableStateFlow<UiState<AnalyzeInfo>>(UiState.Idle)
    val analysisState: StateFlow<UiState<AnalyzeInfo>> = _analysisState.asStateFlow()

    private val _feedbackState = MutableStateFlow<UiState<WilfredoTunerInfo>>(UiState.Idle)
    val feedbackState: StateFlow<UiState<WilfredoTunerInfo>> = _feedbackState.asStateFlow()

    fun analyzeFrequency(frequency: Float) {
        viewModelScope.launch {
            _analysisState.value = UiState.Loading
            tunerRepository.analyzeFrequency(frequency)
                .onSuccess { info -> _analysisState.value = UiState.Success(info) }
                .onFailure { e -> _analysisState.value = UiState.Error(e.message ?: "Error al analizar frecuencia") }
        }
    }

    fun getTunerFeedback(frequency: Float, note: String, cents: Float) {
        viewModelScope.launch {
            _feedbackState.value = UiState.Loading
            chatRepository.getTunerFeedback(frequency, note, cents)
                .onSuccess { info -> _feedbackState.value = UiState.Success(info) }
                .onFailure { e -> _feedbackState.value = UiState.Error(e.message ?: "Error al obtener feedback") }
        }
    }

    fun resetAnalysis() {
        _analysisState.value = UiState.Idle
    }

    fun resetFeedback() {
        _feedbackState.value = UiState.Idle
    }
}
