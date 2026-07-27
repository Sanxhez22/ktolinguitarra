package com.example.prueba.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.prueba.data.model.CatalogoAcordes
import com.example.prueba.ui.components.DiagramaAcorde
import com.example.prueba.ui.theme.*
import com.example.prueba.viewmodel.FaseCancion
import com.example.prueba.viewmodel.LineaCancionVivo
import com.example.prueba.viewmodel.SongPracticeState
import com.example.prueba.viewmodel.SongPracticeViewModel

private val VerdeOk = Color(0xFF4ADE80)
private val RosaError = Color(0xFFE94584)

/**
 * Práctica guiada de canción (estilo Ultimate Guitar / Yousician):
 * letra por secciones con el acorde de práctica encima de cada línea,
 * línea actual resaltada, desplazamiento automático al ritmo de la
 * canción, diagrama del acorde actual y el siguiente, y reconocimiento
 * del acorde tocado que marca cada línea como lograda.
 */
@Composable
fun SongPracticeScreen(
    songId: Long,
    onBack: () -> Unit,
    viewModel: SongPracticeViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(songId) { viewModel.cargar(songId) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FretBlack)
            .padding(16.dp)
    ) {
        when (state.fase) {
            FaseCancion.CARGANDO -> Centro { CircularProgressIndicator(color = FretGold) }

            FaseCancion.ERROR -> Centro {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("😕", fontSize = 40.sp)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = state.error ?: "No se pudo cargar la práctica",
                        color = FretText,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    TextButton(onClick = onBack) { Text("Volver", color = FretGold) }
                }
            }

            FaseCancion.LISTA -> PortadaCancionView(
                state = state,
                onComenzar = { viewModel.comenzar() },
                onBack = onBack
            )

            FaseCancion.CUENTA -> Centro {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Prepárate…", color = FretMuted, fontSize = 15.sp)
                    Spacer(Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .background(FretGold.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${state.cuenta}",
                            color = FretGold,
                            fontWeight = FontWeight.Black,
                            fontSize = 56.sp
                        )
                    }
                }
            }

            FaseCancion.TOCANDO, FaseCancion.PAUSA -> CancionEnVivoView(
                state = state,
                enPausa = state.fase == FaseCancion.PAUSA,
                onPausa = { viewModel.pausar() },
                onReanudar = { viewModel.reanudar() },
                onTerminar = { viewModel.terminar() },
                onSalir = {
                    viewModel.cancelar()
                    onBack()
                }
            )

            FaseCancion.FINALIZADA -> ResultadoCancionView(
                state = state,
                onRepetir = { viewModel.comenzar() },
                onVolver = onBack
            )
        }
    }
}

@Composable
private fun Centro(contenido: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { contenido() }
}

