package com.example.prueba.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.prueba.FretMindApp
import com.example.prueba.api.EjercicioDto
import com.example.prueba.api.PasoEjercicioDto
import com.example.prueba.audio.LivePracticeEngine
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Tipos de paso que se validan por detección de tono (monofónico, YIN). */
private val TIPOS_POR_PITCH = setOf("NOTE", "SEQUENCE", "STRING", "SCALE", "ARPEGGIO", "MELODY")

/** Frames consecutivos con la nota correcta para contar un acierto (~280 ms). */
private const val FRAMES_SOSTENIDOS = 3

enum class FaseVivo { PREPARANDO, CUENTA, PASO, TRANSICION, FINALIZADO, ERROR }
enum class FeedbackVivo { NEUTRO, ACIERTO, FALLO }

data class LiveState(
    val fase: FaseVivo = FaseVivo.PREPARANDO,
    val cuenta: Int = 3,
    val pasoIdx: Int = 0,
    val totalPasos: Int = 0,
    val titulo: String = "",
    val instruccion: String = "",
    val tipo: String = "",
    val porPitch: Boolean = true,
    val objetivoActual: String? = null,
    val notaDetectada: String? = null,
    val feedback: FeedbackVivo = FeedbackVivo.NEUTRO,
    val racha: Int = 0,
    val aciertosPaso: Int = 0,
    val esperadosPaso: Int = 0,
    val segundosRestantes: Int = 0,
    val segundosTotal: Int = 0,
    val progresoPaso: Float = 0f,
    val progresoGlobal: Float = 0f,
    val puntuacionViva: Int = 0,
    val error: String? = null
)

/** Resultado final de la sesión en vivo, listo para enviarse a /practica. */
data class ResultadoVivo(
    val wav: File?,
    val ejercicioId: String,
    val puntuacion: Double,       // 0-100
    val estrellas: Int,           // 0-3
    val notasAcertadas: Int,
    val notasTotales: Int,
    val duracionSeg: Int,
    val inicioIso: String,
    val detallePasosJson: String
)

private data class ResultadoPaso(
    val paso: PasoEjercicioDto,
    val aciertos: Int,
    val esperados: Int,
    val completado: Boolean
) {
    val puntuacion: Double
        get() = if (esperados > 0) (aciertos.toDouble() / esperados * 100).coerceIn(0.0, 100.0) else 0.0
}

class GuidedPracticeViewModel : ViewModel() {

    private val _state = MutableStateFlow(LiveState())
    val state: StateFlow<LiveState> = _state.asStateFlow()

    private val _resultado = MutableStateFlow<ResultadoVivo?>(null)
    val resultado: StateFlow<ResultadoVivo?> = _resultado.asStateFlow()

