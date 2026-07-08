package com.example.prueba.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.prueba.data.model.AcordeForma
import com.example.prueba.ui.theme.FretBlack
import com.example.prueba.ui.theme.FretGold
import com.example.prueba.ui.theme.FretMuted
import com.example.prueba.ui.theme.FretText

private val VerdeSuena = Color(0xFF4ADE80)   // cuerdas que deben sonar (○)
private val RosaMuda = Color(0xFFE94584)     // cuerdas que no se tocan (X)

/**
 * Diagrama de acorde estilo cancionero: 6 cuerdas verticales, trastes,
 * dedos numerados (1-4), cejilla, cuerdas al aire (○) y mudas (X).
 *
 * Reutilizable en práctica guiada, canciones, tutoriales, onboarding y
 * biblioteca de acordes: recibe cualquier [AcordeForma] del catálogo.
 *
 * [pasosVisibles] permite "armar" el acorde progresivamente (0 = solo el
 * mástil, n = cejilla y n-1 dedos). Cada paso aparece con fade-in +
 * escalado suave; [pasoResaltado] dibuja un anillo sobre el paso actual.
 */
@Composable
fun DiagramaAcorde(
    forma: AcordeForma,
    modifier: Modifier = Modifier,
    pasosVisibles: Int = Int.MAX_VALUE,
    pasoResaltado: Int? = null,
    mostrarNombre: Boolean = true
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (mostrarNombre) {
            Text(forma.nombre, color = FretGold, fontWeight = FontWeight.Black, fontSize = 22.sp)
            Text(forma.nombreLargo, color = FretMuted, fontSize = 12.sp)
            Spacer(Modifier.height(6.dp))
        }

        // Un progreso animado por paso de colocación (cejilla + dedos).
        val total = forma.totalPasosColocacion
        val progresos = List(total) { i ->
            val visible = i < pasosVisibles
            val prog by animateFloatAsState(
                targetValue = if (visible) 1f else 0f,
                animationSpec = tween(durationMillis = 380),
                label = "dedo$i"
            )
            prog
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.82f)
        ) {
            dibujarDiagrama(forma, progresos, pasoResaltado)
        }
    }
}

