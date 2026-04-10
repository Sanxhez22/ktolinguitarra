package com.example.prueba.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SearchScreen() {

    var texto by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Text(
            text = "Buscar canciones",
            style = MaterialTheme.typography.headlineMedium
        )

        OutlinedTextField(
            value = texto,
            onValueChange = { texto = it },
            label = { Text("Nombre de canción") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = { },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Buscar")
        }
    }
}