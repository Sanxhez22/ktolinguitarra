package com.example.prueba.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.prueba.ui.theme.FretBlack
import com.example.prueba.ui.theme.FretGold
import com.example.prueba.ui.theme.FretMuted
import com.example.prueba.ui.theme.FretSurface
import com.example.prueba.ui.theme.FretText
import com.example.prueba.viewmodel.ProgressData
import com.example.prueba.viewmodel.ProgressViewModel
import com.example.prueba.viewmodel.UiState

/** Meta diaria de práctica en minutos (objetivo del día en Home/Progreso). */
const val META_DIARIA_MIN = 15f

@Composable
fun ProgressScreen(progressViewModel: ProgressViewModel = viewModel()) {
    val state by progressViewModel.progressState.collectAsState()
    LaunchedEffect(Unit) { progressViewModel.loadProgress() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FretBlack)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        when (val s = state) {
            is UiState.Success -> ProgressContent(s.data)

            is UiState.Error -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = FretSurface),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "No se pudo cargar tu progreso",
                            color = FretText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(text = s.message, color = FretMuted, fontSize = 14.sp)
                        Button(
                            onClick = { progressViewModel.loadProgress() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = FretGold,
                                contentColor = FretBlack
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Reintentar", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = FretGold)
                }
            }
        }

        Spacer(modifier = Modifier.height(90.dp))
    }
}

@Composable
private fun ProgressContent(data: ProgressData) {
    val dailyGoalProgress = (data.minutesToday / META_DIARIA_MIN).coerceIn(0f, 1f)
    val overallPct = ((data.precisionAvg + data.consistencyAvg) / 2f * 100).toInt()

    HeaderSection(
        userName = data.userName,
        aiLevel = data.aiLevel.replaceFirstChar { it.uppercase() }
    )

    if (data.sessions == 0) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = FretGold.copy(alpha = 0.12f)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Todavía no hay datos",
                    color = FretGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
                Text(
                    text = "Completa tu primera práctica y tu progreso real aparecerá aquí.",
                    color = FretText,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        }
    }

    RingsCard(
        precision = data.precisionAvg,
        consistency = data.consistencyAvg,
        dailyGoal = dailyGoalProgress,
        overallPct = overallPct,
        precisionHistory = data.precisionHistory,
        consistencyHistory = data.consistencyHistory,
        minutesToday = data.minutesToday
    )

    StatsGrid(
        sessions = data.sessions,
        minutesTotal = data.minutesTotal,
        streak = data.streak,
        exercisesCompleted = data.exercisesCompleted
    )

    AiSummaryCard(summary = data.wilfredoSummary)
}

@Composable
fun HeaderSection(
    userName: String,
    aiLevel: String
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "Tu progreso",
            style = MaterialTheme.typography.headlineMedium,
            color = FretText,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = userName,
            color = FretGold,
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold
        )

        Text(
            text = "Nivel: $aiLevel",
            color = FretMuted,
            fontSize = 16.sp
        )
    }
}

@Composable
fun RingsCard(
    precision: Float,
    consistency: Float,
    dailyGoal: Float,
    overallPct: Int,
    precisionHistory: List<Float>,
    consistencyHistory: List<Float>,
    minutesToday: Float
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = FretSurface),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = "Rendimiento general",
                color = FretText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            TripleRingProgress(
                outerProgress = precision,
                middleProgress = consistency,
                innerProgress = dailyGoal,
                centerPct = overallPct
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                RingLegendWithMiniChart(
                    title = "Precisión",
                    progress = precision,
                    color = Color(0xFFE94584),
                    history = precisionHistory
                )

                RingLegendWithMiniChart(
                    title = "Consistencia",
                    progress = consistency,
                    color = Color(0xFF9EF01A),
                    history = consistencyHistory
                )

                RingLegendWithMiniChart(
                    title = "Objetivo diario (${minutesToday.toInt()} / ${META_DIARIA_MIN.toInt()} min)",
                    progress = dailyGoal,
                    color = Color(0xFF5AC8FA),
                    history = emptyList()
                )
            }
        }
    }
}

