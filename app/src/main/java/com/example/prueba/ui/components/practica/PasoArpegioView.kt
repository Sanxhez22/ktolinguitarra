package com.example.prueba.ui.components.practica

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.prueba.data.model.NotasGuitarra
import com.example.prueba.ui.components.EstadoNota
import com.example.prueba.ui.components.FretboardView
import com.example.prueba.ui.components.NotaDiapason
import com.example.prueba.ui.theme.FretGold
import com.example.prueba.ui.theme.FretMuted
import com.example.prueba.ui.theme.FretText
import com.example.prueba.viewmodel.LiveState

/**
 * ARPEGGIO — únicamente las notas del arpegio, numeradas en el orden en
 * que deben tocarse (1, 2, 3…), una por una.
 */
@Composable
fun GuiaArpegio(state: LiveState) {
    val posiciones = remember(state.objetivos) { NotasGuitarra.posicionesDe(state.objetivos) }
    // Fingerpicking: cada nota lleva su dedo de mano derecha (p-i-m-a).
    val esFingerstyle = state.skill == "fingerstyle"
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = if (esFingerstyle) "El patrón p-i-m-a, en orden" else "Las notas del arpegio, en orden",
            color = FretMuted,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(4.dp))
        Row {
            state.objetivos.forEachIndexed { i, nota ->
                val etiqueta = if (esFingerstyle) {
                    val pos = posiciones.getOrNull(i)
                    if (pos != null) "$nota (${dedoPima(pos.cuerda)})" else nota
                } else nota
                Text(
                    text = if (i == 0) etiqueta else "  →  $etiqueta",
                    color = if (i == 0) FretGold else FretText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        FretboardView(
            notas = posiciones.mapIndexed { i, pos ->
                NotaDiapason(
                    posicion = pos,
                    estado = if (i == 0) EstadoNota.OBJETIVO else EstadoNota.CONTEXTO,
                    etiqueta = if (esFingerstyle) dedoPima(pos.cuerda) else null,
                    orden = i + 1
                )
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = if (esFingerstyle)
                "p = pulgar · i = índice · m = medio · a = anular.\n" +
                    "Cada dedo pulsa su cuerda; deja sonar cada nota."
            else
                "Toca cada nota por separado y deja que suene antes de " +
                    "pasar a la siguiente. Sigue los números.",
            color = FretText,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            textAlign = TextAlign.Center
        )
    }
}

/** ARPEGGIO en vivo: los números marcan el orden; el turno pulsa en dorado. */
@Composable
fun PasoArpegio(state: LiveState) {
    val posiciones = remember(state.objetivos) { NotasGuitarra.posicionesDe(state.objetivos) }
    val actual = state.objetivoIdx.coerceIn(0, (state.objetivos.size - 1).coerceAtLeast(0))
    val esFingerstyle = state.skill == "fingerstyle"
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Nota ${actual + 1} de ${state.objetivos.size}: ", color = FretMuted, fontSize = 14.sp)
            Text(
                text = state.objetivoActual ?: "—",
                color = FretGold,
                fontWeight = FontWeight.Black,
                fontSize = 26.sp
            )
        }
        if (esFingerstyle) {
            posiciones.getOrNull(actual)?.let { pos ->
                val dedo = dedoPima(pos.cuerda)
                Text(
                    text = "Dedo: $dedo (${nombrePima(dedo)})",
                    color = FretGold,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        FretboardView(
            notas = posiciones.mapIndexed { i, pos ->
                NotaDiapason(
                    posicion = pos,
                    estado = when {
                        i < actual -> EstadoNota.ACIERTO
                        i == actual -> estadoNotaDe(state.feedback)
                        else -> EstadoNota.CONTEXTO
                    },
                    etiqueta = if (esFingerstyle) dedoPima(pos.cuerda) else null,
                    orden = i + 1
                )
            },
            modifier = Modifier.fillMaxWidth()
        )
        NotaDetectadaLabel(state.notaDetectada, state.feedback)
    }
}
