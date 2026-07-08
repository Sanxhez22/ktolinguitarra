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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.prueba.api.CancionDetalleDto
import com.example.prueba.api.PlanCancionDto
import com.example.prueba.ui.theme.*
import com.example.prueba.viewmodel.SongDetailViewModel
import com.example.prueba.viewmodel.UiState

@Composable
fun SongDetailScreen(
    songId: Long,
    onBack: () -> Unit,
    onPracticar: (ejercicioId: String, songId: Long) -> Unit = { _, _ -> },
    viewModel: SongDetailViewModel = viewModel()
) {
    val state by viewModel.detailState.collectAsState()
    val biblioteca by viewModel.bibliotecaState.collectAsState()
    val practicarState by viewModel.practicarState.collectAsState()
    LaunchedEffect(songId) { viewModel.loadSong(songId) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FretBlack)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
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
            is UiState.Success -> SongDetailContent(
                det = s.data,
                guardada = biblioteca?.guardada == true,
                favorita = biblioteca?.favorita == true,
                practicando = practicarState is UiState.Loading,
                onGuardar = { viewModel.toggleGuardar(songId) },
                onFavorito = { viewModel.toggleFavorito(songId) },
                onPracticarClick = { viewModel.practicar(songId) }
            )
        }

        Spacer(Modifier.height(20.dp))
    }

    // Plan de RIFF listo: diálogo con objetivos y botón para comenzar.
    (practicarState as? UiState.Success<PlanCancionDto>)?.let { s ->
        PlanCancionDialog(
            plan = s.data,
            onComenzar = {
                viewModel.resetPracticar()
                onPracticar(s.data.ejercicio.id, songId)
            },
            onDismiss = { viewModel.resetPracticar() }
        )
    }
    (practicarState as? UiState.Error)?.let { e ->
        LaunchedEffect(e) { /* el error se muestra inline abajo del botón */ }
    }
}

