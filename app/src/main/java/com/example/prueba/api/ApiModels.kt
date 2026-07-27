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
    val historial: List<String> = emptyList(),
    // Con userId, Wilfredo responde como tutor con memoria de desempeño.
    val userId: String? = null
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

// Actualización de una habilidad tras una práctica (P1).
data class HabilidadUpdateDto(
    val habilidad: String,
    val nombre: String,
    val delta: Double = 0.0,
    val nivel: Int = 0,
    val progreso: Double = 0.0,
    val confianza: Double = 0.0,
    val subioNivel: Boolean = false
)

data class PasoCompletadoDto(
    val id: String,
    val nombre: String
)

// Resumen del ExerciseAttempt registrado por el backend.
data class IntentoDto(
    val id: String? = null,
    val xpGanado: Int = 0,
    val puntuacion: Double? = null,
    val estrellas: Int? = null,
    // Progreso real del ejercicio en vivo (aditivos: null en backend previo).
    val notasAcertadas: Int? = null,
    val notasTotales: Int? = null
)

data class PracticaResult(
    val metrics: PracticaMetrics,
    val profile: PracticaProfile,
    // Campos P1 (aditivos): pueden faltar si el backend es P0.
    val aprobado: Boolean? = null,
    val criterios: CriteriosDto? = null,
    val habilidadesActualizadas: List<HabilidadUpdateDto> = emptyList(),
    val subioNivel: Boolean = false,
    val pasoCompletado: PasoCompletadoDto? = null,
    val intento: IntentoDto? = null
)

data class PracticaAnalyzeInfo(
    val pitchMean: Double,
    val energy: Double,
    val brightness: Double,
    val tempo: Double
)

// ==========================================
// PROGRESO DOMAIN
// ==========================================

// Sesión individual dentro del historial de /progreso/{user_id}.
data class SesionHistorialDto(
    val fecha: String? = null,
    val ejercicio: String? = null,
    val precision: Double = 0.0,
    val consistencia: Double = 0.0,
    val duracionSeg: Int = 0
)

// Respuesta de GET /progreso/{user_id}: todo agregado desde MongoDB.
data class ProgresoDto(
    val userId: String,
    val nivel: String = "principiante",
    val sesiones: Int = 0,
    val precisionPromedio: Double = 0.0,
    val consistenciaPromedio: Double = 0.0,
    val rachaDias: Int = 0,
    val minutosHoy: Double = 0.0,
    val minutosTotales: Double = 0.0,
    val ejerciciosCompletados: Int = 0,
    val ultimaPractica: String? = null,
    val historial: List<SesionHistorialDto> = emptyList()
)

// ==========================================
// P1 - HABILIDADES / CAMINO / ENTRENADOR
// ==========================================

data class HabilidadDto(
    val id: String,
    val nombre: String,
    val nivel: Int = 0,
    val progreso: Double = 0.0,
    val confianza: Double = 0.0,
    val intentos: Int = 0,
    val ultimaPractica: String? = null,
    val valor: Double = 0.0
)

data class HabilidadesResponse(
    val usuario: String,
    val habilidades: List<HabilidadDto> = emptyList(),
    val debiles: List<String> = emptyList(),
    val fuertes: List<String> = emptyList(),
    val actualizado: String? = null
)

data class PasoCaminoDto(
    val id: String,
    val nombre: String,
    val descripcion: String,
    val habilidad: String,
    val ejercicios: List<String> = emptyList(),
    val estado: String,                 // bloqueado | disponible | en_curso | completado
    val fechaCompletado: String? = null
)

data class CaminoResponse(
    val usuario: String,
    val pasoActual: String,
    val pasos: List<PasoCaminoDto> = emptyList(),
    val refuerzosPendientes: List<String> = emptyList(),
    val completados: Int = 0,
    val totalPasos: Int = 0
)

// Paso de práctica guiada en vivo (11 tipos: NOTE, SEQUENCE, RHYTHM, CHORD,
// CHORD_CHANGE, STRING, SCALE, ARPEGGIO, MELODY, SONG_FRAGMENT, CUSTOM).
data class PasoEjercicioDto(
    val id: String,
    val titulo: String,
    val instruccion: String,
    val tipo: String,
    val objetivos: List<String> = emptyList(),
    val duracionSeg: Int = 30,
    val bpm: Int? = null,
    val difficulty: Int = 1,
    val xp: Int = 10,
    val skill: String = ""
)

