package com.example.prueba.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.prueba.audio.TunerEngine
import com.example.prueba.ui.theme.FretBlack
import com.example.prueba.ui.theme.FretGold
import com.example.prueba.ui.theme.FretMuted
import com.example.prueba.ui.theme.FretSurface
import com.example.prueba.ui.theme.FretText
import com.example.prueba.viewmodel.OnboardingViewModel
import com.example.prueba.viewmodel.UiState
import kotlin.math.abs
import kotlin.math.ln

/**
 * Onboarding de usuarios nuevos (solo aparece la primera vez):
 * Bienvenida → Experiencia → Objetivo → (guardar en MongoDB) →
 * "Vamos a preparar tu guitarra" → Afinador obligatorio (6 cuerdas) →
 * Primera práctica → Home.
 */
private enum class PasoOnboarding { BIENVENIDA, EXPERIENCIA, OBJETIVO, PREPARAR, AFINADOR, PRACTICA }

private data class Opcion(val valor: String, val etiqueta: String, val emoji: String)

private val OPCIONES_EXPERIENCIA = listOf(
    Opcion("nunca", "Nunca he tocado guitarra", "🌱"),
    Opcion("principiante", "Principiante", "🎸"),
    Opcion("intermedio", "Intermedio", "🎵"),
    Opcion("avanzado", "Avanzado", "🔥")
)

private val OPCIONES_OBJETIVO = listOf(
    Opcion("aprender_desde_cero", "Aprender desde cero", "📖"),
    Opcion("mejorar_tecnica", "Mejorar técnica", "💪"),
    Opcion("aprender_canciones", "Aprender canciones", "🎶"),
    Opcion("practicar_diario", "Practicar diariamente", "📅")
)

@Composable
fun OnboardingScreen(
    onboardingViewModel: OnboardingViewModel = viewModel(),
    onFinished: () -> Unit
) {
    var paso by remember { mutableStateOf(PasoOnboarding.BIENVENIDA) }
    var experiencia by remember { mutableStateOf<String?>(null) }
    var objetivo by remember { mutableStateOf<String?>(null) }

    val saveState by onboardingViewModel.saveState.collectAsState()
    val completeState by onboardingViewModel.completeState.collectAsState()

    when (paso) {
        PasoOnboarding.PRACTICA -> {
            // Primera práctica: el flujo real de práctica con grabación y
            // análisis. Al terminar se marca el onboarding como completado.
            Column(modifier = Modifier.fillMaxSize().background(FretBlack)) {
                Text(
                    text = "Última parada: tu primera práctica 🎉",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp),
                    color = FretGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
                if (completeState is UiState.Error) {
                    Text(
                        text = "⚠️ ${(completeState as UiState.Error).message}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        color = Color(0xFFFF8A9B),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
                PracticeScreen(
                    onSessionComplete = {
                        onboardingViewModel.completeOnboarding(onCompleted = onFinished)
                    }
                )
            }
        }

        PasoOnboarding.AFINADOR -> {
            OnboardingTunerStep(
                onAfinada = { paso = PasoOnboarding.PRACTICA },
                onOmitir = {
                    onboardingViewModel.omitirAfinacion {
                        paso = PasoOnboarding.PRACTICA
                    }
                }
            )
        }

        else -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(FretBlack)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                OnboardingProgress(paso)

                when (paso) {
                    PasoOnboarding.BIENVENIDA -> BienvenidaStep(
                        onContinuar = { paso = PasoOnboarding.EXPERIENCIA }
                    )

                    PasoOnboarding.EXPERIENCIA -> PreguntaStep(
                        titulo = "¿Cuál es tu experiencia con la guitarra?",
                        subtitulo = "Wilfredo ajustará los ejercicios a tu nivel.",
                        opciones = OPCIONES_EXPERIENCIA,
                        seleccion = experiencia,
                        onSelect = { experiencia = it },
                        botonTexto = "Continuar",
                        botonEnabled = experiencia != null,
                        onContinuar = { paso = PasoOnboarding.OBJETIVO }
                    )

                    PasoOnboarding.OBJETIVO -> {
                        PreguntaStep(
                            titulo = "¿Cuál es tu objetivo?",
                            subtitulo = "Esto define tu plan de práctica.",
                            opciones = OPCIONES_OBJETIVO,
                            seleccion = objetivo,
                            onSelect = { objetivo = it },
                            botonTexto = if (saveState is UiState.Loading) "Guardando..." else "Continuar",
                            botonEnabled = objetivo != null && saveState !is UiState.Loading,
                            onContinuar = {
                                onboardingViewModel.saveAnswers(
                                    experiencia = experiencia ?: "principiante",
                                    objetivo = objetivo ?: "practicar_diario",
                                    onSaved = { paso = PasoOnboarding.PREPARAR }
                                )
                            }
                        )
                        if (saveState is UiState.Error) {
                            Text(
                                text = "⚠️ ${(saveState as UiState.Error).message}",
                                color = Color(0xFFFF8A9B),
                                fontSize = 13.sp
                            )
                        }
                    }

                    PasoOnboarding.PREPARAR -> PrepararStep(
                        onContinuar = { paso = PasoOnboarding.AFINADOR }
                    )

                    else -> {}
                }
            }
        }
    }
}

