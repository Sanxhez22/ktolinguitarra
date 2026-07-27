package com.example.prueba.ui.components.practica

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.prueba.data.model.AcordeForma
import com.example.prueba.data.model.CatalogoAcordes
import com.example.prueba.ui.components.DiagramaAcorde
import com.example.prueba.ui.theme.FretGold
import com.example.prueba.ui.theme.FretMuted
import com.example.prueba.ui.theme.FretText
import com.example.prueba.viewmodel.LiveState
import kotlinx.coroutines.delay

private const val MS_POR_DEDO_ANIM = 450L
private const val MS_ACORDE_ARMADO = 1500L

/**
 * CHORD_CHANGE — guía animada del cambio: cada acorde de la secuencia se
 * arma dedo a dedo, se sostiene un instante y pasa al siguiente en bucle,
 * mostrando exactamente cómo se mueven los dedos entre formas.
 */
@Composable
fun GuiaCambioAcorde(state: LiveState) {
    val formas = remember(state.guiaAcordes) {
        state.guiaAcordes.mapNotNull { CatalogoAcordes.buscar(it) }
    }
    if (formas.isEmpty()) return

    var idx by remember(formas) { mutableIntStateOf(0) }
    var pasosVisibles by remember(formas) { mutableIntStateOf(0) }

    LaunchedEffect(formas) {
        while (true) {
            val forma = formas[idx % formas.size]
            pasosVisibles = 0
            for (p in 1..forma.totalPasosColocacion) {
                delay(MS_POR_DEDO_ANIM)
                pasosVisibles = p
            }
            delay(MS_ACORDE_ARMADO)
            idx = (idx + 1) % formas.size
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Cabecera "Am → C" con el acorde activo encendido.
        Row {
            formas.forEachIndexed { i, forma ->
                if (i > 0) Text("  →  ", color = FretMuted, fontSize = 18.sp)
                Text(
                    text = forma.nombre,
                    color = if (i == idx % formas.size) FretGold else FretMuted,
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Crossfade(targetState = idx % formas.size, label = "cambioAcorde") { i ->
            DiagramaAcorde(
                forma = formas[i],
                modifier = Modifier.width(190.dp),
                pasosVisibles = if (i == idx % formas.size) pasosVisibles else Int.MAX_VALUE,
                pasoResaltado = (pasosVisibles - 1)
                    .takeIf { i == idx % formas.size && it in 0 until formas[i].totalPasosColocacion }
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Mira cómo se coloca cada dedo y practica el movimiento " +
                "en el aire. El truco: mover todos los dedos JUNTOS.",
            color = FretText,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * CHORD_CHANGE en vivo: todos los acordes visibles; el objetivo ACTUAL
 * (el que el motor espera reconocer ahora) se agranda con borde dorado y
 * avanza solo cuando el acorde correcto suena de verdad.
 */
@Composable
fun PasoCambioAcorde(state: LiveState) {
    val formas = remember(state.guiaAcordes) {
        state.guiaAcordes.mapNotNull { CatalogoAcordes.buscar(it) }
    }
    if (formas.isEmpty()) return

    // El acorde activo lo dicta el motor (objetivoIdx rota por la secuencia).
    val activo = if (formas.isNotEmpty()) state.objetivoIdx % formas.size else 0

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Ahora toca:", color = FretMuted, fontSize = 13.sp)
        Text(
            text = state.objetivoActual ?: formas[activo].nombre,
            color = when (state.feedback) {
                com.example.prueba.viewmodel.FeedbackVivo.ACIERTO -> VerdeOk
                com.example.prueba.viewmodel.FeedbackVivo.FALLO -> RosaError
                else -> FretGold
            },
            fontWeight = FontWeight.Black,
            fontSize = 30.sp
        )
        Spacer(Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            formas.forEachIndexed { i, forma ->
                val esActivo = i == activo
                DiagramaAcorde(
                    forma = forma,
                    modifier = Modifier
                        .width(if (formas.size <= 2) 108.dp else 92.dp)
                        .scale(if (esActivo) 1.08f else 0.94f)
                        .border(
                            width = if (esActivo) 2.dp else 0.dp,
                            color = if (esActivo) FretGold else Color.Transparent,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(4.dp),
                    compacto = true
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Cambio ${state.aciertosPaso} de ${state.esperadosPaso} · " +
                (state.acordeDetectado?.let { "suena: $it" } ?: "escuchando…"),
            color = when (state.feedback) {
                com.example.prueba.viewmodel.FeedbackVivo.ACIERTO -> VerdeOk
                com.example.prueba.viewmodel.FeedbackVivo.FALLO -> RosaError
                else -> FretMuted
            },
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
    }
}
