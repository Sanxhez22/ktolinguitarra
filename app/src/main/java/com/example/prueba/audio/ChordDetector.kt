package com.example.prueba.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Detección de acordes en tiempo real por croma (chromagram):
 *
 *   PCM -> ventana de Hann -> FFT -> espectro de magnitud ->
 *   energía por clase de nota (12 bins C..B) -> matching contra
 *   plantillas de tríadas mayores/menores -> nombre del acorde + score.
 *
 * YIN es monofónico y no puede validar un acorde rasgueado; este detector
 * cubre esa carencia para los pasos CHORD / CHORD_CHANGE y la práctica
 * guiada de canciones. Es liviano (una FFT de 4096 por ventana) y corre
 * en el mismo hilo de audio del LivePracticeEngine.
 */
class ChordDetector(
    private val sampleRate: Float,
    private val fftSize: Int = 4096
) {
    private val re = FloatArray(fftSize)
    private val im = FloatArray(fftSize)
    private val hann = FloatArray(fftSize) { i ->
        (0.5 - 0.5 * cos(2.0 * PI * i / (fftSize - 1))).toFloat()
    }
    private val chroma = FloatArray(12)

    /** Resultado del matching: acorde ("Am") y score de similitud 0..1. */
    data class Resultado(val acorde: String, val score: Float)

    /**
     * Analiza una ventana PCM y devuelve el acorde más probable, o null si
     * la señal no parece un acorde (poca energía o matching pobre).
     */
    fun detectar(samples: ShortArray, length: Int): Resultado? {
        val n = minOf(length, fftSize)
        if (n < fftSize / 2) return null

        // Ventana de Hann + relleno con ceros.
        var energia = 0.0
        for (i in 0 until fftSize) {
            val v = if (i < n) samples[i] / 32768f else 0f
            re[i] = v * hann[i]
            im[i] = 0f
            energia += (v * v).toDouble()
        }
        val rms = sqrt(energia / n)
        if (rms < 0.015) return null   // silencio o ruido de fondo

        fft(re, im)

        // Croma: acumula magnitud por clase de nota entre 70 y 1050 Hz
        // (fundamentales y primeros armónicos del registro de la guitarra).
        chroma.fill(0f)
        val binHz = sampleRate / fftSize
        val binMin = (70f / binHz).toInt().coerceAtLeast(1)
        val binMax = (1050f / binHz).toInt().coerceAtMost(fftSize / 2 - 1)
        for (b in binMin..binMax) {
            val mag = sqrt(re[b] * re[b] + im[b] * im[b])
            if (mag <= 0f) continue
            val freq = b * binHz
            val midi = 69.0 + 12.0 * (ln(freq / 440.0) / ln(2.0))
            val clase = ((midi.roundToInt() % 12) + 12) % 12
            // Atenúa progresivamente los agudos: los armónicos altos aportan
            // clases de nota ajenas a la tríada (3º armónico = quinta, ok,
            // pero 5º = tercera mayor aunque el acorde sea menor).
            val peso = if (freq < 400f) 1f else (400f / freq)
            chroma[clase] += mag * peso
        }

        var total = 0f
        for (c in chroma) total += c
        if (total <= 0f) return null
        // Normaliza a norma unitaria para el coseno.
        var norma = 0f
        for (c in chroma) norma += c * c
        norma = sqrt(norma)
        if (norma <= 0f) return null

        var mejor: String? = null
        var mejorScore = 0f
        var segundo = 0f
        for ((nombre, plantilla) in PLANTILLAS) {
            var punto = 0f
            for (i in 0 until 12) punto += (chroma[i] / norma) * plantilla[i]
            if (punto > mejorScore) {
                segundo = mejorScore
                mejorScore = punto
                mejor = nombre
            } else if (punto > segundo) {
                segundo = punto
            }
        }
        // Exige un match claro Y con margen sobre el segundo candidato
        // (evita parpadeo C <-> Am, que comparten 2 de 3 notas).
        if (mejor == null || mejorScore < 0.75f || mejorScore - segundo < 0.02f) return null
        return Resultado(mejor, mejorScore)
    }

    companion object {
        private val NOMBRES = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

        // Plantillas normalizadas de tríadas: raíz reforzada (el bajo del
        // acorde domina el rasgueo), tercera y quinta.
        private val PLANTILLAS: List<Pair<String, FloatArray>> = buildList {
            for (raiz in 0 until 12) {
                add(NOMBRES[raiz] to plantilla(raiz, tercera = 4))
                add(NOMBRES[raiz] + "m" to plantilla(raiz, tercera = 3))
            }
        }

        private fun plantilla(raiz: Int, tercera: Int): FloatArray {
            val v = FloatArray(12)
            v[raiz] = 1.0f
            v[(raiz + tercera) % 12] = 0.85f
            v[(raiz + 7) % 12] = 0.75f
            var norma = 0f
            for (x in v) norma += x * x
            norma = sqrt(norma)
            for (i in v.indices) v[i] /= norma
            return v
        }

        private val BEMOL_A_SOSTENIDO = mapOf(
            "Ab" to "G#", "Bb" to "A#", "Cb" to "B", "Db" to "C#",
            "Eb" to "D#", "Fb" to "E", "Gb" to "F#"
        )

        /**
         * Normaliza el objetivo de un paso al vocabulario del detector
         * (tríadas mayores/menores): "Am"->"Am", "A7"->"A", "Bm7"->"Bm",
         * "Em7"->"Em", "Bb"->"A#" (bemoles a sostenidos), "Cmaj7"->"C"
         * (maj NO es menor), "D/F#"->"D" (el bajo no cambia la tríada).
         * Null si no parsea como acorde.
         */
        fun normalizarObjetivo(objetivo: String): String? {
            val m = Regex("^([A-G][#b]?)(m(?![a]j))?").find(objetivo.trim()) ?: return null
            val raizCruda = m.groupValues[1]
            val menor = m.groupValues[2]
            val raiz = BEMOL_A_SOSTENIDO[raizCruda] ?: raizCruda
            if (raiz !in NOMBRES) return null
            return raiz + menor
        }
    }
}

/**
 * FFT radix-2 iterativa in-place (Cooley-Tukey). Suficiente y sin
 * dependencias para el croma; [re]/[im] deben medir una potencia de 2.
 */
internal fun fft(re: FloatArray, im: FloatArray) {
    val n = re.size
    require(n and (n - 1) == 0) { "FFT requiere tamaño potencia de 2" }

    // Reordenamiento bit-reversal.
    var j = 0
    for (i in 0 until n - 1) {
        if (i < j) {
            var t = re[i]; re[i] = re[j]; re[j] = t
            t = im[i]; im[i] = im[j]; im[j] = t
        }
        var m = n shr 1
        while (m in 1..j) {
            j -= m
            m = m shr 1
        }
        j += m
    }

    // Mariposas.
    var largo = 2
    while (largo <= n) {
        val ang = -2.0 * PI / largo
        val wRe = cos(ang).toFloat()
        val wIm = sin(ang).toFloat()
        var i = 0
        while (i < n) {
            var curRe = 1f
            var curIm = 0f
            for (k in 0 until largo / 2) {
                val a = i + k
                val b = i + k + largo / 2
                val tRe = re[b] * curRe - im[b] * curIm
                val tIm = re[b] * curIm + im[b] * curRe
                re[b] = re[a] - tRe
                im[b] = im[a] - tIm
                re[a] += tRe
                im[a] += tIm
                val nRe = curRe * wRe - curIm * wIm
                curIm = curRe * wIm + curIm * wRe
                curRe = nRe
            }
            i += largo
        }
        largo = largo shl 1
    }
}