@Composable
private fun OnboardingProgress(paso: PasoOnboarding) {
    val progreso = (paso.ordinal + 1) / PasoOnboarding.entries.size.toFloat()
    LinearProgressIndicator(
        progress = { progreso },
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp),
        color = FretGold,
        trackColor = FretSurface
    )
}

@Composable
private fun BienvenidaStep(onContinuar: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(FretGold, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("🎸", fontSize = 44.sp)
        }
        Text(
            text = "¡Bienvenido a FretMind!",
            color = FretText,
            fontWeight = FontWeight.Black,
            fontSize = 28.sp,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Soy Wilfredo, tu instructor personal de guitarra. En unos pasos dejamos todo listo para tu primera práctica.",
            color = FretMuted,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onContinuar,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = FretGold, contentColor = FretBlack),
            shape = RoundedCornerShape(20.dp)
        ) {
            Text("Empezar", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun PreguntaStep(
    titulo: String,
    subtitulo: String,
    opciones: List<Opcion>,
    seleccion: String?,
    onSelect: (String) -> Unit,
    botonTexto: String,
    botonEnabled: Boolean,
    onContinuar: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            text = titulo,
            color = FretText,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            lineHeight = 30.sp
        )
        Text(text = subtitulo, color = FretMuted, fontSize = 14.sp)

        opciones.forEach { opcion ->
            val selected = seleccion == opcion.valor
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(opcion.valor) },
                colors = CardDefaults.cardColors(
                    containerColor = if (selected) FretGold.copy(alpha = 0.18f) else FretSurface
                ),
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(text = opcion.emoji, fontSize = 22.sp)
                    Text(
                        text = opcion.etiqueta,
                        color = if (selected) FretGold else FretText,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 16.sp,
                        modifier = Modifier.weight(1f)
                    )
                    if (selected) {
                        Text(text = "✓", color = FretGold, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Button(
            onClick = onContinuar,
            enabled = botonEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = FretGold,
                contentColor = FretBlack,
                disabledContainerColor = FretSurface,
                disabledContentColor = FretMuted
            ),
            shape = RoundedCornerShape(20.dp)
        ) {
            Text(botonTexto, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun PrepararStep(onContinuar: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        Text("🎯", fontSize = 56.sp)
        Text(
            text = "Vamos a preparar tu guitarra.",
            color = FretText,
            fontWeight = FontWeight.Black,
            fontSize = 26.sp,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Antes de tocar, tu guitarra debe estar afinada. Afinaremos las 6 cuerdas juntas: toca cada cuerda y sigue las indicaciones.",
            color = FretMuted,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onContinuar,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = FretGold, contentColor = FretBlack),
            shape = RoundedCornerShape(20.dp)
        ) {
            Text("Afinar mi guitarra", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

/**
 * Afinador obligatorio del onboarding: no se puede continuar hasta que las
 * 6 cuerdas se detecten afinadas (±10 cents sostenidos frente a su frecuencia
 * objetivo real).
 */
@Composable
private fun OnboardingTunerStep(
    onAfinada: () -> Unit,
    onOmitir: () -> Unit = {}
) {
    val context = LocalContext.current

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasMicPermission = granted }

    var notaDetectada by remember { mutableStateOf<String?>(null) }
    var centsVsObjetivo by remember { mutableFloatStateOf(0f) }
    var cuerdaActual by remember { mutableStateOf<Cuerda?>(null) }
    var afinadas by remember { mutableStateOf(setOf<String>()) }
    var enTonoConsecutivos by remember { mutableIntStateOf(0) }

    val engine = remember {
        TunerEngine(onPitch = { freq ->
            if (freq > 0f) {
                val cuerda = nearestString(freq)
                val (nombreNota, octava, _) = frequencyToNote(freq)
                notaDetectada = "$nombreNota$octava"
                cuerdaActual = cuerda
                if (cuerda != null) {
                    // Cents reales contra la frecuencia objetivo de la cuerda.
                    val cents = (1200.0 * ln(freq / cuerda.freq.toDouble()) / ln(2.0)).toFloat()
                    centsVsObjetivo = cents
                    if (abs(cents) < 10f) {
                        enTonoConsecutivos++
                        // ~0.4 s sostenido en tono para marcar la cuerda.
                        if (enTonoConsecutivos >= 4 && cuerda.nombre !in afinadas) {
                            afinadas = afinadas + cuerda.nombre
                        }
                    } else {
                        enTonoConsecutivos = 0
                    }
                }
            }
        })
    }

    DisposableEffect(hasMicPermission) {
        if (hasMicPermission) {
            engine.start()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
        onDispose { engine.stop() }
    }

    val todasAfinadas = afinadas.size == CUERDAS.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FretBlack)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Afina tu guitarra",
            color = FretText,
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp
        )
        Text(
            text = "Toca cada cuerda al aire. Se marcará en verde cuando esté afinada. Este paso es obligatorio.",
            color = FretMuted,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )

        if (!hasMicPermission) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE94584).copy(alpha = 0.15f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "🎙️ Necesito el micrófono para escuchar tu guitarra.",
                        color = FretText,
                        fontSize = 14.sp
                    )
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                        colors = ButtonDefaults.buttonColors(containerColor = FretGold, contentColor = FretBlack),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Conceder permiso", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Progreso de cuerdas afinadas
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CUERDAS.forEach { c ->
                val lista = c.nombre in afinadas
                val esActual = cuerdaActual?.nombre == c.nombre
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            lista -> Color(0xFF1E3A29)
                            esActual -> FretGold.copy(alpha = 0.2f)
                            else -> FretSurface
                        }
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = c.nombre,
                            color = if (lista) Color(0xFF4ADE80) else FretText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = if (lista) "✓" else c.numero,
                            color = if (lista) Color(0xFF4ADE80) else FretMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Display de afinación en vivo
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = FretSurface),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (notaDetectada != null) {
                    val enTono = abs(centsVsObjetivo) < 10f
                    Text(
                        text = notaDetectada!!,
                        color = if (enTono) Color(0xFF9EF01A) else FretGold,
                        fontWeight = FontWeight.Black,
                        fontSize = 56.sp
                    )
                    cuerdaActual?.let { c ->
                        Text(
                            text = "Cuerda ${c.numero} (${c.nombre})",
                            color = FretMuted,
                            fontSize = 14.sp
                        )
                    }
                    Text(
                        text = when {
                            abs(centsVsObjetivo) < 10f -> "✅ ¡En tono! Mantenla..."
                            centsVsObjetivo > 0 -> "▲ Alta: afloja la clavija despacio"
                            else -> "▼ Baja: aprieta la clavija despacio"
                        },
                        color = if (abs(centsVsObjetivo) < 10f) Color(0xFF9EF01A) else FretText,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                } else {
                    Text("🎵", fontSize = 44.sp)
                    Text(
                        text = if (hasMicPermission) "Toca una cuerda..." else "Esperando permiso de micrófono",
                        color = FretMuted,
                        fontSize = 14.sp
                    )
                }
            }
        }

        if (todasAfinadas) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E3A29)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "🎉 ¡Guitarra afinada! Estás listo para tu primera práctica.",
                    modifier = Modifier.padding(14.dp),
                    color = Color(0xFF4ADE80),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }

        Button(
            onClick = onAfinada,
            enabled = todasAfinadas,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = FretGold,
                contentColor = FretBlack,
                disabledContainerColor = FretSurface,
                disabledContentColor = FretMuted
            ),
            shape = RoundedCornerShape(20.dp)
        ) {
            if (todasAfinadas) {
                Text("Continuar a mi primera práctica", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = FretMuted,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("Afinadas: ${afinadas.size} / ${CUERDAS.size}", fontWeight = FontWeight.Bold)
            }
        }

        // Salida secundaria y discreta: se puede omitir la afinación, pero la
        // recomendación clara sigue siendo afinar primero.
        if (!todasAfinadas) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Recomendamos afinar antes de tu primera práctica.",
                    color = FretMuted,
                    fontSize = 12.sp
                )
                TextButton(onClick = onOmitir) {
                    Text(
                        text = "Afinar después",
                        color = FretMuted,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
