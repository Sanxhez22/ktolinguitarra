package com.example.prueba.ui.components.practica

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
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
import com.example.prueba.ui.theme.FretSurface
import com.example.prueba.ui.theme.FretText
import com.example.prueba.viewmodel.LiveState

/** Notas de la escala sobre el diapasón, con las raíces marcadas con aro. */
private fun notasEscala(
    objetivos: List<String>,
    actual: Int,
    estadoActual: EstadoNota
): List<NotaDiapason> {
    val raiz = objetivos.firstOrNull() ?: return emptyList()
    return objetivos.mapIndexedNotNull { i, nota ->
        val pos = NotasGuitarra.posicionDe(nota) ?: return@mapIndexedNotNull null
        NotaDiapason(
            posicion = pos,
            estado = when {
                actual < 0 -> EstadoNota.CONTEXTO          // guía: patrón completo
                i < actual -> EstadoNota.ACIERTO
                i == actual -> estadoActual
                else -> EstadoNota.CONTEXTO
            },
            esRaiz = NotasGuitarra.mismaClase(nota, raiz)
        )
    }
}

/**
 * SCALE — el patrón completo de la escala queda visible en el diapasón
 * (las raíces con aro dorado) y la nota que toca se va iluminando.
 */
@Composable
fun GuiaEscala(state: LiveState) {
    val notas = remember(state.objetivos) {
        notasEscala(state.objetivos, actual = 0, estadoActual = EstadoNota.OBJETIVO)
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("El patrón de la escala", color = FretMuted, fontSize = 13.sp)
        Text(
            text = state.objetivos.joinToString(" · "),
            color = FretGold,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        FretboardView(notas = notas, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Las notas con aro son la raíz. Sube nota por nota, " +
                "lenta y limpia; la que pulsa en dorado es la primera.",
            color = FretText,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            textAlign = TextAlign.Center
        )
    }
}

/** SCALE en vivo: patrón fijo, recorrido en verde y siguiente iluminada. */
@Composable
fun PasoEscala(state: LiveState) {
    val actual = state.objetivoIdx.coerceIn(0, (state.objetivos.size - 1).coerceAtLeast(0))
    val notas = remember(state.objetivos, actual, state.feedback) {
        notasEscala(state.objetivos, actual, estadoNotaDe(state.feedback))
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Siguiente: ", color = FretMuted, fontSize = 14.sp)
            Text(
                text = state.objetivoActual ?: "—",
                color = FretGold,
                fontWeight = FontWeight.Black,
                fontSize = 26.sp
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "${state.aciertosPaso}/${state.objetivos.size}",
                color = FretText,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
        }
        Spacer(Modifier.height(6.dp))
        FretboardView(notas = notas, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = {
                if (state.objetivos.isEmpty()) 0f
                else state.aciertosPaso.toFloat() / state.objetivos.size
            },
            modifier = Modifier.fillMaxWidth().height(5.dp),
            color = VerdeOk,
            trackColor = FretSurface
        )
        NotaDetectadaLabel(state.notaDetectada, state.feedback)
    }
}
