package com.example.prueba.ui.components.practica

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.prueba.ui.components.EstadoNota
import com.example.prueba.ui.theme.FretMuted
import com.example.prueba.viewmodel.FeedbackVivo

/** Verde/rosa de feedback compartidos por las vistas de práctica. */
internal val VerdeOk = Color(0xFF4ADE80)
internal val RosaError = Color(0xFFE94584)

/** Estado visual de la nota EN TURNO según el feedback vivo. */
internal fun estadoNotaDe(feedback: FeedbackVivo): EstadoNota = when (feedback) {
    FeedbackVivo.ACIERTO -> EstadoNota.ACIERTO
    FeedbackVivo.FALLO -> EstadoNota.FALLO
    else -> EstadoNota.OBJETIVO
}

/** Pie común "Detectado: X / Escuchando..." bajo las vistas por pitch. */
@Composable
internal fun NotaDetectadaLabel(notaDetectada: String?, feedback: FeedbackVivo) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(6.dp))
        Text(
            text = notaDetectada?.let { "Detectado: $it" } ?: "Escuchando…",
            color = when (feedback) {
                FeedbackVivo.ACIERTO -> VerdeOk
                FeedbackVivo.FALLO -> RosaError
                else -> FretMuted
            },
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}
