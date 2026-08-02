package com.example.prueba.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.prueba.audio.PitchStabilizer
import com.example.prueba.audio.TunerEngine
import com.example.prueba.ui.theme.*
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.roundToInt

data class Cuerda(val nombre: String, val freq: Float, val numero: String)

internal val CUERDAS = listOf(
    Cuerda("E2", 82.41f, "6ª"),
    Cuerda("A2", 110.0f, "5ª"),
    Cuerda("D3", 146.83f, "4ª"),
    Cuerda("G3", 196.0f, "3ª"),
    Cuerda("B3", 246.94f, "2ª"),
    Cuerda("E4", 329.63f, "1ª")
)

private val NOTE_NAMES = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

/** Convierte una frecuencia en (nota, octava, cents respecto al semitono más cercano). */
internal fun frequencyToNote(freq: Float): Triple<String, Int, Float> {
    val midi = 69.0 + 12.0 * (ln(freq / 440.0) / ln(2.0))
    val midiR = midi.roundToInt()
    val cents = ((midi - midiR) * 100.0).toFloat()
    val name = NOTE_NAMES[((midiR % 12) + 12) % 12]
    val octave = midiR / 12 - 1
    return Triple(name, octave, cents)
}

/** Cents de desviación de la frecuencia detectada respecto a una cuerda concreta. */
internal fun centsVsCuerda(freq: Float, cuerda: Cuerda): Float =
    (1200.0 * (ln(freq / cuerda.freq) / ln(2.0))).toFloat()

/**
 * Cuerda de guitarra más cercana en escala LOGARÍTMICA (cents, no Hz):
 * con distancia lineal en Hz una 6ª muy baja se atribuía a la cuerda
 * equivocada porque los graves están mucho más juntos en Hz.
 */
internal fun nearestString(freq: Float): Cuerda? =
    if (freq <= 0f) null else CUERDAS.minByOrNull { abs(centsVsCuerda(freq, it)) }

/** Verde afinado / dorado cerca / rosa lejos, compartidos por la pantalla. */
private val VerdeAfinado = Color(0xFF4ADE80)
private val RosaLejos = Color(0xFFE94584)

/** Tolerancia para dar una cuerda por afinada (± cents, sostenido). */
private const val CENTS_AFINADA = 5f

/**
 * Histéresis del estado "afinada": se ENTRA con ±[CENTS_AFINADA], pero una
 * vez confirmada se MANTIENE hasta ±[CENTS_MANTENER]. Sin esto, una lectura
 * en 5.5 cents hacía parpadear el verde aunque la cuerda siguiera bien.
 */
private const val CENTS_MANTENER = 8f

/** Lecturas estables consecutivas dentro de tolerancia para confirmar (~0.5 s). */
private const val LECTURAS_EN_TONO = 10

/** Lecturas seguidas fuera de la banda de mantener para perder el "afinada". */
private const val LECTURAS_PARA_PERDER = 3

/**
 * Afinador estilo GuitarTuna: escala de cents con puntero central, nota
 * detectada en grande, frecuencia actual vs objetivo, diferencia en cents
 * e indicaciones de acción (aprieta / afloja / afinada). La cuerda objetivo
 * se elige sola (con histéresis) o fijada a mano tocando su tarjeta.
 */
