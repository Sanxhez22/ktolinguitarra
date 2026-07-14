package com.example.prueba.ui.components.practica

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.prueba.ui.theme.FretBlack
import com.example.prueba.ui.theme.FretGold
import com.example.prueba.ui.theme.FretMuted
import com.example.prueba.ui.theme.FretSurface
import com.example.prueba.ui.theme.FretText
import com.example.prueba.viewmodel.FeedbackVivo
import com.example.prueba.viewmodel.LiveState

/**
 * Barra de ritmo: el patrón de rasgueo (↓ / ↑) como fichas y un pulso de
 * metrónomo visual que recorre el patrón al tempo del paso. Es la
 * representación característica de RHYTHM (guía y vivo).
 */
@Composable
private fun BarraRitmo(patron: List<String>, bpm: Int, grande: Boolean) {
    val msPorPulso = 60_000 / bpm.coerceAtLeast(20)
    val transicion = rememberInfiniteTransition(label = "ritmo")
    val fase by transicion.animateFloat(
        initialValue = 0f,
        targetValue = patron.size.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = msPorPulso * patron.size, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "faseRitmo"
    )
    val pulsoActual = fase.toInt().coerceIn(0, patron.size - 1)
    val dentroDelPulso = fase - fase.toInt()   // 0..1 dentro del pulso

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Metrónomo: late al inicio de cada pulso.
        val escala = 1f + (if (grande) 0.30f else 0.18f) * (1f - dentroDelPulso)
        Box(
            modifier = Modifier
                .size(if (grande) 64.dp else 44.dp)
                .scale(escala)
                .background(FretGold.copy(alpha = 0.18f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${pulsoActual + 1}",
                color = FretGold,
                fontWeight = FontWeight.Black,
                fontSize = if (grande) 26.sp else 18.sp
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            patron.forEachIndexed { i, golpe ->
                val activo = i == pulsoActual
                Box(
                    modifier = Modifier
                        .size(if (grande) 46.dp else 38.dp)
                        .scale(if (activo) 1.12f else 1f)
                        .background(
                            if (activo) FretGold else FretSurface,
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = golpe,
                        color = if (activo) FretBlack else FretMuted,
                        fontWeight = FontWeight.Black,
                        fontSize = if (grande) 22.sp else 18.sp
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text("♩ = $bpm", color = FretMuted, fontSize = 12.sp)
    }
}

/** RHYTHM — guía: qué significa cada flecha y cómo suena el pulso. */
@Composable
fun GuiaRitmo(state: LiveState) {
    val patron = state.objetivos.ifEmpty { listOf("↓") }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Tu patrón de rasgueo", color = FretMuted, fontSize = 13.sp)
        Spacer(Modifier.height(10.dp))
        BarraRitmo(patron, state.bpm ?: 60, grande = true)
        Spacer(Modifier.height(10.dp))
        Text(
            text = "↓ rasguea hacia abajo · ↑ hacia arriba.\n" +
                "Sigue el círculo del metrónomo: un rasgueo por pulso, " +
                "con la muñeca relajada.",
            color = FretText,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            textAlign = TextAlign.Center
        )
    }
}

/** RHYTHM en vivo: metrónomo + contador de golpes con flash de acierto. */
@Composable
fun PasoRitmo(state: LiveState) {
    val patron = state.objetivos.ifEmpty { listOf("↓") }
    val colorGolpes by animateColorAsState(
        targetValue = if (state.feedback == FeedbackVivo.ACIERTO) VerdeOk else FretGold,
        label = "golpesColor"
    )
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BarraRitmo(patron, state.bpm ?: 60, grande = false)
        Spacer(Modifier.height(10.dp))
        Text(
            text = "${state.aciertosPaso} / ${state.esperadosPaso}",
            color = colorGolpes,
            fontWeight = FontWeight.Black,
            fontSize = 44.sp
        )
        Text("golpes detectados · sigue el pulso", color = FretMuted, fontSize = 13.sp)
    }
}
