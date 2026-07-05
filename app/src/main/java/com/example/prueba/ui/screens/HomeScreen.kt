package com.example.prueba.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.prueba.data.repository.AuthRepository
import com.example.prueba.ui.theme.FretBlack
import com.example.prueba.ui.theme.FretGold
import com.example.prueba.ui.theme.FretMuted
import com.example.prueba.ui.theme.FretSurface
import com.example.prueba.ui.theme.FretText
import com.example.prueba.viewmodel.HomeData
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
    val data = (state as? UiState.Success)?.data ?: HomeData()
    val scope = rememberCoroutineScope()
    var showRoutine by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FretBlack)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        HomeHeader(
            userName = data.userName,
            aiLevel = data.aiLevel,
            minutesToday = data.minutesToday,
            onLogout = { scope.launch { AuthRepository.signOut(); onLogout() } }
        )
        ContinuePracticeCard(
            lastExercise = data.lastExercise,
            completedSessions = data.completedSessions,
            onSeguir = { onNavigate("practice") },
            onVerRutina = { showRoutine = true }
        )
        StatsRow(
            streak = data.streak,
            accuracy = data.accuracy,
            completed = data.completedSessions
        )
        DailyGoalCard(minutesToday = data.minutesToday)
        NextExerciseCard(
            nextExercise = data.nextExercise,
            advice = data.routineAdvice,
            onPracticar = { onNavigate("practice") }
        )
        WilfredoTipCard(
            tip = data.wilfredoTip,
            loading = data.isTipLoading || state is UiState.Loading
        )
        Spacer(modifier = Modifier.height(90.dp))
    }

    if (showRoutine) {
        RoutineDialog(
            exercises = data.routineExercises,
            duration = data.routineDuration,
            advice = data.routineAdvice,
            onDismiss = { showRoutine = false },
            onPracticar = {
                showRoutine = false
                onNavigate("practice")
            }
        )
    }
}

@Composable
fun HomeHeader(
    userName: String = "Guitarrista",
    aiLevel: String = "principiante",
    minutesToday: Float = 0f,
    onLogout: () -> Unit = {}
) {
    val nivelDisplay = aiLevel.replaceFirstChar { it.uppercase() }
    val faltan = (META_DIARIA_MIN - minutesToday).toInt()
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Hola, $userName 👋",
                color = FretText,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onLogout) {
                Text("Salir", color = FretMuted, fontSize = 13.sp)
            }
        }

        Text(
            text = when {
                minutesToday >= META_DIARIA_MIN -> "¡Objetivo de hoy completado! 🎉"
                minutesToday > 0f -> "Te faltan $faltan min para tu objetivo de hoy."
                else -> "Aún no practicas hoy. ¡Tu guitarra te espera!"
            },
            color = FretMuted,
            fontSize = 15.sp
        )

        Text(
            text = "Nivel actual: $nivelDisplay",
            color = FretGold,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun ContinuePracticeCard(
    lastExercise: String? = null,
    completedSessions: Int = 0,
    onSeguir: () -> Unit = {},
    onVerRutina: () -> Unit = {}
) {
    val gradient = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF1A2233),
            Color(0xFF0F1725)
        )
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
    ) {
        Column(
            modifier = Modifier
                .background(gradient)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = FretGold
                )
                Text(
                    text = if (completedSessions > 0) "Continuar práctica" else "Empieza a practicar",
                    color = FretText,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = when {
                    lastExercise != null -> "Tu última práctica fue de $lastExercise. Sigue con una sesión más para consolidar lo aprendido."
                    completedSessions > 0 -> "Ya llevas $completedSessions sesiones registradas. ¡Una más hoy!"
                    else -> "Todavía no registras ninguna sesión. Empieza tu primera práctica y Wilfredo evaluará tu ejecución."
                },
                color = FretMuted,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onSeguir,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FretGold,
                        contentColor = FretBlack
                    )
                ) {
                    Text(if (completedSessions > 0) "Seguir" else "Empezar")
                }

                OutlinedButton(onClick = onVerRutina) {
                    Text("Ver rutina")
                }
            }
        }
    }
}

