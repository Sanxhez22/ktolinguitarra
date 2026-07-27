package com.example.prueba.ui.components.practica

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.prueba.data.model.CatalogoAcordes
import com.example.prueba.ui.components.DiagramaAcorde
import com.example.prueba.ui.theme.FretGold
import com.example.prueba.ui.theme.FretMuted
import com.example.prueba.ui.theme.FretSurface
import com.example.prueba.ui.theme.FretText
import com.example.prueba.viewmodel.LiveState

/**
 * SONG_FRAGMENT — línea de acordes del fragmento con sus diagramas
 * gráficos (nada de texto plano): cada acorde es una tarjeta con su
 * digitación y el activo se marca en dorado compás a compás.
 */
@Composable
fun GuiaFragmentoCancion(state: LiveState) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("El fragmento completo", color = FretMuted, fontSize = 13.sp)
        Text(
            text = state.objetivos.joinToString("  →  "),
            color = FretGold,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
        LineaAcordes(state.objetivos, activo = 0)
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Un compás por acorde, sin detenerte: si un cambio sale " +
                "mal, sigue adelante como en una canción de verdad.",
            color = FretText,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * SONG_FRAGMENT en vivo: el compás activo lo marca el reloj del motor de
 * práctica (el mismo contra el que se cuentan los golpes), y se muestra el
 * acorde que realmente está sonando.
 */
@Composable
fun PasoFragmentoCancion(state: LiveState) {
    val n = state.objetivos.size.coerceAtLeast(1)
    val activo = state.objetivoIdx.coerceIn(0, n - 1)
    val pulso = (state.pulsoIdx.coerceAtLeast(0)) + 1   // 1..4 dentro del compás

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Compás de ", color = FretMuted, fontSize = 14.sp)
            Text(
                text = state.objetivos.getOrNull(activo) ?: "—",
                color = FretGold,
                fontWeight = FontWeight.Black,
                fontSize = 26.sp
            )
            Spacer(Modifier.width(10.dp))
            // Puntos del compás (1-2-3-4).
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (p in 1..4) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                if (p <= pulso) FretGold else FretSurface,
                                CircleShape
                            )
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        LineaAcordes(state.objetivos, activo)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "${state.aciertosPaso} / ${state.esperadosPaso} golpes · " +
                (state.acordeDetectado?.let { "suena: $it" } ?: "sigue el pulso"),
            color = FretMuted,
            fontSize = 13.sp
        )
    }
}

/** Tarjetas de acorde en fila; la del compás activo con borde dorado. */
@Composable
private fun LineaAcordes(acordes: List<String>, activo: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        acordes.forEachIndexed { i, nombre ->
            val forma = remember(nombre) { CatalogoAcordes.buscar(nombre) }
            val esActivo = i == activo
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .border(
                        width = if (esActivo) 2.dp else 1.dp,
                        color = if (esActivo) FretGold else FretSurface,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(6.dp)
            ) {
                if (forma != null) {
                    DiagramaAcorde(
                        forma = forma,
                        modifier = Modifier.width(86.dp),
                        compacto = true
                    )
                } else {
                    Text(
                        text = nombre,
                        color = if (esActivo) FretGold else FretText,
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 24.dp)
                    )
                }
            }
        }
    }
}
