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
import com.example.prueba.data.model.PosicionDiapason
import com.example.prueba.data.model.nombreCuerda
import com.example.prueba.ui.components.EstadoNota
import com.example.prueba.ui.components.FretboardView
import com.example.prueba.ui.components.NotaDiapason
import com.example.prueba.ui.theme.FretGold
import com.example.prueba.ui.theme.FretMuted
import com.example.prueba.ui.theme.FretText
import com.example.prueba.viewmodel.LiveState

/** Cuerda al aire objetivo del paso STRING (por la nota que valida el motor). */
private fun cuerdaObjetivo(state: LiveState): Int? =
    (state.objetivoActual ?: state.objetivos.firstOrNull())
        ?.let { NotasGuitarra.cuerdaAlAire(it) }

/**
 * STRING — se ilumina ÚNICAMENTE la cuerda que debe tocarse, con su
 * indicador de cuerda al aire junto a la cejuela.
 */
@Composable
fun GuiaCuerda(state: LiveState) {
    val cuerda = cuerdaObjetivo(state) ?: return
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Vas a tocar la cuerda", color = FretMuted, fontSize = 13.sp)
        Text(
            text = nombreCuerda(cuerda),
            color = FretGold,
            fontWeight = FontWeight.Black,
            fontSize = 30.sp
        )
        Spacer(Modifier.height(8.dp))
        FretboardView(
            notas = listOf(NotaDiapason(PosicionDiapason(cuerda, 0), EstadoNota.OBJETIVO)),
            cuerdaResaltada = cuerda,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Tócala al aire, sin pisar ningún traste, y déjala sonar.",
            color = FretText,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * STRING en vivo: la cuerda entera brilla, y como es el paso de AFINACIÓN
 * incluye un mini-afinador con la desviación en cents en tiempo real: la
 * cuerda solo se valida cuando suena afinada, igual que en GuitarTuna.
 */
@Composable
fun PasoCuerda(state: LiveState) {
    val cuerda = remember(state.objetivoActual) { cuerdaObjetivo(state) } ?: return
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Toca la cuerda", color = FretMuted, fontSize = 13.sp)
        Text(
            text = nombreCuerda(cuerda),
            color = when (state.feedback) {
                com.example.prueba.viewmodel.FeedbackVivo.ACIERTO -> VerdeOk
                com.example.prueba.viewmodel.FeedbackVivo.FALLO -> RosaError
                else -> FretGold
            },
            fontWeight = FontWeight.Black,
            fontSize = 34.sp
        )
        FretboardView(
            notas = listOf(
                NotaDiapason(PosicionDiapason(cuerda, 0), estadoNotaDe(state.feedback))
            ),
            cuerdaResaltada = cuerda,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(4.dp))
        MiniAfinador(cents = state.centsDetectados, toleranciaCents = 20f)
        val cents = state.centsDetectados
        Text(
            text = when {
                cents == null -> "Escuchando…"
                kotlin.math.abs(cents) <= 20f -> "✓ Afinada, sostenla"
                cents < 0f -> "← Aprieta la cuerda (${cents.toInt()} cents)"
                else -> "→ Afloja la cuerda (+${cents.toInt()} cents)"
            },
            color = when {
                cents == null -> FretMuted
                kotlin.math.abs(cents) <= 20f -> VerdeOk
                else -> FretGold
            },
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
