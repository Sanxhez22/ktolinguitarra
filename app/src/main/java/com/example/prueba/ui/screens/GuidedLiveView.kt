package com.example.prueba.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.prueba.api.EjercicioDto
import com.example.prueba.data.model.CatalogoAcordes
import com.example.prueba.ui.components.DiagramaAcorde
import com.example.prueba.ui.components.practica.GuiaArpegio
import com.example.prueba.ui.components.practica.GuiaCambioAcorde
import com.example.prueba.ui.components.practica.GuiaCuerda
import com.example.prueba.ui.components.practica.GuiaCustom
import com.example.prueba.ui.components.practica.GuiaEscala
import com.example.prueba.ui.components.practica.GuiaFragmentoCancion
import com.example.prueba.ui.components.practica.GuiaMelodia
import com.example.prueba.ui.components.practica.GuiaNota
import com.example.prueba.ui.components.practica.GuiaRitmo
import com.example.prueba.ui.components.practica.GuiaSecuencia
import com.example.prueba.ui.components.practica.PasoArpegio
import com.example.prueba.ui.components.practica.PasoCambioAcorde
import com.example.prueba.ui.components.practica.PasoCuerda
import com.example.prueba.ui.components.practica.PasoCustom
import com.example.prueba.ui.components.practica.PasoEscala
import com.example.prueba.ui.components.practica.PasoFragmentoCancion
import com.example.prueba.ui.components.practica.PasoMelodia
import com.example.prueba.ui.components.practica.PasoNota
import com.example.prueba.ui.components.practica.PasoRitmo
import com.example.prueba.ui.components.practica.PasoSecuencia
import com.example.prueba.ui.theme.FretBlack
import com.example.prueba.ui.theme.FretGold
import com.example.prueba.ui.theme.FretMuted
import com.example.prueba.ui.theme.FretSurface
import com.example.prueba.ui.theme.FretText
import com.example.prueba.viewmodel.FaseVivo
import com.example.prueba.viewmodel.FeedbackVivo
import com.example.prueba.viewmodel.GuidedPracticeViewModel
import com.example.prueba.viewmodel.ResultadoVivo

/**
 * Práctica guiada EN VIVO (estilo Yousician): cuenta regresiva,
 * instrucciones dinámicas, detección continua, feedback inmediato,
 * barra de progreso, racha, timer y puntuación en tiempo real.
 */
@Composable
fun GuidedLiveView(
    liveViewModel: GuidedPracticeViewModel,
    ejercicio: EjercicioDto,
    onFinalizado: (ResultadoVivo) -> Unit,
    onCancelar: () -> Unit
) {
    val state by liveViewModel.state.collectAsState()
    val resultado by liveViewModel.resultado.collectAsState()

    LaunchedEffect(ejercicio.id) { liveViewModel.iniciar(ejercicio) }
    LaunchedEffect(resultado) { resultado?.let { onFinalizado(it) } }

    // Si la vista sale de composición con la sesión a medias (cambio de
    // pestaña del bottom bar, navegación), hay que soltar el micrófono:
    // con la entrada del nav guardada el ViewModel sigue vivo y sin esto
    // la captura de audio quedaba corriendo en segundo plano.
    DisposableEffect(Unit) {
        onDispose {
            if (liveViewModel.resultado.value == null) liveViewModel.cancelar()
        }
    }

    when (state.fase) {
        FaseVivo.PREPARANDO, FaseVivo.CUENTA -> CuentaRegresivaView(state.cuenta, state.fase)

        FaseVivo.GUIA -> GuiaAcordeView(
            state = state,
            onSaltar = { liveViewModel.saltarGuia() },
            onCancelar = {
                liveViewModel.cancelar()
                onCancelar()
            }
        )

        FaseVivo.ERROR -> Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("😕", fontSize = 44.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                text = state.error ?: "No se pudo iniciar la sesión en vivo",
                color = FretText,
                fontSize = 15.sp,
                textAlign = TextAlign.Center
            )
            TextButton(onClick = onCancelar) { Text("Volver", color = FretGold) }
        }

        FaseVivo.FINALIZADO -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("🎉 ¡Sesión completada!", color = FretGold, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }

        FaseVivo.PASO, FaseVivo.TRANSICION -> SesionEnVivoView(
            state = state,
            enTransicion = state.fase == FaseVivo.TRANSICION,
            onCancelar = {
                liveViewModel.cancelar()
                onCancelar()
            }
        )
    }
}

/**
 * Guía visual del acorde (antes de la detección): nombre, diagrama que se
 * arma dedo a dedo con su instrucción, y unos segundos para acomodar la
 * mano. La detección arranca sola al terminar; "Empezar ya" la adelanta.
 */
