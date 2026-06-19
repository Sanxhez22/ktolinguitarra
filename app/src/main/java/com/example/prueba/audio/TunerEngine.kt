package com.example.prueba.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlin.concurrent.thread
import kotlin.math.sqrt

/**
 * Captura audio del micrófono con AudioRecord y detecta la frecuencia
 * fundamental en tiempo real usando el algoritmo YIN.
 *
 * El permiso RECORD_AUDIO debe estar concedido ANTES de llamar a start().
 *
 * @param onPitch callback invocado por cada ventana analizada con la
 *                frecuencia en Hz, o -1f si no se detecta tono válido.
 */
class TunerEngine(
    private val sampleRate: Int = 44100,
    private val analysisSize: Int = 4096,
    private val onPitch: (Float) -> Unit
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
            val buffer = ShortArray(analysisSize)
            val detector = YinPitchDetector(sampleRate.toFloat(), analysisSize)
            while (running) {
                val read = rec.read(buffer, 0, analysisSize)
                if (read > 0) {
                    onPitch(detector.detect(buffer, read))
                }
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
 * Detector de tono YIN (de Cheveigné & Kawahara, 2002).
 * Devuelve la frecuencia fundamental en Hz o -1f si no hay tono fiable.
 */
class YinPitchDetector(
    private val sampleRate: Float,
    bufferSize: Int,
    private val threshold: Float = 0.15f
) {
    private val halfSize = bufferSize / 2
    private val diff = FloatArray(halfSize)
    private val cmnd = FloatArray(halfSize)
    private val signal = FloatArray(bufferSize)

    fun detect(samples: ShortArray, length: Int): Float {
        val n = if (length < signal.size) length else signal.size

        // Normaliza a [-1,1] y calcula RMS para descartar silencio/ruido bajo.
        var sumSq = 0.0
        for (i in 0 until n) {
            val v = samples[i] / 32768f
            signal[i] = v
            sumSq += v.toDouble() * v
        }
        val rms = sqrt(sumSq / n)
        if (rms < 0.01) return -1f

        val tauMax = n / 2
        if (tauMax < 4) return -1f

        // 1) Función de diferencia.
        for (tau in 0 until tauMax) {
            var sum = 0f
            for (j in 0 until tauMax) {
                val d = signal[j] - signal[j + tau]
                sum += d * d
            }
            diff[tau] = sum
        }

        // 2) Diferencia media acumulada normalizada.
        cmnd[0] = 1f
        var running = 0f
        for (tau in 1 until tauMax) {
            running += diff[tau]
            cmnd[tau] = if (running == 0f) 1f else diff[tau] * tau / running
        }

        // 3) Umbral absoluto: primer mínimo bajo el umbral.
        var tauEstimate = -1
        var tau = 2
        while (tau < tauMax) {
            if (cmnd[tau] < threshold) {
                while (tau + 1 < tauMax && cmnd[tau + 1] < cmnd[tau]) tau++
                tauEstimate = tau
                break
            }
            tau++
        }
        if (tauEstimate == -1) return -1f

        // 4) Interpolación parabólica para refinar el periodo.
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
        // Rango útil para guitarra (E2 ~82 Hz; deja margen para armónicos).
        return if (freq in 60f..1200f) freq else -1f
    }
}
