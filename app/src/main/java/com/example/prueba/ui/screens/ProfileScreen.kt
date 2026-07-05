package com.example.prueba.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.prueba.data.model.UserSession
import com.example.prueba.data.repository.AuthRepository
import com.example.prueba.ui.theme.FretBlack
import com.example.prueba.ui.theme.FretGold
import com.example.prueba.ui.theme.FretMuted
import com.example.prueba.ui.theme.FretSurface
import com.example.prueba.ui.theme.FretText

@Composable
fun ProfileScreen() {
    // Datos reales de la sesión persistida (perfil de MongoDB vía /auth/google).
    var session by remember { mutableStateOf<UserSession?>(null) }
    LaunchedEffect(Unit) {
        session = AuthRepository.restoreSession().getOrNull()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FretBlack)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Perfil",
            style = MaterialTheme.typography.headlineMedium,
            color = FretText,
            fontWeight = FontWeight.Bold
        )

        val s = session
        if (s == null) {
            Text("No hay sesión activa.", color = FretMuted)
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = FretSurface),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = s.nombre, color = FretGold, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(text = s.email, color = FretMuted, fontSize = 14.sp)
                    Text(
                        text = "Nivel: ${s.nivel.replaceFirstChar { it.uppercase() }}",
                        color = FretText,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}