data class EjercicioDto(
    val id: String,
    val nombre: String,
    val emoji: String = "🎸",
    val descripcion: String = "",
    val habilidad: String = "",
    val secundarias: List<String> = emptyList(),
    val paso: String? = null,
    val dificultad: Int = 1,
    val duracionMin: Int = 10,
    val objetivo: String = "",
    val criterios: CriteriosDto? = null,
    val pasos: List<PasoEjercicioDto> = emptyList()
)

data class CriteriosDto(
    val precisionMin: Double = 0.0,
    val consistenciaMin: Double = 0.0,
    val duracionMinSeg: Int = 0
)

// --- Entrenador (Home) ---

data class RecomendacionDto(
    val tipo: String,                   // practicar | repetir | subir_dificultad | bajar_dificultad | descanso
    val habilidad: String? = null,
    val ejercicioId: String? = null,
    val dificultad: Int? = null,
    val duracionMin: Int? = null,
    val razon: String = ""
)

data class CelebracionDto(
    val tipo: String,
    val titulo: String,
    val mensaje: String
)

data class ObjetivoDiaDto(
    val metaMin: Int = 15,
    val minutosHoy: Double = 0.0,
    val restanteMin: Double = 0.0,
    val cumplido: Boolean = false
)

data class HabilidadResumenDto(
    val id: String,
    val nombre: String,
    val nivel: Int = 0,
    val confianza: Double = 0.0,
    val valor: Double = 0.0
)

data class ProximoLogroDto(
    val tipo: String,
    val titulo: String,
    val detalle: String,
    val paso: String? = null
)

data class EntrenadorResponse(
    val usuario: String,
    val recomendacion: RecomendacionDto,
    val ejercicio: EjercicioDto? = null,
    val celebracion: CelebracionDto? = null,
    val consejo: String = "",
    val objetivoDia: ObjetivoDiaDto = ObjetivoDiaDto(),
    val racha: Int = 0,
    val habilidadesDebiles: List<HabilidadResumenDto> = emptyList(),
    val habilidadesFuertes: List<HabilidadResumenDto> = emptyList(),
    val proximoLogro: ProximoLogroDto? = null,
    val pasoActual: String? = null
)

// ==========================================
// MOTOR COGNITIVO (RIFF)
// ==========================================

// Hito detectado por Progress Intelligence (logro/evolución/estancamiento/recaída).
data class HitoDto(
    val clave: String,
    val tipo: String,
    val titulo: String,
    val detalle: String = "",
    val fecha: String? = null
)

data class HitosResponse(
    val usuario: String,
    val hitos: List<HitoDto> = emptyList()
)

data class PlanItemDto(
    val orden: Int,
    val tipo: String,                 // ejercicio | cancion | descanso
    val ejercicioId: String? = null,
    val titulo: String = "",
    val emoji: String = "🎸",
    val cancionId: Long? = null,
    val duracionMin: Int = 0,
    val xpPotencial: Int = 0,
    val razon: String = "",
    val completado: Boolean = false
)

data class PlanDiarioDto(
    val fecha: String = "",
    val items: List<PlanItemDto> = emptyList(),
    val duracionTotalMin: Int = 0,
    val xpPotencial: Int = 0,
    val metaMin: Int = 15
)

// ==========================================
// AUTH DOMAIN
// ==========================================

data class GoogleLoginRequest(val idToken: String)

// Cuerpo de POST /auth/onboarding/{user_id}. Todos los campos opcionales:
// se envían las respuestas al terminar las preguntas y `completado` al final.
data class OnboardingRequest(
    val experiencia: String? = null,
    val objetivo: String? = null,
    val completado: Boolean? = null,
    val afinacionOmitida: Boolean? = null
)

data class EstadisticasDto(
    val sesiones: Int = 0,
    val precisionPromedio: Double = 0.0
)

