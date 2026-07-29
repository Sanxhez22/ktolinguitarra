package com.example.prueba.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlin.concurrent.thread
import kotlin.math.sqrt

/**
 * Lectura de pitch con su calidad: frecuencia en Hz (-1 si no hay tono),
 * confianza 0..1 (1 = periodicidad perfecta) y RMS de la ventana.
 */
data class PitchReading(
    val freqHz: Float,
    val confidence: Float,
    val rms: Float
)

/**
 * Captura audio del micrófono con AudioRecord y detecta la frecuencia
 * fundamental en tiempo real usando MPM (McLeod Pitch Method) sobre
 * ventanas SOLAPADAS: se analiza una ventana de [analysisSize] muestras
 * cada [hopSize], así el afinador responde ~2x más rápido sin perder
 * resolución en graves.
 *
 * La fuente de audio se elige en cascada: UNPROCESSED (sin AGC, sin
 * supresión de ruido, sin filtro paso-alto del OEM) → VOICE_RECOGNITION
 * (sin AGC/NS en la mayoría de dispositivos) → MIC. El procesamiento del
 * OEM en MIC atenúa el fundamental de la 6ª cuerda (82 Hz) y mete
 * distorsión que ensucia la detección.
 *
 * El permiso RECORD_AUDIO debe estar concedido ANTES de llamar a start().
 *
 * @param onReading callback por cada ventana analizada.
 */
class TunerEngine(
    private val sampleRate: Int = 44100,
    private val analysisSize: Int = 4096,
    private val hopSize: Int = 2048,
    private val onReading: (PitchReading) -> Unit
) {
    @Volatile private var running = false
    private var record: AudioRecord? = null
    private var worker: Thread? = null

    private val minBuffer = AudioRecord.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    ).coerceAtLeast(analysisSize * 2)

    @SuppressLint("MissingPermission") // El llamador garantiza RECORD_AUDIO concedido.
    private fun crearAudioRecord(): AudioRecord? {
        val fuentes = intArrayOf(
            MediaRecorder.AudioSource.UNPROCESSED,
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            MediaRecorder.AudioSource.MIC
        )
        for (fuente in fuentes) {
            val rec = try {
                AudioRecord(
                    fuente,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBuffer
                )
            } catch (_: IllegalArgumentException) {
                continue
            }
            if (rec.state == AudioRecord.STATE_INITIALIZED) return rec
            rec.release()
        }
        return null
    }

    fun start() {
        if (running) return

        val rec = crearAudioRecord() ?: return
        record = rec
        running = true
        rec.startRecording()

        worker = thread(name = "TunerEngine") {
            val ventana = ShortArray(analysisSize)
            val hop = ShortArray(hopSize)
            val detector = MpmPitchDetector(sampleRate.toFloat(), analysisSize)
            var llenado = 0
            while (running) {
                val read = rec.read(hop, 0, hopSize)
                if (read <= 0) continue
                // Desplaza la ventana y agrega el hop al final (solapamiento).
                if (llenado + read <= analysisSize) {
                    hop.copyInto(ventana, llenado, 0, read)
                    llenado += read
                } else {
                    val desplaza = llenado + read - analysisSize
                    ventana.copyInto(ventana, 0, desplaza, llenado)
                    hop.copyInto(ventana, analysisSize - read, 0, read)
                    llenado = analysisSize
                }
                if (llenado < analysisSize) continue
                onReading(detector.detectReading(ventana, analysisSize))
            }
        }
    }

    fun stop() {
        running = false
        worker?.join(250)
        worker = null
        record?.let {
            try {
                if (it.recordingState == AudioRecord.RECORDSTATE_RECORDING) it.stop()
            } catch (_: IllegalStateException) {
            }
            it.release()
        }
        record = null
    }
}

