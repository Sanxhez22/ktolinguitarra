package com.example.prueba.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import java.io.File
import kotlin.concurrent.thread
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Motor de práctica guiada en vivo (estilo Yousician).
 *
 * UN SOLO AudioRecord hace las dos cosas a la vez (dos capturas simultáneas
 * del micrófono no son posibles):
 *  - análisis en tiempo real por ventana: pitch (YIN), energía RMS y
 *    detección de ataques (rasgueos/pulsaciones) -> callback [onFrame]
 *  - acumulación del PCM en un buffer rodante para el WAV final que el
 *    backend analiza como ground truth (igual que WavRecorder)
 *
 * El permiso RECORD_AUDIO debe estar concedido ANTES de llamar a start().
 */
class LivePracticeEngine(
    private val outputDir: File,
    private val sampleRate: Int = 44100,
    maxSeconds: Int = 60,
    private val onFrame: (LiveFrame) -> Unit
) {
    /**
     * Lectura de una ventana de análisis (~93 ms a 44.1 kHz):
     * @param freqHz      frecuencia fundamental detectada (-1 si no hay tono)
     * @param nota        nombre de la nota con octava ("A2") o null; solo se
     *                    entrega cuando la claridad MPM supera el umbral de
     *                    confianza (las lecturas dudosas no cuentan notas)
     * @param confianza   claridad MPM 0..1 de la detección de pitch
     * @param rms         energía de la ventana (0..1)
     * @param ataque      true si esta ventana contiene un ataque (golpe/rasgueo)
     * @param acorde      acorde detectado por croma ("Am") o null si la
     *                    ventana no suena a acorde claro
     * @param acordeScore similitud 0..1 del acorde detectado
     */
    data class LiveFrame(
        val freqHz: Float,
        val nota: String?,
        val confianza: Float = 0f,
        val rms: Float,
        val ataque: Boolean,
        val acorde: String? = null,
        val acordeScore: Float = 0f
    )

    @Volatile private var running = false
    private var record: AudioRecord? = null
    private var worker: Thread? = null

    private val chunkSize = 4096
    private val maxChunks = (sampleRate * maxSeconds) / chunkSize
    private val chunks = ArrayDeque<ShortArray>()
    private val lock = Any()

    @SuppressLint("MissingPermission") // El llamador garantiza RECORD_AUDIO concedido.
    fun start(): Boolean {
        if (running) return true

        val minBuffer = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(chunkSize * 2)

        // Misma cascada de fuentes que el afinador (UNPROCESSED primero):
        // con MIC a secas el procesamiento del OEM se comía el fundamental
        // de las cuerdas graves y el fingerpicking en E2/A2 no se reconocía.
        val rec = abrirAudioRecordPreferido(sampleRate, minBuffer) ?: return false
        record = rec
        running = true
        rec.startRecording()

        worker = thread(name = "LivePracticeEngine") {
            val detector = MpmPitchDetector(sampleRate.toFloat(), chunkSize)
            val acordes = ChordDetector(sampleRate.toFloat(), chunkSize)
            var rmsAnterior = 0f
            var muestrasDesdeAtaque = Long.MAX_VALUE / 2
            val gapMinAtaque = (sampleRate * 0.18f).toLong() // 180 ms entre ataques

            while (running) {
                val buffer = ShortArray(chunkSize)
                val read = rec.read(buffer, 0, chunkSize)
                if (read <= 0) continue

                // 1) Acumular para el WAV final (buffer rodante).
                val chunk = if (read == chunkSize) buffer else buffer.copyOf(read)
                synchronized(lock) {
                    chunks.addLast(chunk)
                    while (chunks.size > maxChunks) chunks.removeFirst()
                }

                // 2) Análisis en vivo de la misma ventana.
                var sumSq = 0.0
                for (i in 0 until read) {
                    val v = buffer[i] / 32768f
                    sumSq += (v * v).toDouble()
                }
                val rms = sqrt(sumSq / read).toFloat()

                // Ataque: salto claro de energía tras un mínimo de silencio
                // relativo, con separación mínima entre ataques.
                muestrasDesdeAtaque += read
                val esAtaque = rms > 0.03f &&
                    rms > rmsAnterior * 1.8f &&
                    muestrasDesdeAtaque >= gapMinAtaque
                if (esAtaque) muestrasDesdeAtaque = 0
                rmsAnterior = rms

                val lectura = detector.detectReading(buffer, read)
                val acorde = acordes.detectar(buffer, read)
                // La nota solo cuenta con claridad suficiente: una lectura
                // dudosa (ataque sucio, armónico suelto) no debe sumar ni
                // resetear los contadores de acierto de la práctica.
                val notaConfiable = lectura.freqHz > 0f &&
                    lectura.confidence >= CONFIANZA_MIN_NOTA
                onFrame(
                    LiveFrame(
                        freqHz = lectura.freqHz,
                        nota = if (notaConfiable) freqToNoteName(lectura.freqHz) else null,
                        confianza = lectura.confidence,
                        rms = rms,
                        ataque = esAtaque,
                        acorde = acorde?.acorde,
                        acordeScore = acorde?.score ?: 0f
                    )
                )
            }
        }
        return true
    }

    /** Detiene la captura y escribe el WAV de la sesión. Null si no hay audio. */
    fun stop(): File? {
        stopCapture()
        val samples = synchronized(lock) {
            val total = chunks.sumOf { it.size }
            if (total == 0) return null
            val all = ShortArray(total)
            var pos = 0
            chunks.forEach { c ->
                c.copyInto(all, pos)
                pos += c.size
            }
            chunks.clear()
            all
        }
        return PcmWav.escribir(outputDir, samples, sampleRate)
    }

    /** Detiene y descarta el audio acumulado. */
    fun discard() {
        stopCapture()
        synchronized(lock) { chunks.clear() }
    }

    private fun stopCapture() {
        running = false
        worker?.join(500)
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

/** Claridad MPM mínima para que un frame de práctica reporte nota. */
private const val CONFIANZA_MIN_NOTA = 0.55f

private val NOMBRES_NOTA =
    listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

/** Nombre de nota con octava ("A2") de una frecuencia en Hz. */
fun freqToNoteName(freq: Float): String {
    val midi = (69.0 + 12.0 * (ln(freq / 440.0) / ln(2.0))).roundToInt()
    return NOMBRES_NOTA[((midi % 12) + 12) % 12] + (midi / 12 - 1)
}

private val NOTA_REGEX = Regex("^([A-G]#?)(-?\\d+)$")

/** Frecuencia en Hz de una nota con octava ("D3" -> 146.83), o null si no parsea. */
fun noteNameToFreq(nota: String): Float? {
    val m = NOTA_REGEX.find(nota) ?: return null
    val idx = NOMBRES_NOTA.indexOf(m.groupValues[1])
    if (idx < 0) return null
    val midi = (m.groupValues[2].toInt() + 1) * 12 + idx
    return (440.0 * Math.pow(2.0, (midi - 69) / 12.0)).toFloat()
}
