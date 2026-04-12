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

enum class FasePractica { SELECCION, MODO, GUIADO, CRONOMETRO, ANALISIS, RESULTADO }

@Composable
fun PracticeScreen() {
    var fase by remember { mutableStateOf(FasePractica.SELECCION) }
    var ejercicioSel by remember { mutableStateOf<Ejercicio?>(null) }
    var modoSeleccionado by remember { mutableStateOf<String?>(null) } // "guiado" | "libre"
    var segundos by remember { mutableIntStateOf(0) }
    var activo by remember { mutableStateOf(false) }
    var metricas by remember { mutableStateOf<Map<String, Int>?>(null) }
    var ejerciciosCompletados by remember { mutableIntStateOf(0) }

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
                        modoSeleccionado = "libre"
                        activo = true
                        fase = FasePractica.CRONOMETRO
                    },
                    onGuiado = {
                        modoSeleccionado = "guiado"
                        fase = FasePractica.GUIADO
                    },
                    onBack = { fase = FasePractica.SELECCION }
                )
            }

            FasePractica.GUIADO -> {
                EjerciciosGuiadosView(
                    ejercicioId = ejercicioSel!!.id,
                    onFinalizar = { completados ->
                        ejerciciosCompletados = completados
                        activo = true
                        fase = FasePractica.CRONOMETRO
                    },
                    onBack = { fase = FasePractica.MODO }
                )
            }

            FasePractica.ANALISIS -> {
                AnalisisView(
                    onComplete = {
                        metricas = simularMetricas(ejercicioSel!!.id, segundos)
                        fase = FasePractica.RESULTADO
                    }
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
                        fase = FasePractica.ANALISIS
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
                                .clickable { 
                                    selected = ej
                                    onSelect(ej)
                                },
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

// Datos de ejercicios guiados
private data class EjercicioGuiado(
    val titulo: String,
    val descripcion: String,
    val patron: List<String>,
    val tip: String,
    val bpm: Int? = null,
    val duracion: Int = 90
)

private val EJERCICIOS_POR_TIPO = mapOf(
    "escalas" to listOf(
        EjercicioGuiado("Posición de la mano", "Coloca el pulgar detrás del mástil, dedos curvados.", listOf("1", "2", "3", "4"), "Mantén los dedos cerca de los trastes."),
        EjercicioGuiado("Escala de Do Mayor - Subida", "Sigue: C-D-E-F-G-A-B-C.", listOf("C", "D", "E", "F", "G", "A", "B", "C↑"), "Usa un dedo por traste.", 60),
        EjercicioGuiado("Escala de Do Mayor - Bajada", "En sentido inverso: C-B-A-G-F-E-D-C.", listOf("C↓", "B", "A", "G", "F", "E", "D", "C"), "Mantén el mismo ritmo.", 60),
        EjercicioGuiado("Escala a 80 BPM", "Repite la escala completa a 80 BPM.", listOf("↓", "↑"), "No pares si te equivocas.", 80),
        EjercicioGuiado("Escala pentatónica menor", "Posición 1: trastes 5-8.", listOf("A", "C", "D", "E", "G", "A↑"), "¡Memorízala!", 70)
    ),
    "acordes" to listOf(
        EjercicioGuiado("Acorde de La menor (Am)", "Dedos: índice 2ª/c1, medio 4ª/c2, anular 3ª/c2.", listOf("Am", "×", "○", "2", "2", "1", "○"), "Presiona fuerte."),
        EjercicioGuiado("Acorde de Do Mayor (C)", "Índice 2ª/c1, medio 4ª/c2, anular 5ª/c3.", listOf("C", "×", "3", "2", "○", "1", "○"), "Practica hasta que salga limpio."),
        EjercicioGuiado("Cambio Am → C", "Alterna entre Am y C cada 4 tiempos.", listOf("Am", "Am", "C", "C"), "Anticipa el cambio.", 60),
        EjercicioGuiado("Acorde de Sol Mayor (G)", "Meñique 1ª/c3, anular 6ª/c3, medio 5ª/c2.", listOf("G", "3", "2", "○", "○", "○", "3"), "Practica la forma."),
        EjercicioGuiado("Progresión Am - C - G", "La progresión más popular del pop.", listOf("Am", "C", "G", "Am"), "¡Base de cientos de canciones!", 70)
    ),
    "fingerpicking" to listOf(
        EjercicioGuiado("Posición mano derecha", "Pulgar→cuerdas 4-5-6, índice→3ª, medio→2ª, anular→1ª.", listOf("p", "i", "m", "a"), "No apoyes la mano."),
        EjercicioGuiado("Patrón básico p-i-m-a", "Pulgar→6ª, índice→3ª, medio→2ª, anular→1ª.", listOf("p", "i", "m", "a", "p", "i", "m", "a"), "Empieza lento.", 60),
        EjercicioGuiado("Patrón p-i-m-i", "Pulgar, índice, medio, índice.", listOf("p", "i", "m", "i"), "El índice es el pivot.", 60),
        EjercicioGuiado("Travis Picking", "Pulgar alterna 6ª y 4ª.", listOf("p6", "i", "p4", "m"), "Estilo Paul Simon.", 60)
    ),
    "rasgueo" to listOf(
        EjercicioGuiado("Rasgueo hacia abajo", "Muñeca relajada, rasguea hacia abajo.", listOf("↓", "↓", "↓", "↓"), "El movimiento viene de la muñeca."),
        EjercicioGuiado("Rasgueo arriba-abajo", "Alterna ↓ y ↑.", listOf("↓", "↑", "↓", "↑"), "El ↑ es más suave.", 60),
        EjercicioGuiado("Patrón D-DU-UDU", "El patrón más común: ↓ ↓↑ ↑↓↑.", listOf("↓", "↓", "↑", "↓", "↑"), "Usado en Wonderwall.", 70),
        EjercicioGuiado("Muting con palma", "Apoya la palma cerca del puente.", listOf("PM↓", "PM↓", "↓", "↑"), "Sonido chunky del rock.", 80)
    ),
    "ritmo" to listOf(
        EjercicioGuiado("Sentir el pulso", "Golpea el cuerpo en tiempos 1 y 3.", listOf("1", "2", "3", "4"), "Sin ritmo no hay música."),
        EjercicioGuiado("Subdivisión en corcheas", "Cuenta: 1-y-2-y-3-y-4-y.", listOf("1", "y", "2", "y"), "Las y son importantes.", 70),
        EjercicioGuiado("Síncopa básica", "Acento en tiempos 2 y 4.", listOf("1", "2", "3", "4"), "El backbeat del rock.", 70),
        EjercicioGuiado("Compás de 3/4 (Vals)", "Tres tiempos por compás.", listOf("1", "2", "3"), "Música latinoamericana.", 60)
    ),
    "calentamiento" to listOf(
        EjercicioGuiado("Estiramiento de dedos", "Estira cada dedo por 10 segundos.", listOf("🤙", "💍", "🖕", "👆"), "Nunca toques sin calentar."),
        EjercicioGuiado("Ejercicio 1-2-3-4 Araña", "Dedos 1-2-3-4 en trastes consecutivos.", listOf("1", "2", "3", "4 →"), "Entrena independencia.", 60),
        EjercicioGuiado("Trinos de dedos", "Alterna índice-medio, medio-anular.", listOf("i-m", "m-a"), "Mejoran velocidad.", 80),
        EjercicioGuiado("Cromatismo completo", "Cada semitraste consecutivas.", listOf("T1", "T2", "T3"), "Base de profesionales.", 70)
    )
)

@Composable
fun EjerciciosGuiadosView(
    ejercicioId: String,
    onFinalizar: (Int) -> Unit,
    onBack: () -> Unit
) {
    val ejercicios = EJERCICIOS_POR_TIPO[ejercicioId] ?: emptyList()
    var paso by remember { mutableIntStateOf(0) }
    var completados by remember { mutableStateOf(setOf<Int>()) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "📋", fontSize = 24.sp)
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(text = "Práctica guiada", color = FretText, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(text = "Sigue cada ejercicio", color = FretMuted, fontSize = 13.sp)
            }
        }

        if (ejercicios.isNotEmpty()) {
            // Progreso
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Ejercicio ${paso + 1} de ${ejercicios.size}", color = FretMuted, fontSize = 12.sp)
                val progreso = (completados.size * 100) / ejercicios.size
                Text(text = "$progreso%", color = FretGold, fontSize = 12.sp)
            }

            // Barrita progreso
            LinearProgressIndicator(
                progress = { completados.size.toFloat() / ejercicios.size },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = FretGold,
                trackColor = FretSurface
            )

            // Indicadores de pasos
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                ejercicios.forEachIndexed { i, _ ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .background(
                                when {
                                    completados.contains(i) -> Color(0xFF4ADE80)
                                    i == paso -> FretGold
                                    else -> FretSurface
                                },
                                RoundedCornerShape(2.dp)
                            )
                    )
                }
            }

            // Ejercicio actual
            val actual = ejercicios[paso]
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = FretSurface),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Ejercicio ${paso + 1}",
                            color = FretGold,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        if (completados.contains(paso)) {
                            Text(text = "✓", color = Color(0xFF4ADE80), fontSize = 16.sp)
                        }
                    }
                    Text(text = actual.titulo, color = FretText, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text(text = actual.descripcion, color = FretMuted, fontSize = 14.sp, lineHeight = 20.sp)

                    // Patrón
                    if (actual.patron.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            actual.patron.take(6).forEach { p ->
                                Box(
                                    modifier = Modifier
                                        .background(FretBlack, RoundedCornerShape(10.dp))
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(text = p, color = FretText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        }
                    }

                    // BPM y duración
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (actual.bpm != null) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF5AC8FA).copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                                    .padding(12.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "${actual.bpm}", color = Color(0xFF5AC8FA), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text(text = "BPM", color = FretMuted, fontSize = 10.sp)
                                }
                            }
                        }
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFFB923C).copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "${actual.duracion}s", color = Color(0xFFFB923C), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(text = "Duración", color = FretMuted, fontSize = 10.sp)
                            }
                        }
                    }

                    // Tip
                    Box(
                        modifier = Modifier
                            .background(FretGold.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(text = "💡 Tip de Wilfredo", color = FretGold, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = actual.tip, color = FretText, fontSize = 13.sp)
                        }
                    }
                }
            }

            // Botones navegación
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (paso > 0) {
                    Button(
                        onClick = { paso-- },
                        colors = ButtonDefaults.buttonColors(containerColor = FretSurface, contentColor = FretText),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("← Anterior", fontWeight = FontWeight.SemiBold)
                    }
                }
                Button(
                    onClick = {
                        if (!completados.contains(paso)) {
                            completados = completados + paso
                        }
                        if (paso < ejercicios.size - 1) {
                            paso++
                        } else {
                            onFinalizar(completados.size)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = FretGold, contentColor = FretBlack),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = if (paso < ejercicios.size - 1) {
                            if (completados.contains(paso)) "Siguiente →" else "✓ Listo, siguiente"
                        } else "🎉 Finalizar",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("← Cambiar modo", color = FretMuted)
        }
    }
}

@Composable
fun AnalisisView(onComplete: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(2500)
        onComplete()
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Spinner
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(FretGold.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = FretGold,
                strokeWidth = 4.dp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(text = "Wilfredo está analizando...", color = FretText, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Generando feedback personalizado 🎸", color = FretMuted, fontSize = 14.sp)

        Spacer(modifier = Modifier.height(24.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Procesando métricas...", "Revisando historial...", "Preparando feedback...").forEachIndexed { i, t ->
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(i * 500L)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "✓", color = FretGold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = t, color = FretMuted, fontSize = 12.sp)
                }
            }
        }
    }
}