package com.example.prueba.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.example.prueba.data.model.CejillaAcorde
import com.example.prueba.data.model.PosicionDiapason
import com.example.prueba.ui.theme.FretBlack
import com.example.prueba.ui.theme.FretGold
import com.example.prueba.ui.theme.FretMuted
import com.example.prueba.ui.theme.FretSurface
import com.example.prueba.ui.theme.FretText
import kotlin.math.sin

private val VerdeAcierto = Color(0xFF4ADE80)
private val RosaFallo = Color(0xFFE94584)

/** Rol visual de una nota dibujada sobre el diapasón. */
enum class EstadoNota {
    OBJETIVO,   // la nota que hay que tocar AHORA (dorada, pulsa)
    SIGUIENTE,  // la que viene después (contorno dorado)
    CONTEXTO,   // parte del patrón (escala/arpegio) pero no es el turno
    ACIERTO,    // se tocó bien (verde)
    FALLO       // se tocó mal (rosa)
}

/** Técnica de mano izquierda/derecha asociada a una nota (para canciones). */
enum class TecnicaNota { NINGUNA, HAMMER_ON, PULL_OFF, SLIDE, BEND, VIBRATO, PALM_MUTE }

/**
 * Una nota colocada en el diapasón: posición física + cómo dibujarla.
 * [etiqueta] es el texto dentro del círculo (por defecto el dedo sugerido);
 * [orden] dibuja un numerito de secuencia junto a la nota (arpegios).
 */
data class NotaDiapason(
    val posicion: PosicionDiapason,
    val estado: EstadoNota = EstadoNota.CONTEXTO,
    val etiqueta: String? = null,
    val orden: Int? = null,
    val esRaiz: Boolean = false,
    val tecnica: TecnicaNota = TecnicaNota.NINGUNA
)

/**
 * Diapasón de guitarra horizontal reutilizable (estilo Yousician/GuitarTuna):
 * cuerdas con grosor real (1ª arriba, 6ª abajo), trastes, marcadores de
 * posición, círculos con número de dedo, indicador de cuerda al aire (zona
 * a la izquierda de la cejuela), cejilla, técnicas (hammer-on, pull-off,
 * slide, bend, vibrato, palm mute) y animación de pulso sobre la nota
 * objetivo. Sirve para práctica guiada, canciones y lecciones futuras.
 *
 * [cuerdaResaltada] ilumina la cuerda completa (ejercicios STRING).
 */
@Composable
fun FretboardView(
    notas: List<NotaDiapason>,
    modifier: Modifier = Modifier,
    cuerdaResaltada: Int? = null,
    cejilla: CejillaAcorde? = null,
    animar: Boolean = true
) {
    // Pulso compartido (0..1) para la nota objetivo y la cuerda resaltada.
    val transicion = rememberInfiniteTransition(label = "fretboardPulse")
    val pulso by transicion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulso"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.85f)
    ) {
        dibujarDiapason(
            notas = notas,
            cuerdaResaltada = cuerdaResaltada,
            cejilla = cejilla,
            pulso = if (animar) pulso else 0f
        )
    }
}

