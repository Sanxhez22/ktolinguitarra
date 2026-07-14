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
 * MELODY — la melodía sobre el diapasón: nota en turno grande y pulsando,
 * y las que vienen anunciadas ("Luego: …") para poder anticipar la mano.
 */
@Composable
fun GuiaMelodia(state: LiveState) {
    val posiciones = remember(state.objetivos) { NotasGuitarra.posicionesDe(state.objetivos) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("La melodía que vas a tocar", color = FretMuted, fontSize = 13.sp)
        Text(
            text = state.objetivos.joinToString("  "),
            color = FretGold,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        FretboardView(
            notas = posiciones.mapIndexed { i, pos ->
                NotaDiapason(
                    posicion = pos,
                    estado = if (i == 0) EstadoNota.OBJETIVO else EstadoNota.SIGUIENTE
                )
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Sigue la melodía nota a nota; la app te va mostrando " +
                "cuál viene después para que anticipes los dedos.",
            color = FretText,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            textAlign = TextAlign.Center
        )
    }
}

/** MELODY en vivo: nota actual resaltada y anticipo de las próximas. */
@Composable
fun PasoMelodia(state: LiveState) {
    val posiciones = remember(state.objetivos) { NotasGuitarra.posicionesDe(state.objetivos) }
    val actual = state.objetivoIdx.coerceIn(0, (state.objetivos.size - 1).coerceAtLeast(0))
    val proximas = state.objetivos.drop(actual + 1).take(3)
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = state.objetivoActual ?: "—",
            color = FretGold,
            fontWeight = FontWeight.Black,
            fontSize = 40.sp
        )
        if (proximas.isNotEmpty()) {
            Row {
                Text("Luego: ", color = FretMuted, fontSize = 13.sp)
                Text(
                    text = proximas.joinToString(" · "),
                    color = FretText,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
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
                        i == actual + 1 -> EstadoNota.SIGUIENTE
                        else -> EstadoNota.CONTEXTO
                    }
                )
            },
            modifier = Modifier.fillMaxWidth()
        )
        NotaDetectadaLabel(state.notaDetectada, state.feedback)
    }
}