// Perfil devuelto por /auth/google y /auth/perfil/{id}.
// Gson mapea snake_case del backend (fecha_registro, precision_promedio...).
data class UserProfileDto(
    val id: String,
    val nombre: String? = null,
    val email: String? = null,
    val foto: String? = null,
    val nivel: String = "principiante",
    val experiencia: String? = null,
    val objetivo: String? = null,
    val onboardingCompletado: Boolean = false,
    val afinacionOmitida: Boolean = false,
    val fechaRegistro: String? = null,
    val ultimaSesion: String? = null,
    val estadisticas: EstadisticasDto? = null
)

// ==========================================
// CANCIONES (Song Detail) - DTOs propios de FretMind
// Toda la integración con Songsterr/iTunes vive en el backend
// (/canciones, /biblioteca); Android solo consume estos modelos.
// ==========================================

data class CancionResumenDto(
    val songId: Long,
    val titulo: String,
    val artista: String,
    val dificultad: String? = null,      // Fácil | Intermedio | Avanzado
    val tieneAcordes: Boolean = false,
    val tienePlayer: Boolean = false,
    val pistas: Int = 0
)

data class BusquedaCancionesResponse(
    val canciones: List<CancionResumenDto> = emptyList(),
    val cantidad: Int = 0,
    val desde: Int = 0,
    val size: Int = 10,
    val hayMas: Boolean = false
)

data class PistaDto(
    val instrumento: String,
    val nombre: String? = null,
    val afinacion: String? = null,
    val dificultad: String? = null,
    val esVoz: Boolean = false,
    val vistas: Int? = null
)

data class CancionDetalleDto(
    val songId: Long,
    val titulo: String,
    val artista: String,
    // Enriquecimiento iTunes (null si no hubo match confiable).
    val portada: String? = null,
    val genero: String? = null,
    val duracionSeg: Int? = null,
    val album: String? = null,
    // Metadata Songsterr.
    val afinacion: String? = null,
    val dificultad: String? = null,
    val tieneAcordes: Boolean = false,
    val tienePlayer: Boolean = false,
    val descripcion: String? = null,
    val autor: String? = null,
    val vistas: Int? = null,
    val favoritos: Int? = null,
    val tags: List<String> = emptyList(),
    val pistas: List<PistaDto> = emptyList(),
    val pistaDefault: Int = 0,
    val videos: List<String> = emptyList(),
    val enlaceTab: String = ""
)

data class BibliotecaItemDto(
    val songId: Long,
    val titulo: String = "",
    val artista: String = "",
    val portada: String? = null,
    val guardada: Boolean = false,
    val favorita: Boolean = false,
    val practicas: Int = 0,
    val fechaAgregada: String? = null,
    val ultimaPractica: String? = null
)

data class BibliotecaResponse(
    val usuario: String,
    val items: List<BibliotecaItemDto> = emptyList(),
    val total: Int = 0
)

data class BibliotecaUpdateRequest(
    val guardada: Boolean? = null,
    val favorita: Boolean? = null
)

data class CancionMiniDto(
    val songId: Long,
    val titulo: String,
    val artista: String,
    val portada: String? = null
)

// Respuesta de GET /canciones/{id}/plan y POST /canciones/{id}/practicar.
data class PlanCancionDto(
    val cancion: CancionMiniDto,
    val ejercicio: EjercicioDto,
    val objetivos: List<String> = emptyList(),
    val consejo: String = "",
    val duracionSugeridaMin: Int = 15
)

// --- Práctica guiada de canción (GET /canciones/{id}/practica_guiada) ---

// Una línea de la canción: texto (vacío en modo solo-acordes), tiempo de
// entrada en segundos y el acorde de práctica que suena encima.
data class LineaCancionDto(
    val texto: String = "",
    val tSeg: Double? = null,
    val acorde: String? = null
)

data class SeccionCancionDto(
    val nombre: String = "",
    val lineas: List<LineaCancionDto> = emptyList()
)

data class PracticaGuiadaCancionDto(
    val cancion: CancionMiniDto,
    val tieneLetra: Boolean = false,
    val sincronizada: Boolean = false,
    val bpm: Int = 60,
    val progresion: List<String> = emptyList(),
    val secciones: List<SeccionCancionDto> = emptyList(),
    val consejo: String = "",
    val ejercicioId: String = "primera_cancion",
    val nota: String = ""
)
