package com.example.prueba.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.prueba.api.WilfredoAnalyzeInfo
import com.example.prueba.api.WilfredoChatInfo
import com.example.prueba.data.repository.AuthRepository
import com.example.prueba.data.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {
    private val chatRepository = ChatRepository()

    private val _chatState = MutableStateFlow<UiState<WilfredoChatInfo>>(UiState.Idle)
    val chatState: StateFlow<UiState<WilfredoChatInfo>> = _chatState.asStateFlow()

    private val _analyzeState = MutableStateFlow<UiState<WilfredoAnalyzeInfo>>(UiState.Idle)
    val analyzeState: StateFlow<UiState<WilfredoAnalyzeInfo>> = _analyzeState.asStateFlow()

    fun sendMessage(
        message: String,
        level: String = "principiante",
        history: List<String> = emptyList()
    ) {
        viewModelScope.launch {
            _chatState.value = UiState.Loading
            // Con sesión activa, Wilfredo responde como tutor con memoria.
            val session = AuthRepository.restoreSession().getOrNull()
            chatRepository.sendMessage(
                message,
                session?.nivel?.lowercase() ?: level,
                history,
                userId = session?.id
            )
                .onSuccess { info -> _chatState.value = UiState.Success(info) }
                .onFailure { e -> _chatState.value = UiState.Error(e.message ?: "Error al enviar mensaje") }
        }
    }

    fun analyzeMetrics(
        precision: Float,
        rhythm: Float = 0f,
        bpm: Int = 120,
        note: String? = null
    ) {
        viewModelScope.launch {
            _analyzeState.value = UiState.Loading
            chatRepository.analyzeMetrics(precision, rhythm, bpm, note)
                .onSuccess { info -> _analyzeState.value = UiState.Success(info) }
                .onFailure { e -> _analyzeState.value = UiState.Error(e.message ?: "Error al analizar métricas") }
        }
    }

    fun resetChatState() {
        _chatState.value = UiState.Idle
    }
}