private fun DrawScope.dibujarDiapason(
    notas: List<NotaDiapason>,
    cuerdaResaltada: Int?,
    cejilla: CejillaAcorde?,
    pulso: Float
) {
    // ---- Ventana de trastes visibles -------------------------------------
    val trastesUsados = notas.map { it.posicion.traste }.filter { it > 0 } +
        listOfNotNull(cejilla?.traste)
    val trasteMax = trastesUsados.maxOrNull() ?: 0
    val trasteMin = trastesUsados.minOrNull() ?: 1
    // Desde la cejuela si todo cabe en los primeros 5 trastes; si no, ventana.
    val base = if (trasteMax <= 5) 1 else trasteMin
    val numTrastes = (trasteMax - base + 1).coerceAtLeast(5)
    val enCejuela = base == 1

    // ---- Geometría --------------------------------------------------------
    val margenIzq = size.width * 0.115f   // etiquetas + zona de cuerdas al aire
    val margenDer = size.width * 0.02f
    val margenSup = size.height * 0.10f   // aire para técnicas (bend, H, ~)
    val margenInf = size.height * 0.06f

    val anchoTabla = size.width - margenIzq - margenDer
    val altoTabla = size.height - margenSup - margenInf
    val sepCuerdas = altoTabla / 5f
    val sepTrastes = anchoTabla / numTrastes

    // Cuerda 1 (aguda) arriba ... cuerda 6 (grave) abajo.
    fun yCuerda(cuerda: Int) = margenSup + (cuerda - 1) * sepCuerdas
    fun xTraste(trasteRel: Int) = margenIzq + trasteRel * sepTrastes
    // Centro del espacio del traste absoluto t (donde se pisa).
    fun xNota(trasteAbs: Int) = xTraste(trasteAbs - base + 1) - sepTrastes / 2f

    val pincel = android.graphics.Paint().apply {
        isAntiAlias = true
        textAlign = android.graphics.Paint.Align.CENTER
        isFakeBoldText = true
    }

    // ---- Tabla del diapasón ------------------------------------------------
    drawRoundRect(
        color = FretSurface,
        topLeft = Offset(margenIzq - sepTrastes * 0.06f, margenSup - sepCuerdas * 0.55f),
        size = Size(anchoTabla + sepTrastes * 0.12f, altoTabla + sepCuerdas * 1.1f),
        cornerRadius = CornerRadius(10.dp.toPx())
    )

    // Marcadores de posición (3, 5, 7, 9 simples; 12 doble).
    val yCentro = margenSup + altoTabla / 2f
    for (t in base..(base + numTrastes - 1)) {
        val x = xNota(t)
        when {
            t == 12 -> {
                drawCircle(FretMuted.copy(alpha = 0.22f), sepCuerdas * 0.30f, Offset(x, yCuerda(2) + sepCuerdas / 2f))
                drawCircle(FretMuted.copy(alpha = 0.22f), sepCuerdas * 0.30f, Offset(x, yCuerda(4) + sepCuerdas / 2f))
            }
            t % 12 in setOf(3, 5, 7, 9) ->
                drawCircle(FretMuted.copy(alpha = 0.22f), sepCuerdas * 0.34f, Offset(x, yCentro))
        }
    }

    // Cejuela (o borde de ventana) y trastes.
    drawLine(
        color = FretText,
        start = Offset(margenIzq, margenSup - sepCuerdas * 0.3f),
        end = Offset(margenIzq, margenSup + altoTabla + sepCuerdas * 0.3f),
        strokeWidth = if (enCejuela) 5.dp.toPx() else 2.dp.toPx()
    )
    for (t in 1..numTrastes) {
        drawLine(
            color = FretMuted.copy(alpha = 0.45f),
            start = Offset(xTraste(t), margenSup - sepCuerdas * 0.3f),
            end = Offset(xTraste(t), margenSup + altoTabla + sepCuerdas * 0.3f),
            strokeWidth = 1.5.dp.toPx()
        )
    }
    // Número del primer traste visible cuando la ventana no arranca en 1.
    if (!enCejuela) {
        pincel.color = FretMuted.toArgb()
        pincel.textSize = sepCuerdas * 0.55f
        drawContext.canvas.nativeCanvas.drawText(
            "$base", xNota(base), margenSup + altoTabla + sepCuerdas * 0.95f, pincel
        )
    }

    // Cuerdas: de la 1ª (fina, arriba) a la 6ª (gruesa, abajo).
    val nombres = listOf("e", "B", "G", "D", "A", "E")
    for (c in 1..6) {
        val y = yCuerda(c)
        val esResaltada = c == cuerdaResaltada
        val grosor = (0.8f + (c - 1) * 0.38f).dp.toPx()
        if (esResaltada) {
            // Halo pulsante a lo largo de toda la cuerda.
            val alfa = 0.25f + 0.35f * (0.5f + 0.5f * sin(pulso * 2f * Math.PI).toFloat())
            drawLine(
                color = FretGold.copy(alpha = alfa),
                start = Offset(margenIzq, y),
                end = Offset(margenIzq + anchoTabla, y),
                strokeWidth = grosor + 8.dp.toPx()
            )
        }
        drawLine(
            color = if (esResaltada) FretGold else FretMuted.copy(alpha = 0.85f),
            start = Offset(margenIzq, y),
            end = Offset(margenIzq + anchoTabla, y),
            strokeWidth = grosor
        )
        // Etiqueta de la cuerda (e B G D A E).
        pincel.color = (if (esResaltada) FretGold else FretMuted).toArgb()
        pincel.textSize = sepCuerdas * 0.42f
        drawContext.canvas.nativeCanvas.drawText(
            nombres[c - 1], margenIzq * 0.22f, y + sepCuerdas * 0.15f, pincel
        )
    }

    // Cejilla (barra dorada cruzando cuerdas en un traste).
    cejilla?.let { cej ->
        val x = xNota(cej.traste)
        val ancho = sepTrastes * 0.34f
        val y1 = yCuerda(cej.cuerdaHasta) - sepCuerdas * 0.35f
        val y2 = yCuerda(cej.cuerdaDesde) + sepCuerdas * 0.35f
        drawRoundRect(
            color = FretGold.copy(alpha = 0.9f),
            topLeft = Offset(x - ancho / 2f, y1),
            size = Size(ancho, y2 - y1),
            cornerRadius = CornerRadius(ancho / 2f)
        )
    }

    // ---- Notas -------------------------------------------------------------
    val radio = sepCuerdas * 0.42f
    notas.forEach { nota ->
        val pos = nota.posicion
        if (pos.cuerda !in 1..6) return@forEach
        val y = yCuerda(pos.cuerda)
        // Al aire: círculo en la zona izquierda, pegado a la cejuela.
        val x = if (pos.traste <= 0) margenIzq - sepTrastes * 0.38f else xNota(pos.traste)
        if (pos.traste > 0 && (pos.traste < base || pos.traste > base + numTrastes - 1)) return@forEach

        val relleno = when (nota.estado) {
            EstadoNota.OBJETIVO -> FretGold
            EstadoNota.SIGUIENTE -> FretGold.copy(alpha = 0.30f)
            EstadoNota.CONTEXTO -> FretMuted.copy(alpha = 0.35f)
            EstadoNota.ACIERTO -> VerdeAcierto
            EstadoNota.FALLO -> RosaFallo
        }
        val esAire = pos.traste <= 0

        // Anillo pulsante de la nota objetivo.
        if (nota.estado == EstadoNota.OBJETIVO) {
            val expansion = radio * (0.25f + 0.55f * pulso)
            drawCircle(
                color = FretGold.copy(alpha = (1f - pulso) * 0.55f),
                radius = radio + expansion,
                center = Offset(x, y),
                style = Stroke(width = 2.5.dp.toPx())
            )
        }

        if (esAire && nota.estado in setOf(EstadoNota.CONTEXTO, EstadoNota.SIGUIENTE)) {
            // Cuerda al aire sin turno: solo el aro (como el ○ clásico).
            drawCircle(relleno, radio * 0.72f, Offset(x, y), style = Stroke(width = 2.dp.toPx()))
        } else {
            drawCircle(relleno, radio * (if (esAire) 0.85f else 1f), Offset(x, y))
        }
        // Raíz de la escala: aro exterior.
        if (nota.esRaiz) {
            drawCircle(
                color = FretGold.copy(alpha = 0.85f),
                radius = radio + 2.5.dp.toPx(),
                center = Offset(x, y),
                style = Stroke(width = 2.dp.toPx())
            )
        }

        // Etiqueta interna: la pedida, o el dedo sugerido, o "0" al aire.
        val texto = nota.etiqueta ?: pos.dedo?.toString() ?: if (esAire) "0" else ""
        if (texto.isNotEmpty() && (!esAire || nota.estado !in setOf(EstadoNota.CONTEXTO, EstadoNota.SIGUIENTE))) {
            pincel.color = when (nota.estado) {
                EstadoNota.CONTEXTO -> FretText.copy(alpha = 0.8f).toArgb()
                else -> FretBlack.toArgb()
            }
            pincel.textSize = radio * 1.1f
            drawContext.canvas.nativeCanvas.drawText(texto, x, y + radio * 0.38f, pincel)
        }
        // Numerito de orden (arpegios/secuencias) arriba a la derecha.
        nota.orden?.let { orden ->
            pincel.color = FretGold.toArgb()
            pincel.textSize = radio * 0.85f
            drawContext.canvas.nativeCanvas.drawText(
                "$orden", x + radio * 1.15f, y - radio * 0.75f, pincel
            )
        }

        dibujarTecnica(nota.tecnica, x, y, radio, pincel)
    }
}

