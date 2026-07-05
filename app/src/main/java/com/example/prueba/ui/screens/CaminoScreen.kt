package com.example.prueba.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.prueba.api.PasoCaminoDto
import com.example.prueba.ui.theme.FretBlack
import com.example.prueba.ui.theme.FretGold
import com.example.prueba.ui.theme.FretMuted
import com.example.prueba.ui.theme.FretSurface
import com.example.prueba.ui.theme.FretText
import com.example.prueba.viewmodel.CaminoViewModel
import com.example.prueba.viewmodel.UiState

/**
 * Camino de aprendizaje (P1): ruta vertical estilo Duolingo con los 10 pasos.
 * Cada nodo muestra su estado (completado / en curso / disponible / bloqueado);
 * el ejercicio del paso en curso se puede lanzar directamente.
 */
@Composable
fun CaminoScreen(
    caminoViewModel: CaminoViewModel = viewModel(),
    onPracticar: (String) -> Unit = {}
) {
    val state by caminoViewModel.caminoState.collectAsState()
    LaunchedEffect(Unit) { caminoViewModel.loadCamino() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FretBlack)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Tu camino",
            style = MaterialTheme.typography.headlineMedium,
            color = FretText,
            fontWeight = FontWeight.Bold
        )

        when (val s = state) {
            is UiState.Success -> {
                val camino = s.data
                Text(
                    text = "Paso ${camino.completados} de ${camino.totalPasos} completados",
                    color = FretMuted,
                    fontSize = 14.sp
                )
                LinearProgressIndicator(
                    progress = { if (camino.totalPasos > 0) camino.completados.toFloat() / camino.totalPasos else 0f },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = FretGold,
                    trackColor = FretSurface
                )
                Spacer(modifier = Modifier.height(4.dp))

                camino.pasos.forEachIndexed { i, paso ->
                    PasoNodo(
                        paso = paso,
                        numero = i + 1,
                        esUltimo = i == camino.pasos.lastIndex,
                        onPracticar = onPracticar
                    )
                }
                Spacer(modifier = Modifier.height(90.dp))
            }

            is UiState.Error -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = FretSurface),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("No se pudo cargar tu camino", color = FretText, fontWeight = FontWeight.Bold)
                        Text(s.message, color = FretMuted, fontSize = 14.sp)
                        Button(
                            onClick = { caminoViewModel.loadCamino() },
                            colors = ButtonDefaults.buttonColors(containerColor = FretGold, contentColor = FretBlack)
                        ) { Text("Reintentar") }
                    }
                }
            }

            else -> Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 60.dp),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = FretGold) }
        }
    }
}

@Composable
private fun PasoNodo(
    paso: PasoCaminoDto,
    numero: Int,
    esUltimo: Boolean,
    onPracticar: (String) -> Unit
) {
    val (nodoColor, icono) = when (paso.estado) {
        "completado" -> Color(0xFF4ADE80) to "✓"
        "en_curso" -> FretGold to "$numero"
        "disponible" -> Color(0xFF5AC8FA) to "$numero"
        else -> Color(0xFF2A2F3A) to "🔒"
    }
    val bloqueado = paso.estado == "bloqueado"

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        // Columna del nodo + línea conectora.
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(44.dp).background(nodoColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = icono,
                    color = if (bloqueado) FretMuted else FretBlack,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            if (!esUltimo) {
                Box(
                    modifier = Modifier.width(3.dp).height(48.dp)
                        .background(if (paso.estado == "completado") Color(0xFF4ADE80) else Color(0xFF2A2F3A))
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Contenido del paso.
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (paso.estado == "en_curso") FretGold.copy(alpha = 0.12f) else FretSurface
            ),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = paso.nombre,
                        color = if (bloqueado) FretMuted else FretText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    EstadoChip(paso.estado)
                }
                Text(
                    text = paso.descripcion,
                    color = FretMuted,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
                if (paso.estado == "en_curso" && paso.ejercicios.isNotEmpty()) {
                    Button(
                        onClick = { onPracticar(paso.ejercicios.first()) },
                        colors = ButtonDefaults.buttonColors(containerColor = FretGold, contentColor = FretBlack),
                        shape = RoundedCornerShape(14.dp)
                    ) { Text("Practicar", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

@Composable
private fun EstadoChip(estado: String) {
    val (texto, color) = when (estado) {
        "completado" -> "Completado" to Color(0xFF4ADE80)
        "en_curso" -> "En curso" to FretGold
        "disponible" -> "Disponible" to Color(0xFF5AC8FA)
        else -> "Bloqueado" to FretMuted
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(50)
    ) {
        Text(
            text = texto,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
