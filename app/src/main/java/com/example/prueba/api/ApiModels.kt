package com.example.prueba.api

import com.google.gson.annotations.SerializedName

// ==========================================
// GENERIC API WRAPPER
// ==========================================
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: String? = null
)

// ==========================================
// TUNER DOMAIN
// ==========================================

data class PitchRequest(val frecuencia: Float)

data class VerifyRequest(val frecuencia: Float, val notaEsperada: String)

data class ChordRequest(val notas: String)

data class PitchInfo(
    val nota: String,
    val octava: Int,
    val afinado: Boolean,
    val cents: Double,
    val frecuencia: Double,
    val freqTeorica: Double
)

data class VerifyInfo(
    val nota: String,
    val notaEsperada: String,
    val coincide: Boolean,
    val afinado: Boolean,
    val cents: Double
)

data class ChordInfo(
    val acorde: String,
    val tipo: String,
    val raiz: String,
    val notas: List<String>
)

data class PitchFileInfo(
    val frecuencia: Double,
    val nota: String,
    val afinado: Boolean,
    val cents: Double,
    val confianza: Double?,
    val armonicos: Any?
)

data class ChordFileInfo(
    val acorde: String,
    val tipo: String,
    val notas: List<String>
)

data class AnalyzeInfo(
    val nota: String,
    val frecuencia: Double,
    val frecuenciaTeorica: Double,
    val cents: Double,
    val cuerda: String?,
    val afinado: Boolean,
    val direccion: String
)

data class AnalyzeFileInfo(
    val frecuencia: Double,
    val frecuenciaPromedio: Double,
    val nota: String,
    val cents: Double,
    val cuerda: String?,
    val confianza: Double,
    val estabilidad: Double,
    val framesAnalizados: Int,
    @SerializedName("hay_señal") val haySenal: Boolean,
    val afinado: Boolean,
    val direccion: String
)

data class RawFrame(
    val frecuencia: Double,
    val nota: String,
    val cents: Double,
    val confianza: Double,
    val cuerda: String?
)

data class AnalyzeRawInfo(
    val frames: List<RawFrame>,
    val totalFrames: Int,
    val frameDurationMs: Int
)

data class CalibrateInfo(
    val calibrado: Boolean,
    val mensaje: String
)

data class PipelineInfo(
    val pipeline: List<PipelineStage>,
    val algoritmosPitch: List<String>,
    val estabilizacion: String,
    val cuerdasSoportadas: List<String>,
    val toleranciaAfinacion: String
)

data class PipelineStage(
    val etapa: Int,
    val nombre: String,
    val proposito: String,
    val tipo: String
)

// ==========================================
// WILFREDO DOMAIN
// ==========================================

data class WilfredoChatRequest(
    val mensaje: String,
    val nivel: String = "principiante",
    val historial: List<String> = emptyList()
)

data class WilfredoChatInfo(
    val respuesta: String,
    val nivel: String
)

data class WilfredoAnalyzeRequest(
    val precision: Float,
    val ritmo: Float = 0f,
    val bpm: Int = 120,
    val nota: String? = null
)

data class WilfredoAnalyzeInfo(
    val feedback: String,
    val precision: Float,
    val ritmo: Float
)

data class WilfredoPlanRequest(
    val nivel: String = "principiante",
    val objetivo: String? = null
)

data class WilfredoPlanInfo(
    val ejercicios: List<String>,
    val duracion: String,
    val consejo: String
)

data class WilfredoTunerRequest(
    val frecuencia: Float,
    val nota: String,
    val cents: Float
)

data class WilfredoTunerInfo(
    val feedback: String,
    val nota: String,
    val cents: Float,
    val afinado: Boolean
)

data class WilfredoChordRequest(
    val notas: List<String>
)

data class WilfredoChordInfo(
    val feedback: String,
    val acorde: String,
    val notas: List<String>
)

// ==========================================
// PRACTICA DOMAIN
// ==========================================

data class PracticaMetrics(
    val precision: Double,
    val consistencia: Double,
    val error: Double
)

data class PracticaProfile(
    val precisionAvg: Double,
    val consistenciaAvg: Double,
    val errorAvg: Double
)

data class PracticaResult(
    val metrics: PracticaMetrics,
    val profile: PracticaProfile
)

data class PracticaAnalyzeInfo(
    val pitchMean: Double,
    val energy: Double,
    val brightness: Double,
    val tempo: Double
)

// ==========================================
// SONGSTERR DOMAIN
// ==========================================

data class SongsterrTrack(
    val instrumentId: Int,
    val instrument: String,
    val views: Int?,
    val name: String?,
    val tuning: List<Int>?,
    val difficulty: Int?,
    val hash: String?
)

data class SongsterrSong(
    val songId: Long,
    val artistId: Int,
    val artist: String,
    val title: String,
    val hasChords: Boolean,
    val hasPlayer: Boolean,
    val tracks: List<SongsterrTrack>,
    val defaultTrack: Int,
    val popularTrack: Int,
    val isJunk: Boolean,
    val popularTrackGuitar: Int?,
    val popularTrackBass: Int?,
    val popularTrackDrum: Int?,
    val popularTrackVocals: Int?
)