@Composable
private fun GuiaAcordeView(
    state: com.example.prueba.viewmodel.LiveState,
    onSaltar: () -> Unit,
    onCancelar: () -> Unit
) {
    val formas = remember(state.guiaAcordes) {
        state.guiaAcordes.mapNotNull { CatalogoAcordes.buscar(it) }
    }
    if (formas.isEmpty()) {
        // Pasos sin forma de acorde: guía previa genérica con la explicación
        // del paso (las vistas específicas por tipo la enriquecen).
        GuiaGenericaView(state, onSaltar, onCancelar)
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Paso ${state.pasoIdx + 1} de ${state.totalPasos} · ${state.titulo}",
            color = FretMuted,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = if (formas.size == 1) "Aprende el acorde" else "Repasa los acordes",
            color = FretText,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
        Spacer(Modifier.height(12.dp))

        if (formas.size == 1) {
            val forma = formas.first()
            val resaltado = (state.guiaPasoIdx - 1)
                .takeIf { it in 0 until forma.totalPasosColocacion }
            DiagramaAcorde(
                forma = forma,
                modifier = Modifier.width(210.dp),
                pasosVisibles = state.guiaPasoIdx,
                pasoResaltado = resaltado
            )
        } else {
            // Cambio de acorde: animación dedo a dedo entre las formas.
            GuiaCambioAcorde(state)
        }

        if (formas.size == 1) {
            Spacer(Modifier.height(14.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = FretSurface),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(
                    text = state.guiaTexto.ifEmpty { "Observa el diagrama…" },
                    color = FretText,
                    fontSize = 15.sp,
                    lineHeight = 21.sp,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            Spacer(Modifier.height(14.dp))
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = "🎙 La detección empezará automáticamente",
            color = FretMuted,
            fontSize = 12.sp
        )
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onCancelar) {
                Text("Cancelar", color = FretMuted, fontSize = 13.sp)
            }
            TextButton(onClick = onSaltar) {
                Text("Ya lo sé, empezar ▶", color = FretGold, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/** Qué aprende el usuario con cada tipo de paso (para la guía previa). */
private fun queAprenderas(tipo: String): String = when (tipo) {
    "NOTE" -> "Aprenderás a ubicar y tocar una nota exacta en el diapasón."
    "STRING" -> "Aprenderás a reconocer y tocar la cuerda correcta."
    "SEQUENCE" -> "Aprenderás a encadenar notas en orden, con dedos precisos."
    "SCALE" -> "Aprenderás el patrón de una escala, nota por nota."
    "ARPEGGIO" -> "Aprenderás a tocar las notas de un acorde una por una."
    "MELODY" -> "Aprenderás a tocar una melodía siguiendo el diapasón."
    "RHYTHM" -> "Aprenderás a mantener un patrón de rasgueo al tempo."
    "SONG_FRAGMENT" -> "Tocarás un fragmento de canción de principio a fin."
    else -> "Sigue la consigna del paso; la app te escucha y te da feedback."
}

/**
 * Guía previa de pasos no-acorde: nombre del ejercicio, qué se aprende,
 * representación gráfica animada del tipo y aviso de inicio automático.
 */
@Composable
private fun GuiaGenericaView(
    state: com.example.prueba.viewmodel.LiveState,
    onSaltar: () -> Unit,
    onCancelar: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Paso ${state.pasoIdx + 1} de ${state.totalPasos}",
            color = FretMuted,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = state.titulo,
            color = FretText,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = queAprenderas(state.tipo),
            color = FretGold,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))

        // Representación gráfica específica del tipo de paso.
        when (state.tipo) {
            "NOTE" -> GuiaNota(state)
            "STRING" -> GuiaCuerda(state)
            "SEQUENCE" -> GuiaSecuencia(state)
            "SCALE" -> GuiaEscala(state)
            "ARPEGGIO" -> GuiaArpegio(state)
            "MELODY" -> GuiaMelodia(state)
            "RHYTHM" -> GuiaRitmo(state)
            "SONG_FRAGMENT" -> GuiaFragmentoCancion(state)
            else -> GuiaCustom(state)
        }

        Spacer(Modifier.height(12.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = FretSurface),
            shape = RoundedCornerShape(18.dp)
        ) {
            Text(
                text = state.instruccion,
                color = FretText,
                fontSize = 15.sp,
                lineHeight = 21.sp,
                modifier = Modifier.padding(16.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "🎙 La detección empezará automáticamente",
            color = FretMuted,
            fontSize = 12.sp
        )
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onCancelar) {
                Text("Cancelar", color = FretMuted, fontSize = 13.sp)
            }
            TextButton(onClick = onSaltar) {
                Text("Ya lo sé, empezar ▶", color = FretGold, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun CuentaRegresivaView(cuenta: Int, fase: FaseVivo) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Prepárate...", color = FretMuted, fontSize = 16.sp)
        Spacer(Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(FretGold.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (fase == FaseVivo.CUENTA) "$cuenta" else "…",
                color = FretGold,
                fontWeight = FontWeight.Black,
                fontSize = 64.sp
            )
        }
        Spacer(Modifier.height(16.dp))
        Text("Toma tu guitarra 🎸", color = FretText, fontSize = 15.sp)
    }
}

@Composable
private fun SesionEnVivoView(
    state: com.example.prueba.viewmodel.LiveState,
    enTransicion: Boolean,
    onCancelar: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Progreso global + paso actual
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Paso ${state.pasoIdx + 1} de ${state.totalPasos}",
                color = FretMuted,
                fontSize = 13.sp
            )
            Text(
                text = "⏱ ${state.segundosRestantes}s",
                color = if (state.segundosRestantes <= 5) Color(0xFFE94584) else FretMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        LinearProgressIndicator(
            progress = { state.progresoGlobal },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = FretGold,
            trackColor = FretSurface
        )

        // Instrucción dinámica
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = FretSurface),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(state.titulo, color = FretText, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text(state.instruccion, color = FretMuted, fontSize = 13.sp, lineHeight = 18.sp)
            }
        }

        if (enTransicion) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (state.feedback == FeedbackVivo.ACIERTO) "✅ ¡Paso completado!" else "⏭ Siguiente paso...",
                    color = if (state.feedback == FeedbackVivo.ACIERTO) Color(0xFF4ADE80) else FretText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
            }
        } else {
            // Zona central: objetivo + feedback inmediato
            val feedbackColor by animateColorAsState(
                targetValue = when (state.feedback) {
                    FeedbackVivo.ACIERTO -> Color(0xFF1E3A29)
                    FeedbackVivo.FALLO -> Color(0xFF3A1E24)
                    else -> FretSurface
                },
                label = "feedbackBg"
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(feedbackColor, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 10.dp, horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Representación en vivo específica de cada tipo de paso.
                    // CHORD conserva su experiencia original.
                    when {
                        state.tipo == "NOTE" -> PasoNota(state)
                        state.tipo == "STRING" -> PasoCuerda(state)
                        state.tipo == "SEQUENCE" -> PasoSecuencia(state)
                        state.tipo == "SCALE" -> PasoEscala(state)
                        state.tipo == "ARPEGGIO" -> PasoArpegio(state)
                        state.tipo == "MELODY" -> PasoMelodia(state)
                        state.tipo == "RHYTHM" -> PasoRitmo(state)
                        state.tipo == "SONG_FRAGMENT" -> PasoFragmentoCancion(state)
                        state.tipo == "CHORD_CHANGE" && state.guiaAcordes.isNotEmpty() ->
                            PasoCambioAcorde(state)

                        state.tipo == "CHORD" || state.tipo == "CHORD_CHANGE" -> {
                            // Recordatorio visual del acorde mientras se detecta.
                            val formasMini = remember(state.guiaAcordes) {
                                state.guiaAcordes.mapNotNull { CatalogoAcordes.buscar(it) }
                            }
                            if (formasMini.isNotEmpty()) {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    formasMini.take(3).forEach { forma ->
                                        DiagramaAcorde(
                                            forma = forma,
                                            modifier = Modifier.width(if (formasMini.size == 1) 110.dp else 84.dp),
                                            compacto = true
                                        )
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                            }
                            Text(
                                text = state.objetivoActual?.let { "🎸 Rasguea $it" } ?: "🎸",
                                color = FretMuted,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "${state.aciertosPaso} / ${state.esperadosPaso}",
                                color = if (state.feedback == FeedbackVivo.ACIERTO) Color(0xFF4ADE80) else FretGold,
                                fontWeight = FontWeight.Black,
                                fontSize = 56.sp
                            )
                            Text(
                                text = when {
                                    state.acordeDetectado == null -> "rasgueos correctos · escuchando…"
                                    state.feedback == FeedbackVivo.FALLO ->
                                        "suena ${state.acordeDetectado}: revisa los dedos"
                                    else -> "rasgueos correctos · suena: ${state.acordeDetectado}"
                                },
                                color = when (state.feedback) {
                                    FeedbackVivo.ACIERTO -> Color(0xFF4ADE80)
                                    FeedbackVivo.FALLO -> Color(0xFFE94584)
                                    else -> FretMuted
                                },
                                fontSize = 13.sp
                            )
                        }

                        state.porPitch -> {
                            // Tipo por pitch desconocido: objetivo en grande.
                            Text("Toca:", color = FretMuted, fontSize = 14.sp)
                            Text(
                                text = state.objetivoActual ?: "—",
                                color = when (state.feedback) {
                                    FeedbackVivo.ACIERTO -> Color(0xFF4ADE80)
                                    FeedbackVivo.FALLO -> Color(0xFFE94584)
                                    else -> FretGold
                                },
                                fontWeight = FontWeight.Black,
                                fontSize = 72.sp
                            )
                            Text(
                                text = state.notaDetectada?.let { "Detectado: $it" } ?: "Escuchando...",
                                color = FretMuted,
                                fontSize = 13.sp
                            )
                        }

                        else -> PasoCustom(state)
                    }
                    if (state.racha >= 3) {
                        Spacer(Modifier.height(8.dp))
                        Text("🔥 Racha x${state.racha}", color = Color(0xFFFB923C), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }

        // Progreso del paso + puntuación viva
        LinearProgressIndicator(
            progress = { state.progresoPaso },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = Color(0xFF4ADE80),
            trackColor = FretSurface
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Puntuación: ${state.puntuacionViva}",
                color = FretGold,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            TextButton(onClick = onCancelar) {
                Text("Cancelar", color = FretMuted, fontSize = 13.sp)
            }
        }
    }
}
