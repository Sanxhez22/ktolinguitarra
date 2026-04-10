package com.example.prueba.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PracticeScreen() {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Text(
            text = "Modo práctica",
            style = MaterialTheme.typography.headlineMedium
        )

        Text("Aquí se entrenará la guitarra con audio")

        Button(
            onClick = { },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Comenzar práctica")
        }
    }
}