@Composable
private fun SongDetailContent(
    det: CancionDetalleDto,
    guardada: Boolean,
    favorita: Boolean,
    practicando: Boolean,
    onGuardar: () -> Unit,
    onFavorito: () -> Unit,
    onPracticarClick: () -> Unit
) {
    val context = LocalContext.current

    // ---------- Cabecera: portada + identidad ----------
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (det.portada != null) {
            AsyncImage(
                model = det.portada,
                contentDescription = "Portada de ${det.titulo}",
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(18.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            // Sin portada confiable: placeholder con inicial (omisión elegante).
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(FretSurface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = FretGold,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = det.titulo,
                color = FretText,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                lineHeight = 27.sp
            )
            Text(
                text = det.artista,
                color = FretGold,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            det.album?.let {
                Text(text = it, color = FretMuted, fontSize = 12.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                det.genero?.let { ChipInfo(it) }
                det.duracionSeg?.let { ChipInfo(formatoDuracion(it)) }
            }
        }
    }

    // ---------- Acciones: Practicar / Guardar / Favorito ----------
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = onPracticarClick,
            enabled = !practicando,
            modifier = Modifier
                .weight(1f)
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = FretGold,
                contentColor = FretBlack
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            if (practicando) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = FretBlack,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Practicar", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }

        FilledIconToggleButton(
            checked = guardada,
            onCheckedChange = { onGuardar() },
            colors = IconButtonDefaults.filledIconToggleButtonColors(
                containerColor = FretSurface,
                checkedContainerColor = FretGold.copy(alpha = 0.25f)
            )
        ) {
            Icon(
                imageVector = if (guardada) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                contentDescription = "Guardar",
                tint = if (guardada) FretGold else FretMuted
            )
        }

        FilledIconToggleButton(
            checked = favorita,
            onCheckedChange = { onFavorito() },
            colors = IconButtonDefaults.filledIconToggleButtonColors(
                containerColor = FretSurface,
                checkedContainerColor = Color(0xFFE94584).copy(alpha = 0.25f)
            )
        ) {
            Icon(
                imageVector = if (favorita) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = "Favorito",
                tint = if (favorita) Color(0xFFE94584) else FretMuted
            )
        }
    }

    // ---------- Ficha musical ----------
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = FretSurface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            det.dificultad?.let { InfoRow("Dificultad", it) }
            det.afinacion?.let { InfoRow("Afinación", it) }
            det.duracionSeg?.let { InfoRow("Duración", formatoDuracion(it)) }
            det.genero?.let { InfoRow("Género", it) }
            InfoRow("Acordes", if (det.tieneAcordes) "Disponibles en Songsterr" else "No disponibles")
            // Tonalidad, tempo y capo: sin fuente confiable todavía -> no se muestran.
        }
    }

    // ---------- Pistas ----------
    // Solo pistas de guitarra: la app está enfocada únicamente en guitarra,
    // así que Bass, voz y demás instrumentos no se muestran. El backend sigue
    // devolviendo todas las pistas; el filtro es solo de experiencia de usuario.
    val pistasGuitarra = remember(det.songId) {
        det.pistas.filter { p ->
            !p.esVoz &&
                p.instrumento.contains("guitar", ignoreCase = true) &&
                !p.instrumento.contains("bass", ignoreCase = true)
        }
    }
    if (pistasGuitarra.isNotEmpty()) {
        var pistaSel by remember(det.songId) {
            // Conserva la pista por defecto del backend si es de guitarra.
            val def = det.pistas.getOrNull(det.pistaDefault)
            mutableIntStateOf(pistasGuitarra.indexOf(def).coerceAtLeast(0))
        }
        Text("Pistas de la tablatura", color = FretMuted, fontSize = 12.sp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            pistasGuitarra.forEachIndexed { i, p ->
                val isSel = i == pistaSel
                Surface(
                    color = if (isSel) FretGold else FretSurface,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.clickable { pistaSel = i }
                ) {
                    Text(
                        text = p.instrumento,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        color = if (isSel) FretBlack else FretText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
        pistasGuitarra.getOrNull(pistaSel)?.let { p ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF101722)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    p.nombre?.let { Text(it, color = FretText, fontSize = 13.sp) }
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        p.afinacion?.let { Text("🎸 $it", color = FretMuted, fontSize = 12.sp) }
                        p.dificultad?.let { Text("📊 $it", color = FretMuted, fontSize = 12.sp) }
                    }
                }
            }
        }
    }

    // ---------- Notas del autor ----------
    det.descripcion?.let { desc ->
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF101722)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Notas del autor", color = FretGold, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(desc, color = FretText, fontSize = 13.sp, lineHeight = 19.sp)
            }
        }
    }

    // ---------- Tags ----------
    if (det.tags.isNotEmpty()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            det.tags.forEach { tag ->
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

    // ---------- Videos ----------
    if (det.videos.isNotEmpty()) {
        Text("Videos de la comunidad", color = FretMuted, fontSize = 12.sp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            det.videos.forEachIndexed { i, videoId ->
                Surface(
                    color = FretSurface,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.clickable {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=$videoId"))
                        )
                    }
                ) {
                    Text(
                        text = "▶ Video ${i + 1}",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        color = FretText,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }

    // ---------- Métricas + autor ----------
    val metricas = buildList {
        det.vistas?.let { add("👁 ${formatoMiles(it)} vistas") }
        det.favoritos?.let { add("★ ${formatoMiles(it)} favoritos") }
        det.autor?.takeIf { it.isNotBlank() }?.let { add("✍ ${it.trim()}") }
    }
    if (metricas.isNotEmpty()) {
        Text(
            text = metricas.joinToString("   ·   "),
            color = FretMuted,
            fontSize = 12.sp
        )
    }

    // ---------- Tablatura completa: deep-link (CDN firmado de Songsterr) ----------
    OutlinedButton(
        onClick = {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(det.enlaceTab)))
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = FretGold)
        Spacer(Modifier.width(8.dp))
        Text("Ver tablatura completa en Songsterr", color = FretGold)
    }
}

/** Diálogo con el plan de Wilfredo para la canción (botón Practicar). */
@Composable
private fun PlanCancionDialog(
    plan: PlanCancionDto,
    onComenzar: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FretSurface,
        title = {
            Text(
                text = "Plan de RIFF 🎸",
                color = FretGold,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "${plan.cancion.titulo} — ${plan.cancion.artista}",
                    color = FretText,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
                plan.objetivos.forEachIndexed { i, obj ->
                    Text(text = "${i + 1}. $obj", color = FretText, fontSize = 13.sp, lineHeight = 18.sp)
                }
                if (plan.consejo.isNotBlank()) {
                    Text(text = "💡 ${plan.consejo}", color = FretMuted, fontSize = 12.sp, lineHeight = 17.sp)
                }
                Text(
                    text = "⏱️ Duración sugerida: ${plan.duracionSugeridaMin} min",
                    color = FretMuted,
                    fontSize = 12.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onComenzar,
                colors = ButtonDefaults.buttonColors(containerColor = FretGold, contentColor = FretBlack)
            ) {
                Text("Comenzar práctica", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Ahora no", color = FretMuted)
            }
        }
    )
}

@Composable
private fun ChipInfo(texto: String) {
    Surface(color = Color(0xFF1A1F28), shape = RoundedCornerShape(50)) {
        Text(
            text = texto,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            color = FretGold,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
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

private fun formatoDuracion(seg: Int): String = "%d:%02d".format(seg / 60, seg % 60)

private fun formatoMiles(n: Int): String = when {
    n >= 1_000_000 -> "%.1fM".format(n / 1_000_000.0)
    n >= 1_000 -> "%.1fk".format(n / 1_000.0)
    else -> n.toString()
}