/**
 * Detector de tono MPM (McLeod Pitch Method, "A Smarter Way to Find Pitch",
 * McLeod & Wyvill 2005) — el método usado por los afinadores de guitarra
 * modernos. Reemplaza al YIN anterior, cuya "corrección de octava" halvaba
 * sistemáticamente D3/G3/B3 (toda señal periódica también tiene un mínimo
 * profundo en 2·tau, así que la corrección se disparaba con la nota correcta).
 *
 * Cómo evita los errores de octava SIN parches:
 *  1. NSDF (autocorrelación con normalización especial): n'(tau) =
 *     2·acf(tau) / (m(tau)), acotada a [-1, 1] e inmune al decaimiento
 *     de la cuerda punteada.
 *  2. Se listan TODOS los máximos locales entre cruces por cero
 *     (uno por periodo candidato: T/2, T, 2T…).
 *  3. Se elige el PRIMER máximo que supere k·(máximo global) con k=[kUmbral]:
 *     el fundamental real siempre tiene NSDF ≈ máximo, mientras que T/2
 *     (armónico) y 2T (subarmónico) quedan por debajo del umbral relativo.
 *
 * Además:
 *  - elimina el offset DC antes de correlacionar (los mics de celular lo tienen)
 *  - restringe el rango de búsqueda a [minFreq]-[maxFreq]
 *  - interpolación parabólica del pico (precisión sub-muestra, <1 cent)
 *  - devuelve claridad (valor del pico NSDF, 0..1) como confianza para que
 *    el llamador filtre lecturas malas
 */
class MpmPitchDetector(
    private val sampleRate: Float,
    bufferSize: Int,
    minFreq: Float = 60f,
    maxFreq: Float = 1000f,
    private val kUmbral: Float = 0.90f,
    private val claridadMin: Float = 0.50f
) {
    private val tauMin = (sampleRate / maxFreq).toInt().coerceAtLeast(2)
    private val tauMaxCfg = (sampleRate / minFreq).toInt() + 1
    private val nsdf = FloatArray(bufferSize / 2)
    private val signal = FloatArray(bufferSize)
    // Buffers de picos reutilizados (sin alocar en el hot path del hilo de audio).
    private val picoTau = FloatArray(64)
    private val picoVal = FloatArray(64)

    /** Compatibilidad con el llamador clásico: solo la frecuencia (o -1). */
    fun detect(samples: ShortArray, length: Int): Float =
        detectReading(samples, length).freqHz

    fun detectReading(samples: ShortArray, length: Int): PitchReading {
        val n = if (length < signal.size) length else signal.size

        // Normaliza a [-1,1], quita DC y calcula RMS para descartar silencio.
        var suma = 0.0
        for (i in 0 until n) suma += samples[i]
        val media = (suma / n).toFloat() / 32768f
        var sumSq = 0.0
        for (i in 0 until n) {
            val v = samples[i] / 32768f - media
            signal[i] = v
            sumSq += v.toDouble() * v
        }
        val rms = sqrt(sumSq / n).toFloat()
        if (rms < 0.008f) return PitchReading(-1f, 0f, rms)

        val tauMax = minOf(n / 2, tauMaxCfg)
        if (tauMax <= tauMin + 2) return PitchReading(-1f, 0f, rms)

        // 1) NSDF: n'(tau) = 2·sum(x[j]·x[j+tau]) / sum(x[j]² + x[j+tau]²).
        // m(tau) se actualiza incrementalmente (O(1) por tau).
        nsdf[0] = 1f
        var m = (2.0 * sumSq).toFloat()
        for (tau in 1 until tauMax) {
            m -= signal[tau - 1] * signal[tau - 1] + signal[n - tau] * signal[n - tau]
            var acf = 0f
            for (j in 0 until n - tau) acf += signal[j] * signal[j + tau]
            nsdf[tau] = if (m <= 0f) 0f else 2f * acf / m
        }

        // 2) Máximos locales entre cruces por cero neg→pos (un candidato por
        // periodo). Se ignora el pico inicial en tau=0 exigiendo que la señal
        // NSDF haya cruzado por debajo de cero primero.
        var nPicos = 0
        var tau = 1
        while (tau < tauMax - 1 && nPicos < picoTau.size) {
            // Avanza hasta un cruce negativo→positivo.
            while (tau < tauMax - 1 && !(nsdf[tau] <= 0f && nsdf[tau + 1] > 0f)) tau++
            if (tau >= tauMax - 1) break
            // Máximo local dentro de la región positiva.
            var mejorVal = -1f
            var mejorTau = -1
            while (tau < tauMax - 1 && nsdf[tau + 1] > 0f) {
                tau++
                if (nsdf[tau] > mejorVal) {
                    mejorVal = nsdf[tau]
                    mejorTau = tau
                }
            }
            if (mejorTau in tauMin until tauMax - 1) {
                // Interpolación parabólica del pico (tau y valor sub-muestra).
                val s0 = nsdf[mejorTau - 1]
                val s1 = nsdf[mejorTau]
                val s2 = nsdf[mejorTau + 1]
                val denom = 2f * (2f * s1 - s0 - s2)
                val delta = if (denom == 0f) 0f else ((s2 - s0) / denom).coerceIn(-1f, 1f)
                picoTau[nPicos] = mejorTau + delta
                picoVal[nPicos] = s1 + (s2 - s0) * delta / 4f
                nPicos++
            }
        }
        if (nPicos == 0) return PitchReading(-1f, 0f, rms)

        // 3) Primer pico que supere k·máximo global.
        var vMax = 0f
        for (i in 0 until nPicos) if (picoVal[i] > vMax) vMax = picoVal[i]
        if (vMax < claridadMin) return PitchReading(-1f, vMax.coerceIn(0f, 1f), rms)
        val umbral = kUmbral * vMax
        for (i in 0 until nPicos) {
            if (picoVal[i] >= umbral) {
                val freq = sampleRate / picoTau[i]
                val claridad = picoVal[i].coerceIn(0f, 1f)
                return if (freq in 60f..1000f) PitchReading(freq, claridad, rms)
                else PitchReading(-1f, 0f, rms)
            }
        }
        return PitchReading(-1f, 0f, rms)
    }
}