@Composable
fun StatsRow(
    streak: Int = 0,
    accuracy: Int = 0,
    completed: Int = 0
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MiniStatCard(
            modifier = Modifier.weight(1f),
            title = "Racha",
            value = "$streak días",
            accent = Color(0xFFE94584),
            icon = {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color(0xFFE94584)
                )
            }
        )

        MiniStatCard(
            modifier = Modifier.weight(1f),
            title = "Precisión",
            value = "$accuracy%",
            accent = Color(0xFF9EF01A),
            icon = {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = Color(0xFF9EF01A)
                )
            }
        )

        MiniStatCard(
            modifier = Modifier.weight(1f),
            title = "Completadas",
            value = "$completed",
            accent = Color(0xFF5AC8FA),
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF5AC8FA)
                )
            }
        )
    }
}

@Composable
fun MiniStatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    accent: Color,
    icon: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = FretSurface),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            icon()

            Text(
                text = value,
                color = FretText,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )

            Text(
                text = title,
                color = FretMuted,
                fontSize = 12.sp
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(accent, RoundedCornerShape(50))
            )
        }
    }
}

@Composable
fun DailyGoalCard(minutesToday: Float = 0f) {
    val progress = (minutesToday / META_DIARIA_MIN).coerceIn(0f, 1f)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = FretSurface),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Objetivo del día",
                color = FretGold,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            Text(
                text = "Completa ${META_DIARIA_MIN.toInt()} minutos de práctica para mantener tu progreso.",
                color = FretText,
                fontSize = 15.sp,
                lineHeight = 20.sp
            )

            Text(
                text = "Progreso de hoy: ${minutesToday.toInt()} / ${META_DIARIA_MIN.toInt()} min",
                color = FretMuted,
                fontSize = 14.sp
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .background(Color(0xFF222831), RoundedCornerShape(50))
            ) {
                if (progress > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .height(10.dp)
                            .background(FretGold, RoundedCornerShape(50))
                    )
                }
            }
        }
    }
}

@Composable
fun NextExerciseCard(
    nextExercise: String? = null,
    advice: String = "",
    onPracticar: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = FretSurface),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Siguiente ejercicio recomendado",
                color = FretGold,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            if (nextExercise != null) {
                Text(
                    text = "🎯 $nextExercise",
                    color = FretText,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
                if (advice.isNotBlank()) {
                    Text(
                        text = advice,
                        color = FretMuted,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
                Button(
                    onClick = onPracticar,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FretGold,
                        contentColor = FretBlack
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Practicar ahora", fontWeight = FontWeight.Bold)
                }
            } else {
                Text(
                    text = "Wilfredo está preparando tu recomendación...",
                    color = FretMuted,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun WilfredoTipCard(tip: String = "", loading: Boolean = false) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF101722)),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Consejo de Wilfredo",
                color = FretGold,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            Text(
                text = when {
                    tip.isNotBlank() -> tip
                    loading -> "Wilfredo está preparando tu consejo de hoy... 🎸"
                    else -> "Practica un poco cada día: la constancia vale más que las sesiones largas. 🎸"
                },
                color = FretText,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun RoutineDialog(
    exercises: List<String>,
    duration: String,
    advice: String,
    onDismiss: () -> Unit,
    onPracticar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FretSurface,
        title = {
            Text(
                text = "Tu rutina de hoy",
                color = FretGold,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (exercises.isEmpty()) {
                    Text(
                        text = "Wilfredo está preparando tu rutina...",
                        color = FretMuted
                    )
                } else {
                    exercises.forEachIndexed { i, ej ->
                        Text(
                            text = "${i + 1}. $ej",
                            color = FretText,
                            fontSize = 15.sp
                        )
                    }
                    if (duration.isNotBlank()) {
                        Text(
                            text = "⏱️ Duración sugerida: $duration",
                            color = FretMuted,
                            fontSize = 13.sp
                        )
                    }
                    if (advice.isNotBlank()) {
                        Text(
                            text = "💡 $advice",
                            color = FretMuted,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onPracticar,
                colors = ButtonDefaults.buttonColors(
                    containerColor = FretGold,
                    contentColor = FretBlack
                )
            ) {
                Text("Practicar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", color = FretMuted)
            }
        }
    )
}