private fun DrawScope.dibujarDiagrama(
    forma: AcordeForma,
    progresos: List<Float>,
    pasoResaltado: Int?
) {
    val trasteMax = maxOf(
        forma.dedos.maxOfOrNull { it.traste } ?: forma.trasteBase,
        forma.cejilla?.traste ?: forma.trasteBase
    )
    val numTrastes = (trasteMax - forma.trasteBase + 1).coerceAtLeast(4)

    val margenIzq = size.width * 0.10f
    val margenDer = size.width * 0.04f
    val margenSup = size.height * 0.14f   // zona de ○ / X
    val margenInf = size.height * 0.04f

    val anchoRejilla = size.width - margenIzq - margenDer
    val altoRejilla = size.height - margenSup - margenInf
    val sepCuerdas = anchoRejilla / 5f
    val sepTrastes = altoRejilla / numTrastes

    fun xCuerda(cuerda: Int) = margenIzq + (6 - cuerda) * sepCuerdas
    fun yTraste(trasteRel: Int) = margenSup + trasteRel * sepTrastes

    val pincelTexto = android.graphics.Paint().apply {
        isAntiAlias = true
        textAlign = android.graphics.Paint.Align.CENTER
        isFakeBoldText = true
    }

    // Cejuela (gruesa si el diagrama empieza en el traste 1) y trastes.
    val enCejuela = forma.trasteBase == 1
    drawLine(
        color = FretText,
        start = Offset(xCuerda(6), margenSup),
        end = Offset(xCuerda(1), margenSup),
        strokeWidth = if (enCejuela) sepTrastes * 0.14f else 2.dp.toPx()
    )
    for (t in 1..numTrastes) {
        drawLine(
            color = FretMuted.copy(alpha = 0.55f),
            start = Offset(xCuerda(6), yTraste(t)),
            end = Offset(xCuerda(1), yTraste(t)),
            strokeWidth = 1.5.dp.toPx()
        )
    }
    // Cuerdas (de la 6ª gruesa a la 1ª fina).
    for (c in 6 downTo 1) {
        drawLine(
            color = FretMuted.copy(alpha = 0.85f),
            start = Offset(xCuerda(c), margenSup),
            end = Offset(xCuerda(c), margenSup + altoRejilla),
            strokeWidth = (0.8f + (c - 1) * 0.35f).dp.toPx()
        )
    }
    // Número del traste base cuando no arranca en la cejuela (p. ej. "5").
    if (!enCejuela) {
        pincelTexto.color = FretMuted.toArgb()
        pincelTexto.textSize = sepTrastes * 0.45f
        drawContext.canvas.nativeCanvas.drawText(
            "${forma.trasteBase}",
            margenIzq * 0.45f,
            yTraste(1) - sepTrastes * 0.3f,
            pincelTexto
        )
    }

    // ○ cuerdas que deben sonar al aire (verde) y X las que no se tocan.
    val radioMarca = sepCuerdas * 0.18f
    val yMarca = margenSup - sepTrastes * 0.42f
    forma.cuerdasAlAire.forEach { c ->
        drawCircle(
            color = VerdeSuena,
            radius = radioMarca,
            center = Offset(xCuerda(c), yMarca),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
        )
    }
    forma.cuerdasMudas.forEach { c ->
        val x = xCuerda(c)
        val r = radioMarca
        drawLine(RosaMuda, Offset(x - r, yMarca - r), Offset(x + r, yMarca + r), 2.dp.toPx())
        drawLine(RosaMuda, Offset(x - r, yMarca + r), Offset(x + r, yMarca - r), 2.dp.toPx())
    }

    // Pasos de colocación: cejilla (si hay) es el paso 0, luego los dedos.
    val radioDedo = sepCuerdas * 0.40f
    var paso = 0

    forma.cejilla?.let { cej ->
        val prog = progresos.getOrElse(paso) { 1f }
        if (prog > 0.01f) {
            val escala = 0.6f + 0.4f * prog
            val trasteRel = cej.traste - forma.trasteBase + 1
            val y = yTraste(trasteRel) - sepTrastes / 2f
            val x1 = xCuerda(cej.cuerdaDesde) - radioDedo * escala
            val x2 = xCuerda(cej.cuerdaHasta) + radioDedo * escala
            val alto = radioDedo * 2f * escala
            drawRoundRect(
                color = FretGold.copy(alpha = prog),
                topLeft = Offset(x1, y - alto / 2f),
                size = Size(x2 - x1, alto),
                cornerRadius = CornerRadius(alto / 2f)
            )
            if (paso == pasoResaltado) {
                drawRoundRect(
                    color = FretText.copy(alpha = prog),
                    topLeft = Offset(x1 - 3.dp.toPx(), y - alto / 2f - 3.dp.toPx()),
                    size = Size(x2 - x1 + 6.dp.toPx(), alto + 6.dp.toPx()),
                    cornerRadius = CornerRadius(alto / 2f + 3.dp.toPx()),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                )
            }
            pincelTexto.color = FretBlack.toArgb()
            pincelTexto.textSize = radioDedo * 1.1f * escala
            drawContext.canvas.nativeCanvas.drawText(
                "${cej.dedo}",
                xCuerda(cej.cuerdaDesde),
                y + radioDedo * 0.4f * escala,
                pincelTexto.apply { alpha = (255 * prog).toInt() }
            )
        }
        paso++
    }

    forma.dedos.forEach { dedo ->
        val prog = progresos.getOrElse(paso) { 1f }
        if (prog > 0.01f) {
            val escala = 0.6f + 0.4f * prog
            val trasteRel = dedo.traste - forma.trasteBase + 1
            val centro = Offset(xCuerda(dedo.cuerda), yTraste(trasteRel) - sepTrastes / 2f)
            drawCircle(
                color = FretGold.copy(alpha = prog),
                radius = radioDedo * escala,
                center = centro
            )
            if (paso == pasoResaltado) {
                drawCircle(
                    color = FretText.copy(alpha = prog),
                    radius = radioDedo * escala + 3.dp.toPx(),
                    center = centro,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                )
            }
            pincelTexto.color = FretBlack.toArgb()
            pincelTexto.textSize = radioDedo * 1.1f * escala
            drawContext.canvas.nativeCanvas.drawText(
                "${dedo.dedo}",
                centro.x,
                centro.y + radioDedo * 0.4f * escala,
                pincelTexto.apply { alpha = (255 * prog).toInt() }
            )
        }
        paso++
    }
}
