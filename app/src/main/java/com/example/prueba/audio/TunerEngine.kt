package com.example.prueba.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlin.concurrent.thread
import kotlin.math.abs
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
 * fundamental en tiempo real usando YIN mejorado (corrección de octava +
 * interpolación parabólica) sobre ventanas SOLAPADAS: se analiza una
 * ventana de [analysisSize] muestras cada [hopSize], así el afinador
 * responde ~2x más rápido sin perder resolución en graves.
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
    fun start() {
        if (running) return

        val rec = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            minBuffer
        )
        if (rec.state != AudioRecord.STATE_INITIALIZED) {
            rec.release()
            return
        }
        record = rec
        running = true
        rec.startRecording()

        worker = thread(name = "TunerEngine") {
            val ventana = ShortArray(analysisSize)
            val hop = ShortArray(hopSize)
            val detector = YinPitchDetector(sampleRate.toFloat(), analysisSize)
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
 * Detector de tono YIN (de Cheveigné & Kawahara, 2002) afinado para guitarra:
 *
 *  - elimina el offset DC antes de correlacionar (los mics de celular lo tienen)
 *  - restringe el rango de búsqueda a 60-1000 Hz (tau acotado: menos cómputo
 *    y menos falsos positivos fuera del registro de la guitarra)
 *  - CORRECCIÓN DE OCTAVA: cuando el 2º armónico domina (típico en las
 *    cuerdas 4ª y 1ª), YIN elige tau/2 y lee una octava de más; se verifica
 *    si el doble del periodo también es un mínimo fuerte y se prefiere el
 *    fundamental real
 *  - interpolación parabólica del mínimo (precisión sub-muestra ≈ <1 cent)
 *  - devuelve confianza (1 - cmnd) para que el llamador filtre lecturas malas
 */
class YinPitchDetector(
    private val sampleRate: Float,
    bufferSize: Int,
    private val threshold: Float = 0.12f,
    minFreq: Float = 60f,
    maxFreq: Float = 1000f
) {
    private val tauMin = (sampleRate / maxFreq).toInt().coerceAtLeast(2)
    private val tauMaxCfg = (sampleRate / minFreq).toInt() + 1
    private val halfSize = bufferSize / 2
    private val diff = FloatArray(halfSize)
    private val cmnd = FloatArray(halfSize)
    private val signal = FloatArray(bufferSize)

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
        val ventana = n - tauMax   // largo fijo de correlación para todo tau

        // 1) Función de diferencia (ventana constante: d(tau) comparable entre taus).
        for (tau in 0 until tauMax) {
            var sum = 0f
            for (j in 0 until ventana) {
                val d = signal[j] - signal[j + tau]
                sum += d * d
            }
            diff[tau] = sum
        }

        // 2) Diferencia media acumulada normalizada.
        cmnd[0] = 1f
        var acumulado = 0f
        for (tau in 1 until tauMax) {
            acumulado += diff[tau]
            cmnd[tau] = if (acumulado == 0f) 1f else diff[tau] * tau / acumulado
        }

        // 3) Umbral absoluto: primer mínimo local bajo el umbral dentro del rango.
        var tauEstimate = -1
        var tau = tauMin
        while (tau < tauMax) {
            if (cmnd[tau] < threshold) {
                while (tau + 1 < tauMax && cmnd[tau + 1] < cmnd[tau]) tau++
                tauEstimate = tau
                break
            }
            tau++
        }
        // Sin mínimo bajo el umbral: acepta el mínimo global solo si es decente.
        if (tauEstimate == -1) {
            var mejor = tauMin
            for (t in tauMin until tauMax) if (cmnd[t] < cmnd[mejor]) mejor = t
            if (cmnd[mejor] < 0.35f) tauEstimate = mejor
            else return PitchReading(-1f, 0f, rms)
        }

        // 3b) Corrección de octava: si el periodo DOBLE también es un mínimo
        // fuerte, lo detectado era el 2º armónico (una octava de más). Se
        // prefiere el fundamental real. Se repite una vez por si dominaba el 4º.
        repeat(2) {
            val doble = tauEstimate * 2
            if (doble < tauMax) {
                var t2 = doble
                // Busca el mínimo local alrededor del doble exacto (±2).
                for (c in (doble - 2)..(doble + 2)) {
                    if (c in tauMin until tauMax && cmnd[c] < cmnd[t2]) t2 = c
                }
                if (cmnd[t2] < threshold * 1.4f && cmnd[t2] < cmnd[tauEstimate] * 2.2f) {
                    tauEstimate = t2
                }
            }
        }

        // 4) Interpolación parabólica para refinar el periodo (sub-muestra).
        val x0 = if (tauEstimate > 0) tauEstimate - 1 else tauEstimate
        val x2 = if (tauEstimate + 1 < tauMax) tauEstimate + 1 else tauEstimate
        val betterTau: Float = when {
            x0 == tauEstimate -> if (cmnd[tauEstimate] <= cmnd[x2]) tauEstimate.toFloat() else x2.toFloat()
            x2 == tauEstimate -> if (cmnd[tauEstimate] <= cmnd[x0]) tauEstimate.toFloat() else x0.toFloat()
            else -> {
                val s0 = cmnd[x0]; val s1 = cmnd[tauEstimate]; val s2 = cmnd[x2]
                val denom = 2f * (2f * s1 - s2 - s0)
                if (denom == 0f) tauEstimate.toFloat() else tauEstimate + (s2 - s0) / denom
            }
        }

        val freq = sampleRate / betterTau
        val confianza = (1f - cmnd[tauEstimate]).coerceIn(0f, 1f)
        return if (freq in 60f..1000f) PitchReading(freq, confianza, rms)
        else PitchReading(-1f, 0f, rms)
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