/** Portada previa: canción, consejo de RIFF, progresión y botón Comenzar. */
@Composable
private fun PortadaCancionView(
    state: SongPracticeState,
    onComenzar: () -> Unit,
    onBack: () -> Unit
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
    ) { granted ->
        hasMicPermission = granted
        if (granted) onComenzar()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = FretText)
            }
            Text("Práctica guiada", color = FretMuted, fontSize = 14.sp)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            if (state.portada != null) {
                AsyncImage(
                    model = state.portada,
                    contentDescription = null,
                    modifier = Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(FretSurface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.MusicNote, contentDescription = null, tint = FretGold)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(state.titulo, color = FretText, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text(state.artista, color = FretGold, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    text = if (state.tieneLetra) "Letra + acordes · scroll automático"
                    else "Sin letra disponible · práctica por progresión",
                    color = FretMuted,
                    fontSize = 12.sp
                )
            }
        }

        // Progresión de práctica con sus diagramas.
        Text("Acordes de la práctica", color = FretMuted, fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            state.progresion.forEach { nombre ->
                val forma = remember(nombre) { CatalogoAcordes.buscar(nombre) }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (forma != null) {
                        DiagramaAcorde(forma = forma, modifier = Modifier.width(76.dp), compacto = true)
                    } else {
                        Text(nombre, color = FretGold, fontWeight = FontWeight.Black, fontSize = 22.sp)
                    }
                }
            }
        }

        if (state.consejo.isNotBlank()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = FretGold.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "💡 ${state.consejo}",
                    modifier = Modifier.padding(12.dp),
                    color = FretText,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }
        if (state.notaTransparencia.isNotBlank()) {
            Text(state.notaTransparencia, color = FretMuted, fontSize = 11.sp, lineHeight = 15.sp)
        }

        Button(
            onClick = {
                if (hasMicPermission) onComenzar()
                else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = FretGold, contentColor = FretBlack),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Comenzar práctica", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

/** La práctica en vivo: acordes arriba, letra con scroll automático abajo. */
@Composable
private fun CancionEnVivoView(
    state: SongPracticeState,
    enPausa: Boolean,
    onPausa: () -> Unit,
    onReanudar: () -> Unit,
    onTerminar: () -> Unit,
    onSalir: () -> Unit
) {
    val listState = rememberLazyListState()

    // Scroll automático: la línea activa se mantiene visible (una arriba).
    LaunchedEffect(state.lineaActual) {
        if (state.lineaActual >= 0) {
            listState.animateScrollToItem((state.lineaActual - 1).coerceAtLeast(0))
        }
    }

    val lineaActual = state.lineas.getOrNull(state.lineaActual)
    val siguienteAcorde = remember(state.lineaActual, state.lineas) {
        state.lineas.drop((state.lineaActual + 1).coerceAtLeast(0))
            .firstOrNull { it.acorde != null && it.acorde != lineaActual?.acorde }?.acorde
    }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Cabecera: título + tiempo + logrados.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = state.titulo,
                color = FretText,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "⏱ %d:%02d".format(state.segundos / 60, state.segundos % 60),
                color = FretMuted,
                fontSize = 13.sp
            )
        }
        LinearProgressIndicator(
            progress = {
                val fin = state.lineas.lastOrNull()?.tSeg?.toFloat() ?: 1f
                (state.segundos / fin.coerceAtLeast(1f)).coerceIn(0f, 1f)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp),
            color = FretGold,
            trackColor = FretSurface
        )

        // Panel del acorde actual + siguiente + lo que suena.
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = FretSurface),
            shape = RoundedCornerShape(18.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val acordeActual = lineaActual?.acorde
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Ahora", color = FretMuted, fontSize = 11.sp)
                    val forma = acordeActual?.let { CatalogoAcordes.buscar(it) }
                    if (forma != null) {
                        DiagramaAcorde(forma = forma, modifier = Modifier.width(92.dp), compacto = true)
                    } else {
                        Text(
                            text = acordeActual ?: "—",
                            color = FretGold,
                            fontWeight = FontWeight.Black,
                            fontSize = 34.sp
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Sigue", color = FretMuted, fontSize = 11.sp)
                    val formaSig = siguienteAcorde?.let { CatalogoAcordes.buscar(it) }
                    if (formaSig != null) {
                        DiagramaAcorde(forma = formaSig, modifier = Modifier.width(64.dp), compacto = true)
                    } else {
                        Text(
                            text = siguienteAcorde ?: "—",
                            color = FretMuted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Logrados", color = FretMuted, fontSize = 11.sp)
                    Text(
                        text = "${state.aciertos}/${state.totalAcordes}",
                        color = VerdeOk,
                        fontWeight = FontWeight.Black,
                        fontSize = 24.sp
                    )
                    val esAcierto = state.lineaActual in state.lineasAcertadas
                    Text(
                        text = when {
                            esAcierto -> "✓ ¡bien!"
                            state.acordeDetectado != null -> "suena: ${state.acordeDetectado}"
                            else -> "escuchando…"
                        },
                        color = when {
                            esAcierto -> VerdeOk
                            state.acordeDetectado != null &&
                                lineaActual?.acorde != null &&
                                state.acordeDetectado != lineaActual.acorde -> RosaError
                            else -> FretMuted
                        },
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Letra con acordes encima, línea activa resaltada.
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            itemsIndexed(state.lineas) { i, linea ->
                LineaCancionItem(
                    linea = linea,
                    esActual = i == state.lineaActual,
                    acertada = i in state.lineasAcertadas,
                    perdida = i in state.lineasPerdidas
                )
            }
            item { Spacer(Modifier.height(60.dp)) }
        }

        // Controles.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onSalir) { Text("Salir", color = FretMuted, fontSize = 13.sp) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledIconButton(
                    onClick = { if (enPausa) onReanudar() else onPausa() },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = FretSurface, contentColor = FretGold
                    )
                ) {
                    Icon(
                        imageVector = if (enPausa) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = if (enPausa) "Reanudar" else "Pausa"
                    )
                }
                Button(
                    onClick = onTerminar,
                    colors = ButtonDefaults.buttonColors(containerColor = FretGold, contentColor = FretBlack),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Terminar", fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun LineaCancionItem(
    linea: LineaCancionVivo,
    esActual: Boolean,
    acertada: Boolean,
    perdida: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (esActual) FretGold.copy(alpha = 0.10f) else Color.Transparent,
                RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        if (linea.esInicioSeccion) {
            Text(
                text = linea.seccion.uppercase(),
                color = FretMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
            )
        }
        linea.acorde?.let { acorde ->
            Text(
                text = acorde + when {
                    acertada -> " ✓"
                    perdida -> " ·"
                    else -> ""
                },
                color = when {
                    acertada -> VerdeOk
                    esActual -> FretGold
                    perdida -> FretMuted
                    else -> FretGold.copy(alpha = 0.55f)
                },
                fontWeight = FontWeight.Black,
                fontSize = if (esActual) 17.sp else 14.sp
            )
        }
        if (linea.texto.isNotBlank()) {
            Text(
                text = linea.texto,
                color = if (esActual) FretText else FretMuted,
                fontSize = if (esActual) 16.sp else 14.sp,
                fontWeight = if (esActual) FontWeight.SemiBold else FontWeight.Normal,
                lineHeight = 21.sp
            )
        } else if (linea.acorde != null) {
            // Modo sin letra: compás de rasgueo.
            Text("𝄞 rasguea el compás", color = FretMuted, fontSize = 12.sp)
        }
    }
}

/** Resumen final: puntuación, estrellas y estado del registro. */
@Composable
private fun ResultadoCancionView(
    state: SongPracticeState,
    onRepetir: () -> Unit,
    onVolver: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🎤 ¡Práctica completada!", color = FretGold, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(Modifier.height(6.dp))
        Text("${state.titulo} — ${state.artista}", color = FretMuted, fontSize = 13.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            text = (1..3).joinToString("") { if (it <= state.estrellas) "★" else "☆" },
            color = FretGold,
            fontSize = 40.sp
        )
        Text(
            text = "${state.puntuacion.toInt()} / 100",
            color = FretText,
            fontWeight = FontWeight.Black,
            fontSize = 40.sp
        )
        Text(
            text = "${state.aciertos} de ${state.totalAcordes} acordes logrados",
            color = FretMuted,
            fontSize = 14.sp
        )
        Spacer(Modifier.height(10.dp))
        when (state.envioEstado) {
            "enviando" -> Text("Guardando tu sesión…", color = FretMuted, fontSize = 12.sp)
            "ok" -> Text("✓ Sesión registrada en tu progreso", color = VerdeOk, fontSize = 12.sp)
            "error" -> Text("No se pudo registrar la sesión", color = RosaError, fontSize = 12.sp)
        }
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onRepetir, shape = RoundedCornerShape(14.dp)) {
                Text("Repetir", color = FretGold)
            }
            Button(
                onClick = onVolver,
                colors = ButtonDefaults.buttonColors(containerColor = FretGold, contentColor = FretBlack),
                shape = RoundedCornerShape(14.dp)
            ) { Text("Volver", fontWeight = FontWeight.Bold) }
        }
    }
}
