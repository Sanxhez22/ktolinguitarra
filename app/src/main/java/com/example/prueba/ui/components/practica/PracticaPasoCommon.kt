package com.example.prueba.ui.components.practica

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.prueba.ui.components.EstadoNota
import com.example.prueba.ui.theme.FretGold
import com.example.prueba.ui.theme.FretMuted
import com.example.prueba.ui.theme.FretText
import com.example.prueba.viewmodel.FeedbackVivo
import kotlin.math.abs

/** Verde/rosa de feedback compartidos por las vistas de práctica. */
internal val VerdeOk = Color(0xFF4ADE80)
internal val RosaError = Color(0xFFE94584)

/** Estado visual de la nota EN TURNO según el feedback vivo. */
internal fun estadoNotaDe(feedback: FeedbackVivo): EstadoNota = when (feedback) {
    FeedbackVivo.ACIERTO -> EstadoNota.ACIERTO
    FeedbackVivo.FALLO -> EstadoNota.FALLO
    else -> EstadoNota.OBJETIVO
}

/** Dedo de mano derecha p-i-m-a según la cuerda (fingerstyle clásico). */
internal fun dedoPima(cuerda: Int): String = when (cuerda) {
    6, 5, 4 -> "p"
    3 -> "i"
    2 -> "m"
    else -> "a"
}

/** Nombre hablado del dedo p-i-m-a para las instrucciones. */
internal fun nombrePima(dedo: String): String = when (dedo) {
    "p" -> "pulgar"
    "i" -> "índice"
    "m" -> "medio"
    else -> "anular"
}

/**
 * Mini-afinador de los pasos STRING: escala compacta de cents (-50..+50)
 * con zona verde de tolerancia y puntero. Reproduce en pequeño el lenguaje
 * visual del afinador principal para que "afinada" signifique lo mismo en
 * toda la app.
 */
@Composable
internal fun MiniAfinador(cents: Float?, toleranciaCents: Float, modifier: Modifier = Modifier) {
    val objetivo = (cents ?: 0f).coerceIn(-50f, 50f)
    val posicion by animateFloatAsState(
        targetValue = objetivo,
        animationSpec = tween(durationMillis = 120),
        label = "miniAguja"
    )
    val hayTono = cents != null
    val color = when {
        !hayTono -> FretMuted.copy(alpha = 0.4f)
        abs(objetivo) <= toleranciaCents -> VerdeOk
        else -> FretGold
    }

    Canvas(modifier = modifier.fillMaxWidth().height(42.dp)) {
        val w = size.width
        val h = size.height
        val yEscala = h * 0.45f
        val margen = w * 0.06f
        val ancho = w - margen * 2
        fun xDe(c: Float) = margen + (c + 50f) / 100f * ancho

        drawRoundRect(
            color = VerdeOk.copy(alpha = 0.14f),
            topLeft = Offset(xDe(-toleranciaCents), yEscala - h * 0.30f),
            size = Size(xDe(toleranciaCents) - xDe(-toleranciaCents), h * 0.60f),
            cornerRadius = CornerRadius(4.dp.toPx())
        )
        for (c in -50..50 step 10) {
            val x = xDe(c.toFloat())
            drawLine(
                color = if (c == 0) FretText else FretMuted.copy(alpha = 0.5f),
                start = Offset(x, yEscala - h * (if (c == 0) 0.34f else 0.18f)),
                end = Offset(x, yEscala + h * (if (c == 0) 0.34f else 0.18f)),
                strokeWidth = (if (c == 0) 2.5f else 1.2f).dp.toPx()
            )
        }
        val xAguja = xDe(posicion)
        drawLine(
            color = color,
            start = Offset(xAguja, yEscala - h * 0.36f),
            end = Offset(xAguja, yEscala + h * 0.28f),
            strokeWidth = 3.dp.toPx()
        )
        val lado = h * 0.16f
        val yTri = yEscala + h * 0.30f
        val tri = Path().apply {
            moveTo(xAguja, yTri)
            lineTo(xAguja - lado, yTri + lado * 1.3f)
            lineTo(xAguja + lado, yTri + lado * 1.3f)
            close()
        }
        drawPath(tri, color)
    }
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
