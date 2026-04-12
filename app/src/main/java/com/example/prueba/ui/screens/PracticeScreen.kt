package com.example.prueba.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.prueba.ui.theme.*
import kotlinx.coroutines.delay

data class Ejercicio(
    val id: String,
    val label: String,
    val emoji: String,
    val desc: String
)

private val EJERCICIOS = listOf(
    Ejercicio("escalas", "Escalas", "🎼", "Mayor, menor, pentatónica"),
    Ejercicio("acordes", "Acordes", "🎸", "Abiertos y con cejilla"),
    Ejercicio("fingerpicking", "Fingerpicking", "🤌", "Patrones p-i-m-a"),
    Ejercicio("rasgueo", "Rasgueo", "⚡", "Strumming y ritmo"),
    Ejercicio("ritmo", "Ritmo", "🥁", "Compás y subdivisión"),
    Ejercicio("calentamiento", "Calentamiento", "🔥", "Ejercicios de dedos")
)

enum class FasePractica { SELECCION, MODO, CRONOMETRO, RESULTADO }

@Composable
fun PracticeScreen() {
    var fase by remember { mutableStateOf(FasePractica.SELECCION) }
    var ejercicioSel by remember { mutableStateOf<Ejercicio?>(null) }
    var modoLibre by remember { mutableStateOf(true) }
    var segundos by remember { mutableIntStateOf(0) }
    var activo by remember { mutableStateOf(false) }
    var metricas by remember { mutableStateOf<Map<String, Int>?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FretBlack)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        when (fase) {
            FasePractica.SELECCION -> {
                EjercicioSelection(
                    onSelect = { ej -> ejercicioSel = ej },
                    onContinuar = { ejercicioSel?.let { fase = FasePractica.MODO } }
                )
            }

            FasePractica.MODO -> {
                EjercicioModo(
                    ejercicio = ejercicioSel!!,
                    onLibre = {
                        modoLibre = true
                        activo = true
                        fase = FasePractica.CRONOMETRO
                    },
                    onGuiado = {
                        modoLibre = false
                        activo = true
                        fase = FasePractica.CRONOMETRO
                    },
                    onBack = { fase = FasePractica.SELECCION }
                )
            }

            FasePractica.CRONOMETRO -> {
                CronometroView(
                    ejercicio = ejercicioSel!!,
                    segundos = segundos,
                    activo = activo,
                    onTick = { segundos++ },
                    onDetener = {
                        activo = false
                        metricas = simularMetricas(ejercicioSel!!.id, segundos)
                        fase = FasePractica.RESULTADO
                    }
                )
            }

            FasePractica.RESULTADO -> {
                ResultadoView(
                    ejercicio = ejercicioSel!!,
                    segundos = segundos,
                    metricas = metricas,
                    onNueva = {
                        fase = FasePractica.SELECCION
                        ejercicioSel = null
                        segundos = 0
                        metricas = null
                    }
                )
            }
        }
    }
}

@Composable
fun EjercicioSelection(
    onSelect: (Ejercicio) -> Unit,
    onContinuar: () -> Unit
) {
    var selected by remember { mutableStateOf<Ejercicio?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            text = "Práctica",
            color = FretText,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp
        )

        Text(
            text = "Sesión con feedback de Wilfredo 🤖",
            color = FretMuted,
            fontSize = 14.sp
        )

        Text(
            text = "Elige un ejercicio",
            color = FretMuted,
            fontSize = 12.sp
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            EJERCICIOS.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    row.forEach { ej ->
                        val isSelected = selected?.id == ej.id
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selected = ej },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) FretGold.copy(alpha = 0.15f) else FretSurface
                            ),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(text = ej.emoji, fontSize = 24.sp)
                                Text(
                                    text = ej.label,
                                    color = FretText,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = ej.desc,
                                    color = FretMuted,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        if (selected != null) {
            Button(
                onClick = onContinuar,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = FretGold, contentColor = FretBlack),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text("Continuar con " + selected!!.label, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun EjercicioModo(
    ejercicio: Ejercicio,
    onLibre: () -> Unit,
    onGuiado: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = ejercicio.emoji, fontSize = 32.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = ejercicio.label,
                    color = FretText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
                Text(text = ejercicio.desc, color = FretMuted, fontSize = 14.sp)
            }
        }

        Text(
            text = "¿Cómo quieres practicar?",
            color = FretMuted,
            fontSize = 12.sp
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onGuiado() },
            colors = CardDefaults.cardColors(containerColor = FretSurface),
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(FretGold.copy(alpha = 0.2f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🎯", fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "Práctica guiada",
                        color = FretText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Ejercicios paso a paso.",
                        color = FretMuted,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onLibre() },
            colors = CardDefaults.cardColors(containerColor = FretSurface),
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color(0xFF1F252F), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("⏱️", fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "Práctica libre",
                        color = FretText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Toca a tu ritmo.",
                        color = FretMuted,
                        fontSize = 13.sp
                    )
                }
            }
        }

        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("← Cambiar ejercicio", color = FretMuted)
        }
    }
}