/**
 * Estabilizador de pitch para el afinador (estilo GuitarTuna): convierte la
 * ráfaga de lecturas crudas en una frecuencia estable que no salta.
 *
 *  - descarta lecturas de baja confianza
 *  - mediana de las últimas [tamanoMediana] lecturas válidas (mata outliers
 *    de un frame: ataques, armónicos sueltos)
 *  - suavizado exponencial SOLO dentro del mismo semitono; al cambiar de
 *    nota engancha directo (respuesta inmediata al cambiar de cuerda)
 *  - se apaga tras [msSilencio] sin lecturas válidas (la aguja no queda
 *    congelada mostrando una nota vieja)
 */
class PitchStabilizer(
    private val confianzaMin: Float = 0.55f,
    private val tamanoMediana: Int = 5,
    private val alpha: Float = 0.35f,
    private val msSilencio: Long = 700L
) {
    private val recientes = ArrayDeque<Float>()
    private var suavizada = -1f
    private var ultimaValida = 0L

    /**
     * Procesa una lectura cruda y devuelve la frecuencia estable en Hz,
     * o -1 si ahora mismo no hay tono confiable.
     */
    fun procesar(lectura: PitchReading): Float {
        val ahora = System.currentTimeMillis()
        if (lectura.freqHz > 0f && lectura.confidence >= confianzaMin) {
            recientes.addLast(lectura.freqHz)
            while (recientes.size > tamanoMediana) recientes.removeFirst()
            ultimaValida = ahora
        } else if (ahora - ultimaValida > msSilencio) {
            recientes.clear()
            suavizada = -1f
            return -1f
        }
        if (recientes.size < 3) return suavizada.takeIf { it > 0f } ?: -1f

        val mediana = recientes.sorted()[recientes.size / 2]
        suavizada = if (suavizada <= 0f) {
            mediana
        } else {
            // ¿Seguimos en la misma nota? (menos de medio semitono de salto)
            val ratio = mediana / suavizada
            if (ratio in 0.9715f..1.0293f) {
                suavizada + alpha * (mediana - suavizada)
            } else {
                mediana   // cambio de nota: engancha sin arrastre
            }
        }
        return suavizada
    }

    fun reset() {
        recientes.clear()
        suavizada = -1f
        ultimaValida = 0L
    }
}
