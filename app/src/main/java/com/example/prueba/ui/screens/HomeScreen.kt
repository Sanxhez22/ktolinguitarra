package com.example.prueba.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.prueba.ui.theme.FretBlack
import com.example.prueba.ui.theme.FretGold
import com.example.prueba.ui.theme.FretMuted
import com.example.prueba.ui.theme.FretSurface
import com.example.prueba.ui.theme.FretText
import com.example.prueba.viewmodel.HomeViewModel
import com.example.prueba.viewmodel.UiState

@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel = viewModel(),
    onNavigate: (String) -> Unit = {}
) {
    val state by homeViewModel.homeState.collectAsState()
    LaunchedEffect(Unit) { homeViewModel.loadHomeData() }
    val data = (state as? UiState.Success)?.data

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FretBlack)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        HomeHeader()
        // Rutas (coinciden con Dest en Navigation.kt).
        ContinuePracticeCard(
            onSeguir = { onNavigate("practice") },
            onVerRutina = { onNavigate("progress") }
        )
        StatsRow()
        DailyGoalCard()
        WilfredoTipCard(
            tip = data?.wilfredoTip ?: "",
            loading = data?.isTipLoading ?: (state is UiState.Loading)
        )
        SuggestedSongsSection(onVerCanciones = { onNavigate("search") })
        Spacer(modifier = Modifier.height(90.dp))
    }
}

@Composable
fun HomeHeader() {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "Hola, José Luis 👋",
            color = FretText,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "FretMind detecta que hoy puedes subir de nivel.",
            color = FretMuted,
            fontSize = 15.sp
        )

        Text(
            text = "Nivel IA actual: Intermedio",
            color = FretGold,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun ContinuePracticeCard(
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
                    text = "Continuar práctica",
                    color = FretText,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "Tu última sesión fue en 'Precisión en notas - Nivel 3'. Vas muy bien, sigue con 10 minutos más.",
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
                    Text("Seguir")
                }

                OutlinedButton(onClick = onVerRutina) {
                    Text("Ver rutina")
                }
            }
        }
    }
}

@Composable
fun StatsRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MiniStatCard(
            modifier = Modifier.weight(1f),
            title = "Racha",
            value = "7 días",
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
            value = "87%",
            accent = Color(0xFF9EF01A),
            icon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = Color(0xFF9EF01A)
                )
            }
        )

        MiniStatCard(
            modifier = Modifier.weight(1f),
            title = "Completadas",
            value = "12",
            accent = Color(0xFF5AC8FA),
            icon = {
                Icon(
                    imageVector = Icons.Default.Person,
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
fun DailyGoalCard() {
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
                text = "Completa 15 minutos de práctica y mejora tu precisión en notas al 90%.",
                color = FretText,
                fontSize = 15.sp,
                lineHeight = 20.sp
            )

            Text(
                text = "Progreso de hoy: 9 / 15 min",
                color = FretMuted,
                fontSize = 14.sp
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .background(Color(0xFF222831), RoundedCornerShape(50))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.60f)
                        .height(10.dp)
                        .background(FretGold, RoundedCornerShape(50))
                )
            }
        }
    }
}

@Composable
fun WilfredoTipCard(tip: String = "", loading: Boolean = false) {
    val fallback = "Hoy detecté que tus mejores resultados salen cuando tocas despacio primero. Empieza a 70% de velocidad y luego sube."
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
                    else -> fallback
                },
                color = FretText,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun SuggestedSongsSection(onVerCanciones: () -> Unit = {}) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Canciones sugeridas",
            color = FretText,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )

        SuggestedSongCard(
            title = "Do I Wanna Know?",
            artist = "Arctic Monkeys",
            difficulty = "Intermedio",
            onClick = onVerCanciones
        )

        SuggestedSongCard(
            title = "Come As You Are",
            artist = "Nirvana",
            difficulty = "Fácil",
            onClick = onVerCanciones
        )

        SuggestedSongCard(
            title = "505",
            artist = "Arctic Monkeys",
            difficulty = "Intermedio",
            onClick = onVerCanciones
        )
    }
}

@Composable
fun SuggestedSongCard(
    title: String,
    artist: String,
    difficulty: String,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = FretSurface),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    color = FretText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )

                Text(
                    text = artist,
                    color = FretMuted,
                    fontSize = 14.sp
                )
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF101722)),
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    text = difficulty,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    color = FretGold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}