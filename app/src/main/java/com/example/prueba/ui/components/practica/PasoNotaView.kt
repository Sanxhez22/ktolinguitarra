package com.example.prueba.ui.components.practica

import androidx.compose.foundation.layout.Column
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
import com.example.prueba.data.model.descripcionPosicion
import com.example.prueba.ui.components.EstadoNota
import com.example.prueba.ui.components.FretboardView
import com.example.prueba.ui.components.NotaDiapason
import com.example.prueba.ui.theme.FretGold
import com.example.prueba.ui.theme.FretMuted
import com.example.prueba.ui.theme.FretText
import com.example.prueba.viewmodel.LiveState

/**
 * NOTE — una sola nota sobre el diapasón con el dedo recomendado.
 * Guía: dónde va el dedo, con la posición pulsando en dorado.
 */
@Composable
fun GuiaNota(state: LiveState) {
    val nota = state.objetivos.firstOrNull() ?: return
    val pos = remember(nota) { NotasGuitarra.posicionDe(nota) } ?: return
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Vas a tocar la nota", color = FretMuted, fontSize = 13.sp)
        Text(nota, color = FretGold, fontWeight = FontWeight.Black, fontSize = 40.sp)
        Spacer(Modifier.height(8.dp))
        FretboardView(
            notas = listOf(NotaDiapason(pos, EstadoNota.OBJETIVO)),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = descripcionPosicion(pos),
            color = FretText,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
    }
}

/** NOTE en vivo: la posición cambia de color con el feedback inmediato. */
@Composable
fun PasoNota(state: LiveState) {
    val objetivo = state.objetivoActual ?: state.objetivos.firstOrNull() ?: return
    val pos = remember(objetivo) { NotasGuitarra.posicionDe(objetivo) }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Toca:", color = FretMuted, fontSize = 13.sp)
        Text(
            text = objetivo,
            color = when (estadoNotaDe(state.feedback)) {
                EstadoNota.ACIERTO -> VerdeOk
                EstadoNota.FALLO -> RosaError
                else -> FretGold
            },
            fontWeight = FontWeight.Black,
            fontSize = 44.sp
        )
        if (pos != null) {
            FretboardView(
                notas = listOf(NotaDiapason(pos, estadoNotaDe(state.feedback))),
                modifier = Modifier.fillMaxWidth()
            )
            Text(descripcionPosicion(pos), color = FretMuted, fontSize = 12.sp, textAlign = TextAlign.Center)
        }
        NotaDetectadaLabel(state.notaDetectada, state.feedback)
    }
}
