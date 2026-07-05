package com.example.prueba.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.concurrent.thread

/**
 * Graba audio del micrófono (PCM 16-bit mono) manteniendo solo los últimos
 * [maxSeconds] segundos en un buffer rodante en memoria. Al detener, escribe
 * un archivo WAV en [outputDir] listo para enviar a POST /practica.
 *
 * El permiso RECORD_AUDIO debe estar concedido ANTES de llamar a start().
 */
class WavRecorder(
    private val outputDir: File,
    private val sampleRate: Int = 44100,
    maxSeconds: Int = 30
) {
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

        val rec = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            minBuffer
        )
        if (rec.state != AudioRecord.STATE_INITIALIZED) {
            rec.release()
            return false
        }
        record = rec
        running = true
        rec.startRecording()

        worker = thread(name = "WavRecorder") {
            while (running) {
                val buffer = ShortArray(chunkSize)
                val read = rec.read(buffer, 0, chunkSize)
                if (read > 0) {
                    val chunk = if (read == chunkSize) buffer else buffer.copyOf(read)
                    synchronized(lock) {
                        chunks.addLast(chunk)
                        while (chunks.size > maxChunks) chunks.removeFirst()
                    }
                }
            }
        }
        return true
    }

    /** Detiene la captura y escribe el WAV. Devuelve null si no se capturó audio. */
    fun stop(): File? {
        stopCapture()
        val samples = synchronized(lock) {
            val total = chunks.sumOf { it.size }
            if (total == 0) return null
            val all = ShortArray(total)
            var pos = 0
            chunks.forEach { chunk ->
                chunk.copyInto(all, pos)
                pos += chunk.size
            }
            chunks.clear()
            all
        }
        return writeWav(samples)
    }

    /** Detiene la captura y descarta el audio acumulado (sin escribir archivo). */
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

    private fun writeWav(samples: ShortArray): File {
        val dataSize = samples.size * 2
        val file = File(outputDir, "practica_${System.currentTimeMillis()}.wav")

        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        header.put("RIFF".toByteArray())
        header.putInt(36 + dataSize)
        header.put("WAVE".toByteArray())
        header.put("fmt ".toByteArray())
        header.putInt(16)                      // tamaño del subchunk fmt
        header.putShort(1)                     // PCM
        header.putShort(1)                     // mono
        header.putInt(sampleRate)
        header.putInt(sampleRate * 2)          // byte rate (mono, 16 bits)
        header.putShort(2)                     // block align
        header.putShort(16)                    // bits por muestra
        header.put("data".toByteArray())
        header.putInt(dataSize)

        val pcm = ByteBuffer.allocate(dataSize).order(ByteOrder.LITTLE_ENDIAN)
        samples.forEach { pcm.putShort(it) }

        FileOutputStream(file).use { out ->
            out.write(header.array())
            out.write(pcm.array())
        }
        return file
    }
}
