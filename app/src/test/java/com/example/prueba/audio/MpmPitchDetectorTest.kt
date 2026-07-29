package com.example.prueba.audio

import com.example.prueba.ui.screens.CUERDAS
import com.example.prueba.ui.screens.centsVsCuerda
import com.example.prueba.ui.screens.nearestString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Valida el motor de detección de pitch con señales sintéticas de guitarra:
 * serie armónica con inharmonicidad de cuerda real (B≈2e-4), decaimiento
 * exponencial por parcial y ruido de micrófono. Cubre los timbres que
 * rompían al detector anterior (2º armónico dominante, fundamental casi
 * ausente por el filtro paso-alto del mic del celular).
 */
class MpmPitchDetectorTest {

    private val sampleRate = 44100f
    private val n = 4096

    /** Cuerda punteada sintética: parciales inharmónicos con decaimiento. */
    private fun guitarPluck(
        f0: Float,
        armonicos: FloatArray,
        seed: Int = 1,
        ruido: Float = 0.004f
    ): ShortArray {
        val rng = Random(seed)
        val fases = FloatArray(armonicos.size) { rng.nextFloat() * 2f * PI.toFloat() }
        val out = FloatArray(n)
        for (i in 0 until n) {
            val t = i / sampleRate
            var v = 0f
            for (k in armonicos.indices) {
                val orden = k + 1
                val fk = orden * f0 * sqrt(1f + 2e-4f * orden * orden)
                val env = exp(-1.5f * orden * t)
                v += armonicos[k] * env * sin(2f * PI.toFloat() * fk * t + fases[k])
            }
            out[i] = v + ruido * (rng.nextFloat() * 2f - 1f)
        }
        val maxAbs = out.maxOf { abs(it) }.coerceAtLeast(1e-9f)
        return ShortArray(n) { ((out[it] / maxAbs) * 0.25f * 32767f).toInt().toShort() }
    }

    /** Timbres típicos: de púa suave a mic que filtra los graves. */
    private val perfiles = mapOf(
        "fundamental fuerte" to floatArrayOf(1f, 0.5f, 0.3f, 0.2f, 0.1f, 0.05f),
        "2do armonico dominante" to floatArrayOf(0.4f, 1f, 0.5f, 0.3f, 0.2f, 0.1f),
        "fundamental debil" to floatArrayOf(0.15f, 1f, 0.7f, 0.4f, 0.2f, 0.1f),
        "fundamental ausente" to floatArrayOf(0.03f, 1f, 0.8f, 0.5f, 0.3f, 0.15f)
    )

    private fun cents(f: Float, ref: Float): Float =
        (1200.0 * ln(f / ref) / ln(2.0)).toFloat()

    @Test
    fun `las 6 cuerdas afinadas se detectan con la nota y cuerda correctas`() {
        val detector = MpmPitchDetector(sampleRate, n)
        for (cuerda in CUERDAS) {
            for ((nombre, perfil) in perfiles) {
                for (seed in 1..3) {
                    val lectura = detector.detectReading(guitarPluck(cuerda.freq, perfil, seed), n)
                    assertTrue(
                        "${cuerda.nombre} ($nombre, seed $seed): sin tono detectado",
                        lectura.freqHz > 0f
                    )
                    val desvio = cents(lectura.freqHz, cuerda.freq)
                    assertTrue(
                        "${cuerda.nombre} ($nombre, seed $seed): " +
                            "detectó ${lectura.freqHz} Hz (${desvio} cents de error)",
                        abs(desvio) < 5f
                    )
                    assertEquals(
                        "${cuerda.nombre} ($nombre, seed $seed): cuerda equivocada",
                        cuerda.nombre,
                        nearestString(lectura.freqHz)?.nombre
                    )
                }
            }
        }
    }

    @Test
    fun `cuerdas desafinadas 30 cents se miden con error menor a 3 cents`() {
        val detector = MpmPitchDetector(sampleRate, n)
        for (cuerda in CUERDAS) {
            for (detune in floatArrayOf(-30f, 30f)) {
                val fReal = cuerda.freq * 2f.pow(detune / 1200f)
                for ((nombre, perfil) in perfiles) {
                    val lectura = detector.detectReading(guitarPluck(fReal, perfil), n)
                    assertTrue("${cuerda.nombre} $detune c ($nombre): sin tono", lectura.freqHz > 0f)
                    val error = cents(lectura.freqHz, fReal)
                    assertTrue(
                        "${cuerda.nombre} $detune c ($nombre): error de $error cents",
                        abs(error) < 3f
                    )
                    // La cuerda identificada sigue siendo la que se toca.
                    assertEquals(cuerda.nombre, nearestString(lectura.freqHz)?.nombre)
                }
            }
        }
    }

    @Test
    fun `el silencio y el ruido no producen tono`() {
        val detector = MpmPitchDetector(sampleRate, n)
        val silencio = ShortArray(n)
        assertEquals(-1f, detector.detectReading(silencio, n).freqHz)

        val rng = Random(7)
        val ruido = ShortArray(n) { ((rng.nextFloat() * 2f - 1f) * 0.2f * 32767f).toInt().toShort() }
        val lectura = detector.detectReading(ruido, n)
        // Ruido blanco: o no hay tono, o la claridad es tan baja que el
        // estabilizador lo descarta (confianza < 0.55).
        assertTrue(
            "el ruido produjo tono confiable: ${lectura.freqHz} Hz conf ${lectura.confidence}",
            lectura.freqHz <= 0f || lectura.confidence < 0.55f
        )
    }

    @Test
    fun `un armonico natural no se confunde con la cuerda una octava abajo`() {
        // Señal SOLO con contenido en 2f0 y múltiplos (armónico al traste 12
        // de E2): debe detectarse E3 (164.8 Hz), no E2.
        val detector = MpmPitchDetector(sampleRate, n)
        val f0 = 164.81f
        val lectura = detector.detectReading(
            guitarPluck(f0, floatArrayOf(1f, 0.4f, 0.2f)), n
        )
        assertTrue(lectura.freqHz > 0f)
        assertTrue(
            "detectó ${lectura.freqHz} Hz en vez de ~$f0 Hz",
            abs(cents(lectura.freqHz, f0)) < 5f
        )
    }

    @Test
    fun `el estabilizador filtra un outlier de un frame`() {
        val estabilizador = PitchStabilizer()
        // 5 lecturas buenas de A2 con un outlier (armónico suelto) en medio.
        val lecturas = floatArrayOf(110.1f, 109.9f, 220.3f, 110.0f, 110.2f)
        var salida = -1f
        for (f in lecturas) {
            salida = estabilizador.procesar(PitchReading(f, 0.9f, 0.05f))
        }
        assertTrue("el estabilizador no entregó tono", salida > 0f)
        assertTrue(
            "el outlier de 220 Hz contaminó la salida: $salida Hz",
            abs(cents(salida, 110f)) < 20f
        )
    }

    @Test
    fun `frecuencia a cuerda usa distancia logaritmica`() {
        // El punto medio logarítmico entre E2 (82.41) y A2 (110) es ~95.2 Hz:
        // 96 Hz ya queda más cerca de A2 en cents.
        assertEquals("A2", nearestString(96f)?.nombre)
        // Muy por debajo de E2 sigue siendo E2.
        assertEquals("E2", nearestString(70f)?.nombre)
        val cuerda = CUERDAS.first { it.nombre == "E2" }
        assertEquals(0f, centsVsCuerda(82.41f, cuerda), 0.5f)
    }
}