@Composable
fun TripleRingProgress(
    outerProgress: Float,
    middleProgress: Float,
    innerProgress: Float,
    centerPct: Int = 0
) {
    val outerAnimated by animateFloatAsState(
        targetValue = outerProgress,
        animationSpec = tween(1000),
        label = "outerRing"
    )
    val middleAnimated by animateFloatAsState(
        targetValue = middleProgress,
        animationSpec = tween(1200),
        label = "middleRing"
    )
    val innerAnimated by animateFloatAsState(
        targetValue = innerProgress,
        animationSpec = tween(1400),
        label = "innerRing"
    )

    Box(
        modifier = Modifier.size(240.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeOuter = 24.dp.toPx()
            val strokeMiddle = 22.dp.toPx()
            val strokeInner = 20.dp.toPx()

            drawArc(
                color = Color(0xFF2A2A2A),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeOuter, cap = StrokeCap.Round),
                size = Size(size.width, size.height)
            )

            drawArc(
                brush = Brush.sweepGradient(
                    listOf(Color(0xFFFF5C93), Color(0xFFE94584), Color(0xFFFF5C93))
                ),
                startAngle = -90f,
                sweepAngle = 360f * outerAnimated,
                useCenter = false,
                style = Stroke(width = strokeOuter, cap = StrokeCap.Round),
                size = Size(size.width, size.height)
            )

            drawArc(
                color = Color(0xFF2A2A2A),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeMiddle, cap = StrokeCap.Round),
                topLeft = Offset(24f, 24f),
                size = Size(size.width - 48f, size.height - 48f)
            )

            drawArc(
                brush = Brush.sweepGradient(
                    listOf(Color(0xFFB7F65F), Color(0xFF9EF01A), Color(0xFFB7F65F))
                ),
                startAngle = -90f,
                sweepAngle = 360f * middleAnimated,
                useCenter = false,
                style = Stroke(width = strokeMiddle, cap = StrokeCap.Round),
                topLeft = Offset(24f, 24f),
                size = Size(size.width - 48f, size.height - 48f)
            )

            drawArc(
                color = Color(0xFF2A2A2A),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeInner, cap = StrokeCap.Round),
                topLeft = Offset(48f, 48f),
                size = Size(size.width - 96f, size.height - 96f)
            )

            drawArc(
                brush = Brush.sweepGradient(
                    listOf(Color(0xFF79D8FF), Color(0xFF5AC8FA), Color(0xFF79D8FF))
                ),
                startAngle = -90f,
                sweepAngle = 360f * innerAnimated,
                useCenter = false,
                style = Stroke(width = strokeInner, cap = StrokeCap.Round),
                topLeft = Offset(48f, 48f),
                size = Size(size.width - 96f, size.height - 96f)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Progreso calculado",
                color = FretMuted,
                fontSize = 13.sp
            )

            Text(
                text = "por Wilfredo",
                color = FretMuted,
                fontSize = 13.sp
            )

            Text(
                text = "$centerPct%",
                color = FretGold,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun RingLegendWithMiniChart(
    title: String,
    progress: Float,
    color: Color,
    history: List<Float>
) {
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(1200, easing = FastOutSlowInEasing)
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF101722)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(color, RoundedCornerShape(50))
                    )

                    Text(
                        text = title,
                        color = FretText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Text(
                    text = "${(progress * 100).toInt()}%",
                    color = color,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            // La gráfica solo se dibuja con historial real suficiente (≥2 sesiones).
            if (history.size >= 2) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(45.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val maxValue = history.maxOrNull() ?: 100f
                        val minValue = history.minOrNull() ?: 0f
                        val range = (maxValue - minValue).takeIf { it != 0f } ?: 1f
                        val spacingX = size.width / (history.size - 1).coerceAtLeast(1)

                        val points = history.mapIndexed { index, value ->
                            val x = spacingX * index
                            val normalized = (value - minValue) / range
                            val y = size.height - (normalized * size.height * animatedProgress.value)
                            Offset(x, y)
                        }

                        val path = Path()
                        points.forEachIndexed { index, point ->
                            if (index == 0) path.moveTo(point.x, point.y)
                            else path.lineTo(point.x, point.y)
                        }

                        drawPath(
                            path = path,
                            color = color,
                            style = Stroke(
                                width = 3.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        )

                        points.forEach { point ->
                            drawCircle(
                                color = color,
                                radius = 3.8.dp.toPx(),
                                center = point
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatsGrid(
    sessions: Int,
    minutesTotal: Float,
    streak: Int,
    exercisesCompleted: Int
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                title = "Sesiones completadas",
                value = "$sessions"
            )
            StatCard(
                modifier = Modifier.weight(1f),
                title = "Minutos de práctica",
                value = "${minutesTotal.toInt()}"
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                title = "Racha de días",
                value = "$streak"
            )
            StatCard(
                modifier = Modifier.weight(1f),
                title = "Ejercicios completados",
                value = "$exercisesCompleted"
            )
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = FretSurface),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                color = FretMuted,
                fontSize = 14.sp
            )
            Text(
                text = value,
                color = FretText,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun AiSummaryCard(summary: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = FretSurface),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Resumen de Wilfredo",
                color = FretGold,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            Text(
                text = summary,
                color = FretText,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}
