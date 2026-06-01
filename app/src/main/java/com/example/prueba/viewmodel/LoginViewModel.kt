package com.example.prueba.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.prueba.data.model.UserSession
import com.example.prueba.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {
    private val authRepository = AuthRepository()

    private val _loginState = MutableStateFlow<UiState<UserSession>>(UiState.Idle)
    val loginState: StateFlow<UiState<UserSession>> = _loginState.asStateFlow()

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _loginState.value = UiState.Loading
            authRepository.signInWithGoogle(idToken)
                .onSuccess { session -> _loginState.value = UiState.Success(session) }
                .onFailure { e -> _loginState.value = UiState.Error(e.message ?: "Error al iniciar sesión") }
        }
    }

    fun checkExistingSession() {
        viewModelScope.launch {
            authRepository.restoreSession()
                .onSuccess { session ->
                    if (session != null) {
                        _loginState.value = UiState.Success(session)
                    } else {
                        _loginState.value = UiState.Idle
                    }
                }
                .onFailure { _loginState.value = UiState.Idle }
        }
    }

    fun resetState() {
        _loginState.value = UiState.Idle
    }
}
