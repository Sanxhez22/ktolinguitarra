package com.example.prueba.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.prueba.api.EntrenadorResponse
import com.example.prueba.api.HabilidadResumenDto
import com.example.prueba.data.repository.AuthRepository
import com.example.prueba.ui.theme.FretBlack
import com.example.prueba.ui.theme.FretGold
import com.example.prueba.ui.theme.FretMuted
import com.example.prueba.ui.theme.FretSurface
import com.example.prueba.ui.theme.FretText
import com.example.prueba.viewmodel.HomeViewModel
import com.example.prueba.viewmodel.UiState
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel = viewModel(),
    onNavigate: (String) -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val state by homeViewModel.homeState.collectAsState()
    LaunchedEffect(Unit) { homeViewModel.loadHomeData() }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FretBlack)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        when (val s = state) {
            is UiState.Success -> {
                val data = s.data
                TrainerHeader(
                    userName = data.userName,
                    racha = data.entrenador.racha,
                    onLogout = { scope.launch { AuthRepository.signOut(); onLogout() } }
                )
                if (data.afinacionPendiente) {
                    AvisoAfinacionBanner(onAfinar = { onNavigate("tuner") })
                }
                data.entrenador.celebracion?.let { CelebracionBanner(it.titulo, it.mensaje) }
                PracticaDeHoyCard(data.entrenador, onNavigate = onNavigate)
                ObjetivoDiaCard(data.entrenador)
                HabilidadesResumenCard(
                    debiles = data.entrenador.habilidadesDebiles,
                    fuertes = data.entrenador.habilidadesFuertes,
                    onVerProgreso = { onNavigate("progress") }
                )
                data.entrenador.proximoLogro?.let { ProximoLogroCard(it.titulo, it.detalle) }
                ConsejoWilfredoCard(data.entrenador.consejo)
                Spacer(modifier = Modifier.height(90.dp))
            }

            is UiState.Error -> HomeErrorState(s.message, onReintentar = { homeViewModel.loadHomeData() })

            else -> Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 80.dp),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = FretGold) }
        }
    }
}

@Composable
private fun TrainerHeader(userName: String, racha: Int, onLogout: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Hola, $userName 👋",
                color = FretText,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Soy Wilfredo, tu entrenador. Esto es lo que toca hoy.",
                color = FretMuted,
                fontSize = 14.sp
            )
        }
        if (racha > 0) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocalFireDepartment, contentDescription = "Racha", tint = Color(0xFFE94584))
                Text(text = "$racha", color = FretText, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
        TextButton(onClick = onLogout) { Text("Salir", color = FretMuted, fontSize = 12.sp) }
    }
}

/**
 * Aviso mientras el usuario no haya afinado (omitió el afinador del
 * onboarding). Desaparece solo cuando el backend registra una afinación real.
 */
@Composable
private fun AvisoAfinacionBanner(onAfinar: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF3A2E10)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "🎸 Tu guitarra aún no ha sido afinada",
                color = FretGold,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Text(
                text = "Para obtener mejores resultados te recomendamos afinarla antes de practicar.",
                color = FretText,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
            Button(
                onClick = onAfinar,
                colors = ButtonDefaults.buttonColors(containerColor = FretGold, contentColor = FretBlack),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Afinar ahora", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun CelebracionBanner(titulo: String, mensaje: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E3A29)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = "🎉 $titulo", color = Color(0xFF4ADE80), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(text = mensaje, color = FretText, fontSize = 14.sp, lineHeight = 19.sp)
        }
    }
}

