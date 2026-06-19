package com.example.prueba.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.prueba.api.SongsterrMeta
import com.example.prueba.api.SongsterrTrack
import com.example.prueba.ui.theme.*
import com.example.prueba.viewmodel.SongDetailViewModel
import com.example.prueba.viewmodel.UiState

private val DETALLE_NOTE_NAMES =
    listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

private fun midiToNote(midi: Int): String =
    DETALLE_NOTE_NAMES[((midi % 12) + 12) % 12] + (midi / 12 - 1)

/** Afinación legible (de grave a agudo). La API la entrega de agudo a grave. */
private fun tuningToText(tuning: List<Int>?): String? =
    tuning?.takeIf { it.isNotEmpty() }?.reversed()?.joinToString(" ") { midiToNote(it) }

private fun dificultadDetalle(nivel: Int?): String? = when (nivel) {
    0 -> "Fácil"
    1 -> "Intermedio"
    2, 3 -> "Avanzado"
    else -> null
}

@Composable
fun SongDetailScreen(
    songId: Long,
    onBack: () -> Unit,
    viewModel: SongDetailViewModel = viewModel()
) {
    val state by viewModel.detailState.collectAsState()
    LaunchedEffect(songId) { viewModel.loadSong(songId) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FretBlack)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Barra superior con volver
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = FretText
                )
            }
            Text(
                text = "Detalle de canción",
                color = FretMuted,
                fontSize = 14.sp
            )
        }

        when (val s = state) {
            UiState.Idle, UiState.Loading -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(40.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = FretGold)
            }
            is UiState.Error -> Text(
                text = "⚠️ ${s.message}",
                color = FretMuted,
                fontSize = 14.sp
            )
            is UiState.Success -> SongDetailContent(meta = s.data, songId = songId)
        }
    }
}

@Composable
private fun SongDetailContent(meta: SongsterrMeta, songId: Long) {
    val context = LocalContext.current
    val tracks = meta.tracks.filter { it.isEmpty != true }
    var selected by remember(meta.songId) {
        mutableIntStateOf(meta.defaultTrack.coerceIn(0, (tracks.size - 1).coerceAtLeast(0)))
    }
    val track = tracks.getOrNull(selected)

    // Encabezado
    Text(
        text = meta.title,
        color = FretText,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp
    )
    Text(
        text = meta.artist,
        color = FretGold,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold
    )

    // Selector de instrumento / pista
    if (tracks.isNotEmpty()) {
        Text("Pistas disponibles", color = FretMuted, fontSize = 12.sp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tracks.forEachIndexed { i, t ->
                val isSel = i == selected
                Surface(
                    color = if (isSel) FretGold else FretSurface,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.clickable { selected = i }
                ) {
                    Text(
                        text = nombrePista(t),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        color = if (isSel) FretBlack else FretText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

    // Información musical de la pista seleccionada
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = FretSurface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            InfoRow("Instrumento", track?.instrument ?: "—")
            InfoRow("Afinación", tuningToText(track?.tuning) ?: "No disponible")
            dificultadDetalle(track?.difficulty)?.let { InfoRow("Dificultad", it) }
            // BPM y tonalidad: la API de Songsterr no los expone (se omiten si no existen).
            InfoRow("Acordes", if (meta.hasChords) "Disponibles en Songsterr" else "No disponibles")
        }
    }

    // Descripción del aporte (si existe)
    if (!meta.description.isNullOrBlank()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF101722)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Notas del autor", color = FretGold, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(meta.description.trim(), color = FretText, fontSize = 13.sp, lineHeight = 19.sp)
            }
        }
    }

    // Tags
    if (!meta.tags.isNullOrEmpty()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            meta.tags.take(10).forEach { tag ->
                Surface(color = Color(0xFF1A1F28), shape = RoundedCornerShape(50)) {
                    Text(
                        text = tag,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = FretMuted,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }

    // Métricas + autor
    val metricas = buildList {
        meta.views?.let { add("👁 ${formatoMiles(it)} vistas") }
        meta.favoritesCount?.let { add("★ ${formatoMiles(it)} favoritos") }
        meta.author?.name?.takeIf { it.isNotBlank() }?.let { add("✍ ${it.trim()}") }
    }
    if (metricas.isNotEmpty()) {
        Text(
            text = metricas.joinToString("   ·   "),
            color = FretMuted,
            fontSize = 12.sp
        )
    }

    // Abrir tablatura / acordes reales en Songsterr
    Button(
        onClick = {
            val url = "https://www.songsterr.com/a/wa/song?id=$songId"
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = FretGold, contentColor = FretBlack),
        shape = RoundedCornerShape(16.dp)
    ) {
        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Abrir tablatura en Songsterr")
    }

    Spacer(Modifier.height(20.dp))
}

@Composable
private fun InfoRow(etiqueta: String, valor: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(etiqueta, color = FretMuted, fontSize = 14.sp)
        Text(
            valor,
            color = FretText,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

private fun nombrePista(t: SongsterrTrack): String = when {
    t.isVocalTrack == true -> "Voz"
    else -> t.instrument
}

private fun formatoMiles(n: Int): String = when {
    n >= 1_000_000 -> "%.1fM".format(n / 1_000_000.0)
    n >= 1_000 -> "%.1fk".format(n / 1_000.0)
    else -> n.toString()
}