@Composable
fun CronometroView(
    ejercicio: Ejercicio,
    segundos: Int,
    activo: Boolean,
    onTick: () -> Unit,
    onDetener: () -> Unit
) {
    LaunchedEffect(activo) {
        while (activo) {
            delay(1000)
            onTick()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = ejercicio.emoji, fontSize = 32.sp)
        Text(
            text = ejercicio.label,
            color = FretText,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        val mins = segundos / 60
        val secs = segundos % 60
        val timeStr = String.format("%02d:%02d", mins, secs)

        Text(
            text = timeStr,
            color = FretText,
            fontWeight = FontWeight.Bold,
            fontSize = 56.sp
        )

        Text(text = "en curso", color = FretMuted, fontSize = 14.sp)

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(8) { i ->
                val anim = rememberInfiniteTransition(label = "bar$i")
                val height by anim.animateFloat(
                    initialValue = 8f,
                    targetValue = 28f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(400 + i * 80),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "barH$i"
                )
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(height.dp)
                        .background(FretGold, RoundedCornerShape(50))
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = onDetener,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFE94584),
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("■ Finalizar sesión", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ResultadoView(
    ejercicio: Ejercicio,
    segundos: Int,
    metricas: Map<String, Int>?,
    onNueva: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = FretGold.copy(alpha = 0.15f)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Text(
                text = "🎉 ¡Sesión completada!",
                modifier = Modifier.padding(16.dp),
                color = FretGold,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }

        val mins = segundos / 60
        val secs = segundos % 60
        val timeStr = String.format("%02d:%02d", mins, secs)
        val puntuacion = metricas?.get("puntuacion") ?: 8
        val bpm = metricas?.get("bpm") ?: 120

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = FretSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "$puntuacion/10",
                        color = FretGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    Text(text = "Puntuación", color = FretMuted, fontSize = 11.sp)
                }
            }
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = FretSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "$bpm",
                        color = Color(0xFF5AC8FA),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    Text(text = "BPM", color = FretMuted, fontSize = 11.sp)
                }
            }
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = FretSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = timeStr,
                        color = FretText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    Text(text = "Duración", color = FretMuted, fontSize = 11.sp)
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = FretSurface),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(FretGold, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("W", color = FretBlack, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Feedback de Wilfredo",
                        color = FretText,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }
                Text(
                    text = "¡Buen trabajo! Tu técnica mejora sostenidamente. 🎸",
                    color = FretText,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        }

        Button(
            onClick = onNueva,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = FretGold, contentColor = FretBlack),
            shape = RoundedCornerShape(20.dp)
        ) {
            Text("Nueva sesión", fontWeight = FontWeight.Bold)
        }
    }
}

private fun simularMetricas(ejercicioId: String, duracionSeg: Int): Map<String, Int> {
    val base = minOf(duracionSeg / 10, 10)
    return mapOf(
        "puntuacion" to (base + (3..5).random()),
        "bpm" to (80..140).random(),
        "precision" to (50..95).random(),
        "ritmo" to (50..98).random()
    )
}

private fun ClosedRange<Int>.random(): Int = (this.start..this.endInclusive).random()