/** Símbolos de técnica sobre la nota: H, P, slide, bend, vibrato, P.M. */
private fun DrawScope.dibujarTecnica(
    tecnica: TecnicaNota,
    x: Float,
    y: Float,
    radio: Float,
    pincel: android.graphics.Paint
) {
    if (tecnica == TecnicaNota.NINGUNA) return
    val yTop = y - radio * 1.9f
    when (tecnica) {
        TecnicaNota.HAMMER_ON, TecnicaNota.PULL_OFF, TecnicaNota.PALM_MUTE -> {
            pincel.color = FretGold.toArgb()
            pincel.textSize = radio * 0.9f
            val texto = when (tecnica) {
                TecnicaNota.HAMMER_ON -> "H"
                TecnicaNota.PULL_OFF -> "P"
                else -> "P.M."
            }
            drawContext.canvas.nativeCanvas.drawText(texto, x, yTop, pincel)
        }
        TecnicaNota.SLIDE -> {
            // Flecha diagonal ascendente.
            val p = Path().apply {
                moveTo(x - radio, yTop + radio * 0.5f)
                lineTo(x + radio, yTop - radio * 0.3f)
            }
            drawPath(p, FretGold, style = Stroke(width = 2.dp.toPx()))
            drawCircle(FretGold, 2.dp.toPx(), Offset(x + radio, yTop - radio * 0.3f))
        }
        TecnicaNota.BEND -> {
            // Arco hacia arriba con punta.
            val p = Path().apply {
                moveTo(x, yTop + radio * 0.6f)
                quadraticBezierTo(x + radio * 0.9f, yTop + radio * 0.4f, x + radio * 0.8f, yTop - radio * 0.4f)
            }
            drawPath(p, FretGold, style = Stroke(width = 2.dp.toPx()))
            val punta = Path().apply {
                moveTo(x + radio * 0.55f, yTop - radio * 0.15f)
                lineTo(x + radio * 0.8f, yTop - radio * 0.4f)
                lineTo(x + radio * 1.05f, yTop - radio * 0.1f)
            }
            drawPath(punta, FretGold, style = Stroke(width = 2.dp.toPx()))
        }
        TecnicaNota.VIBRATO -> {
            // Onda ~ sobre la nota.
            val p = Path()
            val ancho = radio * 2f
            p.moveTo(x - ancho / 2f, yTop)
            var i = 0
            while (i < 4) {
                val x0 = x - ancho / 2f + ancho * (i + 0.5f) / 4f
                val x1 = x - ancho / 2f + ancho * (i + 1f) / 4f
                p.quadraticBezierTo(x0, yTop + (if (i % 2 == 0) -radio * 0.45f else radio * 0.45f), x1, yTop)
                i++
            }
            drawPath(p, FretGold, style = Stroke(width = 2.dp.toPx()))
        }
        TecnicaNota.NINGUNA -> Unit
    }
}