@Composable
fun TunerScreen() {
    val context = LocalContext.current

    // ---- Estado de la detección (lo escribe el hilo de audio) ----
    var freqEstable by remember { mutableStateOf(-1f) }
    var cuerdaAuto by remember { mutableStateOf<Cuerda?>(null) }
    var cuerdaManual by remember { mutableStateOf<Cuerda?>(null) }
    var enTonoSeguidas by remember { mutableIntStateOf(0) }
    var fueraTonoSeguidas by remember { mutableIntStateOf(0) }
    var cuerdasAfinadas by remember { mutableStateOf(setOf<String>()) }
    var candidataSeguidas by remember { mutableIntStateOf(0) }
    var candidata by remember { mutableStateOf<Cuerda?>(null) }

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasMicPermission = granted }

    // Primera nota en tono: registra la "afinación real" en el backend
    // (apaga el aviso de guitarra sin afinar del onboarding).
    var notaEnTono by remember { mutableStateOf(false) }
    LaunchedEffect(notaEnTono) {
        if (notaEnTono) {
            com.example.prueba.data.repository.AuthRepository.marcarAfinacionRealizada()
        }
    }

    val stabilizer = remember { PitchStabilizer() }
    val engine = remember {
        TunerEngine(onReading = { lectura ->
            val freq = stabilizer.procesar(lectura)
            freqEstable = freq
            if (freq > 0f) {
                // Selección automática de cuerda con histéresis: cambia solo
                // si la nueva candidata se sostiene 3 lecturas (la aguja no
                // rebota entre cuerdas con un armónico suelto).
                val cercana = nearestString(freq)
                if (cercana != null && cercana.nombre != cuerdaAuto?.nombre) {
                    if (cercana.nombre == candidata?.nombre) candidataSeguidas++
                    else {
                        candidata = cercana
                        candidataSeguidas = 1
                    }
                    if (candidataSeguidas >= 3 || cuerdaAuto == null) {
                        cuerdaAuto = cercana
                        enTonoSeguidas = 0
                        fueraTonoSeguidas = 0
                    }
                } else {
                    candidataSeguidas = 0
                }

                val objetivo = cuerdaManual ?: cuerdaAuto
                if (objetivo != null) {
                    val cents = centsVsCuerda(freq, objetivo)
                    val confirmada = enTonoSeguidas >= LECTURAS_EN_TONO
                    if (abs(cents) <= CENTS_AFINADA) {
                        fueraTonoSeguidas = 0
                        enTonoSeguidas++
                        if (enTonoSeguidas >= LECTURAS_EN_TONO) {
                            notaEnTono = true
                            if (objetivo.nombre !in cuerdasAfinadas) {
                                cuerdasAfinadas = cuerdasAfinadas + objetivo.nombre
                            }
                        }
                    } else if (confirmada && abs(cents) <= CENTS_MANTENER) {
                        // Histéresis: ya estaba afinada y sigue dentro de la
                        // banda de mantener; una lectura al borde no la baja.
                        fueraTonoSeguidas = 0
                    } else if (confirmada) {
                        // Se salió de la banda: solo se pierde el estado si
                        // se sostiene (no por un frame suelto del ataque).
                        fueraTonoSeguidas++
                        if (fueraTonoSeguidas >= LECTURAS_PARA_PERDER) {
                            enTonoSeguidas = 0
                            fueraTonoSeguidas = 0
                        }
                    } else {
                        enTonoSeguidas = 0
                    }
                }
            } else {
                enTonoSeguidas = 0
            }
        })
    }

    // El micrófono corre mientras la pantalla está visible (como GuitarTuna).
    DisposableEffect(hasMicPermission) {
        if (hasMicPermission) {
            stabilizer.reset()
            engine.start()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
        onDispose { engine.stop() }
    }

    val cuerdaObjetivo = cuerdaManual ?: cuerdaAuto
    val hayTono = freqEstable > 0f && cuerdaObjetivo != null
    val cents = if (hayTono) centsVsCuerda(freqEstable, cuerdaObjetivo!!) else 0f
    // Confirmada con ±5 y mantenida hasta ±8 (histéresis anti-parpadeo).
    val afinadaAhora = hayTono && abs(cents) <= CENTS_MANTENER && enTonoSeguidas >= LECTURAS_EN_TONO

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FretBlack)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Afinador",
            color = FretText,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp
        )
        Text(
            text = if (cuerdaManual != null)
                "Cuerda fijada: ${cuerdaManual!!.numero} (${cuerdaManual!!.nombre}) · toca la tarjeta para soltar"
            else
                "Detección automática de cuerda 🎸",
            color = FretMuted,
            fontSize = 13.sp
        )

        // ---- Display principal ----
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = FretSurface),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!hasMicPermission) {
                    Text("🎙", fontSize = 44.sp)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Concede el permiso de micrófono para afinar",
                        color = FretMuted,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                        colors = ButtonDefaults.buttonColors(containerColor = FretGold, contentColor = FretBlack)
                    ) { Text("Permitir micrófono") }
                } else {
                    val color by animateColorAsState(
                        targetValue = when {
                            !hayTono -> FretMuted
                            afinadaAhora -> VerdeAfinado
                            abs(cents) <= 15f -> FretGold
                            else -> RosaLejos
                        },
                        label = "colorAfinador"
                    )

                    // Nota objetivo en grande (la de la cuerda, no el semitono suelto).
                    Text(
                        text = cuerdaObjetivo?.nombre ?: "—",
                        color = color,
                        fontWeight = FontWeight.Black,
                        fontSize = 72.sp
                    )
                    cuerdaObjetivo?.let {
                        Text(
                            text = "Cuerda ${it.numero}",
                            color = FretMuted,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(Modifier.height(18.dp))

                    // Escala de cents con puntero (el corazón del afinador).
                    EscalaCents(
                        cents = if (hayTono) cents else null,
                        color = color,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(96.dp)
                    )

                    Spacer(Modifier.height(14.dp))

                    // Mensaje de acción.
                    val mensaje = when {
                        !hayTono -> "Toca una cuerda…"
                        afinadaAhora -> "✓ Afinada"
                        abs(cents) <= CENTS_AFINADA -> "Mantenla sonando…"
                        cents < 0f -> "← Aprieta la cuerda"
                        else -> "→ Afloja la cuerda"
                    }
                    Text(
                        text = mensaje,
                        color = color,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )

                    Spacer(Modifier.height(14.dp))

                    // Frecuencias: actual · objetivo · diferencia en cents.
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        DatoAfinador(
                            titulo = "Detectada",
                            valor = if (hayTono) "%.1f Hz".format(freqEstable) else "— Hz"
                        )
                        DatoAfinador(
                            titulo = "Objetivo",
                            valor = cuerdaObjetivo?.let { "%.2f Hz".format(it.freq) } ?: "— Hz"
                        )
                        DatoAfinador(
                            titulo = "Desviación",
                            valor = if (hayTono) "%+d cents".format(cents.roundToInt()) else "—",
                            color = color
                        )
                    }
                }
            }
        }

        // ---- Cuerdas: estado + selección manual ----
        Text("Cuerdas · toca una para fijarla", color = FretMuted, fontSize = 12.sp)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CUERDAS.chunked(3).forEach { fila ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    fila.forEach { c ->
                        val esObjetivo = cuerdaObjetivo?.nombre == c.nombre
                        val esManual = cuerdaManual?.nombre == c.nombre
                        val estaAfinada = c.nombre in cuerdasAfinadas
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    cuerdaManual = if (esManual) null else c
                                    enTonoSeguidas = 0
                                    fueraTonoSeguidas = 0
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = when {
                                    estaAfinada -> VerdeAfinado.copy(alpha = 0.20f)
                                    esObjetivo -> FretGold.copy(alpha = 0.20f)
                                    else -> FretSurface
                                }
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = buildString {
                                        append(c.numero)
                                        if (estaAfinada) append(" ✓")
                                        if (esManual) append(" 📌")
                                    },
                                    color = if (estaAfinada) VerdeAfinado else FretMuted,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = c.nombre,
                                    color = when {
                                        estaAfinada -> VerdeAfinado
                                        esObjetivo -> FretGold
                                        else -> FretText
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "%.2f Hz".format(c.freq),
                                    color = FretMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        if (cuerdasAfinadas.size == CUERDAS.size) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = VerdeAfinado.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(
                    text = "🎉 ¡Las 6 cuerdas afinadas! Tu guitarra está lista.",
                    modifier = Modifier.padding(14.dp),
                    color = VerdeAfinado,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun DatoAfinador(titulo: String, valor: String, color: Color = FretText) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(titulo, color = FretMuted, fontSize = 11.sp)
        Text(valor, color = color, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    }
}

/**
 * Escala horizontal de cents (-50 .. +50) estilo GuitarTuna: marcas cada
 * 10 cents, zona verde de tolerancia al centro, línea vertical central y
 * puntero triangular que se desliza suavemente hacia la desviación actual.
 * [cents] null = sin tono (puntero apagado al centro).
 */
@Composable
private fun EscalaCents(cents: Float?, color: Color, modifier: Modifier = Modifier) {
    val objetivo = (cents ?: 0f).coerceIn(-50f, 50f)
    val posicion by animateFloatAsState(
        targetValue = objetivo,
        animationSpec = tween(durationMillis = 120),
        label = "aguja"
    )
    val hayTono = cents != null

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val yEscala = h * 0.52f
        val margen = w * 0.04f
        val ancho = w - margen * 2
        fun xDe(c: Float) = margen + (c + 50f) / 100f * ancho

        val pincel = android.graphics.Paint().apply {
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
        }

        // Zona de tolerancia (±5 cents) resaltada al centro.
        drawRoundRect(
            color = VerdeAfinado.copy(alpha = 0.14f),
            topLeft = Offset(xDe(-CENTS_AFINADA), yEscala - h * 0.24f),
            size = Size(xDe(CENTS_AFINADA) - xDe(-CENTS_AFINADA), h * 0.48f),
            cornerRadius = CornerRadius(6.dp.toPx())
        )

        // Marcas cada 10 cents (más altas en -50, 0, +50).
        for (c in -50..50 step 10) {
            val x = xDe(c.toFloat())
            val mayor = c % 50 == 0
            drawLine(
                color = if (c == 0) FretText else FretMuted.copy(alpha = 0.55f),
                start = Offset(x, yEscala - h * (if (mayor) 0.20f else 0.12f)),
                end = Offset(x, yEscala + h * (if (mayor) 0.20f else 0.12f)),
                strokeWidth = (if (c == 0) 3f else 1.5f).dp.toPx()
            )
            if (mayor) {
                pincel.color = FretMuted.toArgb()
                pincel.textSize = h * 0.14f
                drawContext.canvas.nativeCanvas.drawText(
                    if (c == 0) "0" else "%+d".format(c),
                    x,
                    yEscala + h * 0.40f,
                    pincel
                )
            }
        }

        // Línea central vertical destacada (la referencia de "afinado").
        drawLine(
            color = FretText,
            start = Offset(xDe(0f), yEscala - h * 0.30f),
            end = Offset(xDe(0f), yEscala + h * 0.26f),
            strokeWidth = 3.dp.toPx()
        )

        // Puntero: línea + triángulo bajo la escala, del color del estado.
        val xAguja = xDe(posicion)
        val colorAguja = if (hayTono) color else FretMuted.copy(alpha = 0.4f)
        drawLine(
            color = colorAguja,
            start = Offset(xAguja, yEscala - h * 0.26f),
            end = Offset(xAguja, yEscala + h * 0.22f),
            strokeWidth = 4.dp.toPx()
        )
        val lado = h * 0.13f
        val yTri = yEscala + h * 0.24f
        val triangulo = Path().apply {
            moveTo(xAguja, yTri)
            lineTo(xAguja - lado, yTri + lado * 1.4f)
            lineTo(xAguja + lado, yTri + lado * 1.4f)
            close()
        }
        drawPath(triangulo, colorAguja)
        // Halo del puntero cuando está en tono.
        if (hayTono && abs(posicion) <= CENTS_AFINADA) {
            drawCircle(
                color = VerdeAfinado.copy(alpha = 0.35f),
                radius = 8.dp.toPx(),
                center = Offset(xAguja, yEscala - h * 0.30f),
                style = Stroke(width = 3.dp.toPx())
            )
        }
    }
}
