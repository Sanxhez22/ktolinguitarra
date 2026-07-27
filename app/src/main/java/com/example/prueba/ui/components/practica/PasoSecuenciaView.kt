package com.example.prueba.ui.components.practica

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.prueba.data.model.descripcionPosicion
import com.example.prueba.ui.components.EstadoNota
import com.example.prueba.ui.components.FretboardView
import com.example.prueba.ui.components.NotaDiapason
import com.example.prueba.ui.theme.FretBlack
import com.example.prueba.ui.theme.FretGold
import com.example.prueba.ui.theme.FretMuted
import com.example.prueba.ui.theme.FretSurface
import com.example.prueba.ui.theme.FretText
import com.example.prueba.viewmodel.LiveState

/**
 * Camino de notas de la secuencia: hechas en verde, la actual en dorado
 * (más grande) y las próximas apagadas. Compartido por la guía y el vivo
 * de SEQUENCE (es su representación característica).
 */
@Composable
private fun CaminoNotas(notas: List<String>, actual: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        notas.forEachIndexed { i, nota ->
            val (fondo, tinta) = when {
                i < actual -> VerdeOk.copy(alpha = 0.25f) to VerdeOk
                i == actual -> FretGold to FretBlack
                else -> FretSurface to FretMuted
            }
            Text(
                text = nota,
                color = tinta,
                fontWeight = if (i == actual) FontWeight.Black else FontWeight.SemiBold,
                fontSize = if (i == actual) 18.sp else 14.sp,
                modifier = Modifier
                    .background(fondo, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }
}

/**
 * SEQUENCE — la secuencia se muestra paso a paso: camino de notas arriba y
 * diapasón abajo con la nota actual pulsando y la siguiente insinuada.
 */
@Composable
fun GuiaSecuencia(state: LiveState) {
    val posiciones = remember(state.objetivos) { NotasGuitarra.posicionesDe(state.objetivos) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Tocarás estas notas, en orden", color = FretMuted, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))
        CaminoNotas(state.objetivos, actual = 0)
        Spacer(Modifier.height(10.dp))
        FretboardView(
            notas = posiciones.mapIndexed { i, pos ->
                NotaDiapason(
                    posicion = pos,
                    estado = if (i == 0) EstadoNota.OBJETIVO else EstadoNota.CONTEXTO,
                    orden = i + 1
                )
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(6.dp))
        posiciones.firstOrNull()?.let {
            Text(
                text = "Empieza así: ${descripcionPosicion(it)}",
                color = FretText,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** SEQUENCE en vivo: el camino avanza y el diapasón marca actual/siguiente. */
@Composable
fun PasoSecuencia(state: LiveState) {
    val posiciones = remember(state.objetivos) { NotasGuitarra.posicionesDe(state.objetivos) }
    val actual = state.objetivoIdx.coerceIn(0, (state.objetivos.size - 1).coerceAtLeast(0))
    // Fingerpicking: la secuencia también indica el dedo p-i-m-a que pulsa.
    val esFingerstyle = state.skill == "fingerstyle"
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CaminoNotas(state.objetivos, actual)
        if (esFingerstyle) {
            posiciones.getOrNull(actual)?.let { pos ->
                val dedo = dedoPima(pos.cuerda)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Dedo: $dedo (${nombrePima(dedo)})",
                    color = FretGold,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        FretboardView(
            notas = posiciones.mapIndexed { i, pos ->
                NotaDiapason(
                    posicion = pos,
                    estado = when {
                        i < actual -> EstadoNota.ACIERTO
                        i == actual -> estadoNotaDe(state.feedback)
                        i == actual + 1 -> EstadoNota.SIGUIENTE
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
