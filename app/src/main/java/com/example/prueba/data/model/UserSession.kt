package com.example.prueba.data.model

data class UserSession(
    val id: String,
    val nombre: String,
    val email: String,
    val foto: String? = null,
    val nivel: String = "Principiante",
    val token: String
)