@Composable
private fun PracticaDeHoyCard(entrenador: EntrenadorResponse, onNavigate: (String) -> Unit) {
    val rec = entrenador.recomendacion
    val esDescanso = rec.tipo == "descanso"
    val gradient = Brush.horizontalGradient(listOf(Color(0xFF1A2233), Color(0xFF0F1725)))

    val etiquetaTipo = when (rec.tipo) {
        "repetir" -> "🔁 A repetir"
        "subir_dificultad" -> "🚀 Subimos el nivel"
        "bajar_dificultad" -> "🐢 Afianzando la base"
        "descanso" -> "🌙 Momento de descanso"
        else -> "🎯 Tu práctica de hoy"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
    ) {
        Column(
            modifier = Modifier.background(gradient).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = etiquetaTipo, color = FretGold, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)

            Text(
                text = entrenador.ejercicio?.nombre ?: if (esDescanso) "Descansa hoy" else "Práctica libre",
                color = FretText,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            if (!esDescanso && rec.dificultad != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Dificultad ${rec.dificultad}/5", color = FretMuted, fontSize = 13.sp)
                    rec.duracionMin?.let { Text("~$it min", color = FretMuted, fontSize = 13.sp) }
                }
            }

            // El "por qué" de la recomendación (motor explicable).
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0x22FFFFFF)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("¿Por qué?", color = FretGold, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text(text = rec.razon, color = FretText, fontSize = 13.sp, lineHeight = 18.sp)
                }
            }

            if (!esDescanso) {
                Button(
                    onClick = {
                        val ej = rec.ejercicioId
                        onNavigate(if (ej != null) "practice?ej=$ej" else "practice")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FretGold, contentColor = FretBlack),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Empezar", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ObjetivoDiaCard(entrenador: EntrenadorResponse) {
    val o = entrenador.objetivoDia
    val progress = if (o.metaMin > 0) (o.minutosHoy / o.metaMin).coerceIn(0.0, 1.0).toFloat() else 0f
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = FretSurface),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Objetivo del día", color = FretGold, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(
                text = if (o.cumplido) "¡Meta cumplida! Practicaste ${o.minutosHoy.toInt()} min hoy. 🎯"
                else "Te faltan ${o.restanteMin.toInt()} min de ${o.metaMin} para tu meta de hoy.",
                color = FretText,
                fontSize = 15.sp,
                lineHeight = 20.sp
            )
            Text("Progreso de hoy: ${o.minutosHoy.toInt()} / ${o.metaMin} min", color = FretMuted, fontSize = 14.sp)
            Box(
                modifier = Modifier.fillMaxWidth().height(10.dp)
                    .background(Color(0xFF222831), RoundedCornerShape(50))
            ) {
                if (progress > 0f) {
                    Box(
                        modifier = Modifier.fillMaxWidth(progress).height(10.dp)
                            .background(FretGold, RoundedCornerShape(50))
                    )
                }
            }
        }
    }
}

@Composable
private fun HabilidadesResumenCard(
    debiles: List<HabilidadResumenDto>,
    fuertes: List<HabilidadResumenDto>,
    onVerProgreso: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = FretSurface),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Tus habilidades", color = FretGold, fontWeight = FontWeight.Bold, fontSize = 18.sp)

            if (fuertes.isNotEmpty()) {
                Text("💪 Más fuertes", color = FretMuted, fontSize = 13.sp)
                SkillChips(fuertes, Color(0xFF9EF01A))
            }
            Text("🎯 A reforzar", color = FretMuted, fontSize = 13.sp)
            SkillChips(debiles, Color(0xFFE94584))

            TextButton(onClick = onVerProgreso) {
                Text("Ver todo mi progreso →", color = FretGold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun SkillChips(skills: List<HabilidadResumenDto>, accent: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        skills.forEach { s ->
            Card(
                colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    text = "${s.nombre} · N${s.nivel}",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    color = accent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ProximoLogroCard(titulo: String, detalle: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF101722)),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("🏅 Próximo logro", color = FretGold, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(titulo, color = FretText, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(detalle, color = FretMuted, fontSize = 14.sp, lineHeight = 19.sp)
        }
    }
}

@Composable
private fun ConsejoWilfredoCard(consejo: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF101722)),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Consejo de Wilfredo", color = FretGold, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(
                text = consejo.ifBlank { "Practica un poco cada día: la constancia vale más que las sesiones largas. 🎸" },
                color = FretText,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun HomeErrorState(mensaje: String, onReintentar: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = FretSurface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("No se pudo cargar tu entrenador", color = FretText, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Text(mensaje, color = FretMuted, fontSize = 14.sp)
            Button(
                onClick = onReintentar,
                colors = ButtonDefaults.buttonColors(containerColor = FretGold, contentColor = FretBlack),
                shape = RoundedCornerShape(16.dp)
            ) { Text("Reintentar", fontWeight = FontWeight.Bold) }
        }
    }
}