    // Los frames llegan del hilo de audio: se encolan y se procesan en orden.
    private val frames = MutableSharedFlow<LivePracticeEngine.LiveFrame>(
        extraBufferCapacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private var engine: LivePracticeEngine? = null
    private var pasos: List<PasoEjercicioDto> = emptyList()
    private var ejercicioId: String = ""
    private var inicioIso: String = ""
    private var framesJob: kotlinx.coroutines.Job? = null
    private var sesionJob: kotlinx.coroutines.Job? = null

    // Estado interno del paso en curso.
    private var objetivoIdx = 0
    private var framesEnObjetivo = 0
    private var framesEnError = 0
    private var ataquesPaso = 0
    private var pasoTerminado = false
    private val resultados = mutableListOf<ResultadoPaso>()

    fun iniciar(ejercicio: EjercicioDto) {
        val f = _state.value.fase
        // Sesión activa: no reiniciar. Sesión terminada/limpia: reset completo.
        if (f == FaseVivo.CUENTA || f == FaseVivo.PASO || f == FaseVivo.TRANSICION) return
        reset()

        pasos = ejercicio.pasos
        ejercicioId = ejercicio.id
        if (pasos.isEmpty()) {
            _state.value = LiveState(fase = FaseVivo.ERROR, error = "Este ejercicio no tiene pasos en vivo.")
            return
        }

        engine = LivePracticeEngine(FretMindApp.instance.cacheDir) { frame ->
            frames.tryEmit(frame)
        }

        framesJob = viewModelScope.launch { frames.collect { procesarFrame(it) } }

        sesionJob = viewModelScope.launch {
            // Cuenta regresiva 3-2-1.
            for (n in 3 downTo 1) {
                _state.value = _state.value.copy(fase = FaseVivo.CUENTA, cuenta = n, totalPasos = pasos.size)
                delay(1000)
            }
            inicioIso = ahoraIso()
            if (engine?.start() != true) {
                _state.value = _state.value.copy(fase = FaseVivo.ERROR, error = "No se pudo iniciar el micrófono.")
                return@launch
            }
            arrancarPaso(0)
            // Timer global de 1 s: descuenta el paso y cierra al agotarse.
            while (_state.value.fase == FaseVivo.PASO || _state.value.fase == FaseVivo.TRANSICION) {
                delay(1000)
                val s = _state.value
                if (s.fase != FaseVivo.PASO) continue
                val restantes = s.segundosRestantes - 1
                _state.value = s.copy(
                    segundosRestantes = restantes.coerceAtLeast(0),
                    segundosTotal = s.segundosTotal + 1,
                    progresoPaso = progresoPasoActual(restantes)
                )
                if (restantes <= 0) cerrarPaso()
            }
        }
    }

    private fun pasoActual(): PasoEjercicioDto = pasos[_state.value.pasoIdx]

    private fun esperadosDe(paso: PasoEjercicioDto): Int =
        if (paso.tipo in TIPOS_POR_PITCH) {
            paso.objetivos.size
        } else {
            // Actividad: pulsos esperados según bpm (o uno cada 2 s).
            val porBpm = paso.bpm?.let { paso.duracionSeg * it / 60 }
            (porBpm ?: (paso.duracionSeg / 2)).coerceAtLeast(4)
        }

    private fun progresoPasoActual(restantes: Int): Float {
        val paso = pasoActual()
        return if (paso.tipo in TIPOS_POR_PITCH) {
            if (paso.objetivos.isEmpty()) 0f
            else objetivoIdx.toFloat() / paso.objetivos.size
        } else {
            1f - restantes.toFloat() / paso.duracionSeg.coerceAtLeast(1)
        }
    }

    private fun arrancarPaso(idx: Int) {
        val paso = pasos[idx]
        objetivoIdx = 0
        framesEnObjetivo = 0
        framesEnError = 0
        ataquesPaso = 0
        pasoTerminado = false
        _state.value = _state.value.copy(
            fase = FaseVivo.PASO,
            pasoIdx = idx,
            titulo = paso.titulo,
            instruccion = paso.instruccion,
            tipo = paso.tipo,
            porPitch = paso.tipo in TIPOS_POR_PITCH,
            objetivoActual = paso.objetivos.firstOrNull(),
            notaDetectada = null,
            feedback = FeedbackVivo.NEUTRO,
            aciertosPaso = 0,
            esperadosPaso = esperadosDe(paso),
            segundosRestantes = paso.duracionSeg,
            progresoPaso = 0f,
            progresoGlobal = idx.toFloat() / pasos.size
        )
    }

    private fun procesarFrame(frame: LivePracticeEngine.LiveFrame) {
        val s = _state.value
        if (s.fase != FaseVivo.PASO || pasoTerminado) return
        val paso = pasoActual()

        if (paso.tipo in TIPOS_POR_PITCH) {
            val objetivo = paso.objetivos.getOrNull(objetivoIdx) ?: return
            val nota = frame.nota
            if (nota == null) {
                framesEnObjetivo = 0
                return
            }
            if (nota == objetivo) {
                framesEnObjetivo++
                framesEnError = 0
                if (framesEnObjetivo >= FRAMES_SOSTENIDOS) {
                    // Acierto: avanza al siguiente objetivo de la secuencia.
                    framesEnObjetivo = 0
                    objetivoIdx++
                    val aciertos = s.aciertosPaso + 1
                    _state.value = s.copy(
                        aciertosPaso = aciertos,
                        racha = s.racha + 1,
                        feedback = FeedbackVivo.ACIERTO,
                        notaDetectada = nota,
                        objetivoActual = paso.objetivos.getOrNull(objetivoIdx),
                        progresoPaso = objetivoIdx.toFloat() / paso.objetivos.size,
                        puntuacionViva = puntuacionViva(aciertos)
                    )
                    if (objetivoIdx >= paso.objetivos.size) cerrarPaso()
                }
            } else {
                framesEnObjetivo = 0
                framesEnError++
                // Nota equivocada sostenida: feedback de fallo (sin castigar doble).
                if (framesEnError == FRAMES_SOSTENIDOS) {
                    _state.value = s.copy(
                        feedback = FeedbackVivo.FALLO,
                        notaDetectada = nota,
                        racha = 0
                    )
                }
            }
        } else {
            // Validación por actividad: cuenta ataques (rasgueos/golpes).
            if (frame.ataque) {
                ataquesPaso++
                val esperados = s.esperadosPaso
                val aciertos = ataquesPaso.coerceAtMost(esperados)
                _state.value = s.copy(
                    aciertosPaso = aciertos,
                    racha = s.racha + 1,
                    feedback = FeedbackVivo.ACIERTO,
                    notaDetectada = frame.nota,
                    puntuacionViva = puntuacionViva(aciertos)
                )
            }
        }
    }

    private fun puntuacionViva(aciertosActuales: Int): Int {
        // Puntuación en vivo: pasos cerrados + progreso del paso actual.
        var totalXp = 0
        var logrado = 0.0
        resultados.forEach { r ->
            totalXp += r.paso.xp
            logrado += r.puntuacion / 100.0 * r.paso.xp
        }
        val paso = pasoActual()
        val esperados = esperadosDe(paso)
        if (esperados > 0) {
            totalXp += paso.xp
            logrado += (aciertosActuales.toDouble() / esperados).coerceAtMost(1.0) * paso.xp
        }
        // Los pasos aún no jugados también cuentan en el denominador.
        for (i in (_state.value.pasoIdx + 1) until pasos.size) totalXp += pasos[i].xp
        return if (totalXp > 0) ((logrado / totalXp) * 100).toInt() else 0
    }

    @Synchronized
    private fun cerrarPaso() {
        if (pasoTerminado) return
        pasoTerminado = true
        val s = _state.value
        val paso = pasoActual()
        val esperados = s.esperadosPaso
        val completado = if (paso.tipo in TIPOS_POR_PITCH) {
            s.aciertosPaso >= paso.objetivos.size
        } else {
            esperados > 0 && s.aciertosPaso.toDouble() / esperados >= 0.5
        }
        resultados.add(ResultadoPaso(paso, s.aciertosPaso, esperados, completado))

        viewModelScope.launch {
            if (s.pasoIdx + 1 < pasos.size) {
                _state.value = _state.value.copy(
                    fase = FaseVivo.TRANSICION,
                    feedback = if (completado) FeedbackVivo.ACIERTO else FeedbackVivo.NEUTRO,
                    progresoGlobal = (s.pasoIdx + 1).toFloat() / pasos.size
                )
                delay(1600)
                arrancarPaso(s.pasoIdx + 1)
            } else {
                finalizar()
            }
        }
    }

    private fun finalizar() {
        val wav = engine?.stop()
        engine = null

        val totalXp = resultados.sumOf { it.paso.xp }.coerceAtLeast(1)
        val puntuacion = resultados.sumOf { it.puntuacion / 100.0 * it.paso.xp } / totalXp * 100
        val estrellas = when {
            puntuacion >= 85 -> 3
            puntuacion >= 65 -> 2
            puntuacion >= 40 -> 1
            else -> 0
        }

        val detalle = JSONArray()
        resultados.forEach { r ->
            detalle.put(
                JSONObject()
                    .put("id", r.paso.id)
                    .put("tipo", r.paso.tipo)
                    .put("skill", r.paso.skill)
                    .put("difficulty", r.paso.difficulty)
                    .put("aciertos", r.aciertos)
                    .put("total", r.esperados)
                    .put("completado", r.completado)
                    .put("puntuacion", (r.puntuacion * 10).toInt() / 10.0)
            )
        }

        _resultado.value = ResultadoVivo(
            wav = wav,
            ejercicioId = ejercicioId,
            puntuacion = (puntuacion * 10).toInt() / 10.0,
            estrellas = estrellas,
            notasAcertadas = resultados.sumOf { it.aciertos },
            notasTotales = resultados.sumOf { it.esperados },
            duracionSeg = _state.value.segundosTotal,
            inicioIso = inicioIso,
            detallePasosJson = detalle.toString()
        )
        _state.value = _state.value.copy(fase = FaseVivo.FINALIZADO, progresoGlobal = 1f)
    }

    /** Reset síncrono completo (antes de una nueva sesión o al cancelar). */
    private fun reset() {
        framesJob?.cancel()
        sesionJob?.cancel()
        framesJob = null
        sesionJob = null
        engine?.discard()
        engine = null
        resultados.clear()
        objetivoIdx = 0
        framesEnObjetivo = 0
        framesEnError = 0
        ataquesPaso = 0
        pasoTerminado = false
        _resultado.value = null
        _state.value = LiveState()
    }

    /** Cancela la sesión descartando audio y resultados. */
    fun cancelar() {
        reset()
    }

    override fun onCleared() {
        engine?.discard()
        engine = null
    }

    private fun ahoraIso(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'+00:00'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }
}
