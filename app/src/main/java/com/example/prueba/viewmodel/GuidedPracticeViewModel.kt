package com.example.prueba.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.prueba.FretMindApp
import com.example.prueba.api.EjercicioDto
import com.example.prueba.api.PasoEjercicioDto
import com.example.prueba.audio.ChordDetector
import com.example.prueba.audio.LivePracticeEngine
import com.example.prueba.audio.noteNameToFreq
import com.example.prueba.data.model.AcordeForma
import com.example.prueba.data.model.CatalogoAcordes
import com.example.prueba.data.model.instruccionColocacion
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.roundToInt
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

/** Tipos de paso que se validan por detección de tono (monofónico, MPM). */
private val TIPOS_POR_PITCH = setOf("NOTE", "SEQUENCE", "STRING", "SCALE", "ARPEGGIO", "MELODY")

/** Tipos de paso que se validan reconociendo el ACORDE tocado (croma). */
private val TIPOS_VALIDA_ACORDE = setOf("CHORD", "CHORD_CHANGE")

/** Frames consecutivos con la nota correcta para contar un acierto (~280 ms). */
private const val FRAMES_SOSTENIDOS = 3

/**
 * Frames sostenidos exigidos por TIPO de paso (cada ejercicio valida con
 * su propio criterio, sin mezclar reglas):
 *
 *  - ARPEGGIO (arpegios / fingerpicking): la nota punteada decae rápido y
 *    la siguiente cuerda llega enseguida; exigir ~280 ms hacía perder
 *    notas bien tocadas. Bastan 2 frames confiables (~190 ms).
 *  - STRING (afinación), NOTE, SCALE, MELODY y SEQUENCE: nota sostenida,
 *    3 frames (~280 ms) para confirmar que la nota suena de verdad.
 */
private fun framesSostenidosDe(tipo: String): Int = when (tipo) {
    "ARPEGGIO" -> 2
    else -> FRAMES_SOSTENIDOS
}

/** Frames consecutivos con el acorde correcto para contar un rasgueo válido. */
private const val FRAMES_ACORDE = 2

/** Rasgueos correctos que completan un paso CHORD. */
private const val RASGUEOS_POR_ACORDE = 4

/** Vueltas a la secuencia que completan un paso CHORD_CHANGE. */
private const val RONDAS_CAMBIO = 2

/**
 * Tolerancia en cents para validar una cuerda al aire en pasos STRING.
 * Es el criterio de "cuerda afinada" de la práctica: más estricto que el
 * viejo 35 (dejaba pasar cuerdas audiblemente desafinadas), más laxo que
 * los ±5 del afinador (aquí se valida con una sola pasada del mic).
 */
private const val TOLERANCIA_CENTS_STRING = 20.0

/** Tipos de paso que muestran la guía visual del acorde antes de detectar. */
private val TIPOS_ACORDE = setOf("CHORD", "CHORD_CHANGE")

/** Ritmo de la guía: revelado de cada dedo y pausa para acomodar la mano. */
private const val MS_GUIA_POR_DEDO = 2200L
private const val MS_GUIA_ACOMODAR = 3000L
// La guía del cambio anima cada acorde dedo a dedo: tiempo para ver un
// ciclo completo de 2-3 formas antes de que arranque la detección.
private const val MS_GUIA_CAMBIO = 9000L

/** Guía previa de los pasos que no son de acorde (explicación + diagrama). */
private const val MS_GUIA_PREVIA = 7000L

/**
 * Cents de desviación de la frecuencia detectada respecto a la nota objetivo,
 * IGNORANDO la octava (los armónicos pueden hacer saltar de octava al
 * detector en algunas cuerdas al aire). Null si no hay tono o el objetivo
 * no parsea.
 */
private fun centsVsObjetivo(freqHz: Float, objetivo: String): Double? {
    if (freqHz <= 0f) return null
    val objetivoHz = noteNameToFreq(objetivo) ?: return null
    val semitonos = 12.0 * ln(freqHz.toDouble() / objetivoHz) / ln(2.0)
    return (semitonos - (semitonos / 12.0).roundToInt() * 12.0) * 100.0
}

/**
 * ¿El frame detectado cumple el objetivo del paso?
 *
 * STRING: frecuencia contra la nota objetivo con tolerancia en cents e
 * ignorando octava — es el paso de AFINACIÓN: la cuerda se da por buena
 * solo si además de ser la nota correcta está afinada.
 *
 * Resto de tipos por pitch: igualdad exacta nota+octava.
 */
private fun cumpleObjetivo(frame: LivePracticeEngine.LiveFrame, objetivo: String, tipo: String): Boolean {
    if (tipo != "STRING") return frame.nota == objetivo
    val cents = centsVsObjetivo(frame.freqHz, objetivo) ?: return frame.nota == objetivo
    return abs(cents) <= TOLERANCIA_CENTS_STRING
}

enum class FaseVivo { PREPARANDO, CUENTA, GUIA, PASO, TRANSICION, FINALIZADO, ERROR }
enum class FeedbackVivo { NEUTRO, ACIERTO, FALLO }

/** Clasificación temporal de un golpe en pasos de ritmo. */
enum class TimingVivo { NINGUNO, A_TIEMPO, ADELANTADO, ATRASADO }

data class LiveState(
    val fase: FaseVivo = FaseVivo.PREPARANDO,
    val cuenta: Int = 3,
    val pasoIdx: Int = 0,
    val totalPasos: Int = 0,
    val titulo: String = "",
    val instruccion: String = "",
    val tipo: String = "",
    val skill: String = "",
    val porPitch: Boolean = true,
    val objetivoActual: String? = null,
    // Datos del paso para las representaciones visuales por tipo.
    val objetivos: List<String> = emptyList(),
    val objetivoIdx: Int = 0,
    val bpm: Int? = null,
    val notaDetectada: String? = null,
    // Acorde reconocido por croma (pasos CHORD/CHORD_CHANGE/SONG_FRAGMENT).
    val acordeDetectado: String? = null,
    // Desviación en cents del paso STRING (para el mini-afinador de la vista).
    val centsDetectados: Float? = null,
    // Reloj de ritmo del ViewModel (RHYTHM/SONG_FRAGMENT): índice del pulso
    // dentro del patrón/compás; -1 = sin reloj activo.
    val pulsoIdx: Int = -1,
    val timing: TimingVivo = TimingVivo.NINGUNO,
    val feedback: FeedbackVivo = FeedbackVivo.NEUTRO,
    val racha: Int = 0,
    val aciertosPaso: Int = 0,
    val esperadosPaso: Int = 0,
    val segundosRestantes: Int = 0,
    val segundosTotal: Int = 0,
    val progresoPaso: Float = 0f,
    val progresoGlobal: Float = 0f,
    val puntuacionViva: Int = 0,
    val error: String? = null,
    // Guía visual de acordes (solo pasos CHORD / CHORD_CHANGE con forma
    // conocida en el catálogo). guiaAcordes sigue disponible durante PASO
    // para mostrar los mini-diagramas mientras se detecta.
    val guiaAcordes: List<String> = emptyList(),
    val guiaPasoIdx: Int = 0,
    val guiaTexto: String = ""
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
    private var guiaJob: kotlinx.coroutines.Job? = null
    private var ritmoJob: kotlinx.coroutines.Job? = null

    // Estado interno del paso en curso.
    private var objetivoIdx = 0
    private var framesEnObjetivo = 0
    private var framesEnError = 0
    private var framesEnAcorde = 0
    private var framesAcordeError = 0
    private var acordeArmado = true       // exige un ataque nuevo entre rasgueos válidos
    private var ataquesPaso = 0
    private var inicioRitmoMs = 0L
    private var pasoTerminado = false
    private val resultados = mutableListOf<ResultadoPaso>()

    fun iniciar(ejercicio: EjercicioDto) {
        val f = _state.value.fase
        // Sesión activa: no reiniciar. Sesión terminada/limpia: reset completo.
        if (f == FaseVivo.CUENTA || f == FaseVivo.GUIA || f == FaseVivo.PASO || f == FaseVivo.TRANSICION) return
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
            // Durante GUIA el tiempo del paso NO corre (solo se descuenta en PASO).
            while (_state.value.fase == FaseVivo.PASO || _state.value.fase == FaseVivo.TRANSICION ||
                _state.value.fase == FaseVivo.GUIA
            ) {
                delay(1000)
                val s = _state.value
                if (s.fase != FaseVivo.PASO) continue
                val restantes = s.segundosRestantes - 1
                _state.value = s.copy(
                    segundosRestantes = restantes.coerceAtLeast(0),
                    segundosTotal = s.segundosTotal + 1
                )
                if (restantes <= 0) cerrarPaso()
            }
        }
    }

    private fun pasoActual(): PasoEjercicioDto = pasos[_state.value.pasoIdx]

    private fun esperadosDe(paso: PasoEjercicioDto): Int = when {
        paso.tipo in TIPOS_POR_PITCH -> paso.objetivos.size
        // CHORD: rasgueos con el acorde correcto sonando.
        paso.tipo == "CHORD" -> RASGUEOS_POR_ACORDE
        // CHORD_CHANGE: cada acorde de la secuencia, RONDAS_CAMBIO vueltas.
        paso.tipo == "CHORD_CHANGE" -> (paso.objetivos.size * RONDAS_CAMBIO).coerceAtLeast(4)
        else -> {
            // Actividad rítmica: pulsos esperados según bpm (o uno cada 2 s).
            val porBpm = paso.bpm?.let { paso.duracionSeg * it / 60 }
            (porBpm ?: (paso.duracionSeg / 2)).coerceAtLeast(4)
        }
    }

    private fun progresoDe(paso: PasoEjercicioDto, aciertos: Int): Float =
        if (paso.tipo in TIPOS_POR_PITCH) {
            if (paso.objetivos.isEmpty()) 0f else objetivoIdx.toFloat() / paso.objetivos.size
        } else {
            val esperados = esperadosDe(paso)
            if (esperados <= 0) 0f else (aciertos.toFloat() / esperados).coerceAtMost(1f)
        }

    private fun arrancarPaso(idx: Int) {
        val paso = pasos[idx]
        objetivoIdx = 0
        framesEnObjetivo = 0
        framesEnError = 0
        framesEnAcorde = 0
        framesAcordeError = 0
        acordeArmado = true
        ataquesPaso = 0
        pasoTerminado = false
        // Pasos de acorde con forma en el catálogo: primero la guía visual
        // (diagrama + colocación dedo a dedo) y recién después la detección.
        // El resto de tipos también abre con una guía previa (explicación +
        // representación gráfica del paso); un acorde sin forma conocida
        // sigue yendo directo a la detección, como siempre.
        val formasGuia = if (paso.tipo in TIPOS_ACORDE) {
            paso.objetivos.mapNotNull { CatalogoAcordes.buscar(it) }
        } else emptyList()
        val conGuia = formasGuia.isNotEmpty() || paso.tipo !in TIPOS_ACORDE
        _state.value = _state.value.copy(
            fase = if (conGuia) FaseVivo.GUIA else FaseVivo.PASO,
            guiaAcordes = formasGuia.map { it.nombre },
            guiaPasoIdx = 0,
            guiaTexto = "",
            pasoIdx = idx,
            titulo = paso.titulo,
            instruccion = paso.instruccion,
            tipo = paso.tipo,
            skill = paso.skill,
            porPitch = paso.tipo in TIPOS_POR_PITCH,
            objetivoActual = paso.objetivos.firstOrNull(),
            objetivos = paso.objetivos,
            objetivoIdx = 0,
            bpm = paso.bpm,
            notaDetectada = null,
            acordeDetectado = null,
            centsDetectados = null,
            pulsoIdx = -1,
            timing = TimingVivo.NINGUNO,
            feedback = FeedbackVivo.NEUTRO,
            aciertosPaso = 0,
            esperadosPaso = esperadosDe(paso),
            segundosRestantes = paso.duracionSeg,
            progresoPaso = 0f,
            progresoGlobal = idx.toFloat() / pasos.size
        )
        if (formasGuia.isNotEmpty()) {
            lanzarGuia(formasGuia, paso.tipo)
        } else if (conGuia) {
            lanzarGuiaPrevia()
        } else {
            alEntrarEnPaso()
        }
    }

    /**
     * Guía previa genérica (pasos que no son de acorde): la vista muestra la
     * explicación y el diagrama del tipo; pasados unos segundos la detección
     * arranca sola. "Empezar ya" (saltarGuia) la adelanta.
     */
    private fun lanzarGuiaPrevia() {
        guiaJob?.cancel()
        guiaJob = viewModelScope.launch {
            delay(MS_GUIA_PREVIA)
            empezarDeteccion()
        }
    }

    /**
     * Secuencia de la guía visual: revela la colocación paso a paso (cejilla
     * y dedos) con su instrucción hablada, deja unos segundos para acomodar
     * la mano y arranca la detección automáticamente. El micrófono ya está
     * abierto, pero los frames se ignoran hasta entrar en PASO.
     */
    private fun lanzarGuia(formas: List<AcordeForma>, tipo: String) {
        guiaJob?.cancel()
        guiaJob = viewModelScope.launch {
            if (tipo == "CHORD" && formas.size == 1) {
                val forma = formas.first()
                for (i in 0 until forma.totalPasosColocacion) {
                    _state.value = _state.value.copy(
                        guiaPasoIdx = i + 1,
                        guiaTexto = instruccionColocacion(forma, i) ?: ""
                    )
                    delay(MS_GUIA_POR_DEDO)
                }
                _state.value = _state.value.copy(
                    guiaTexto = "Ahora toca todas las cuerdas marcadas, que suene completo."
                )
                delay(MS_GUIA_ACOMODAR)
            } else {
                // Cambios de acorde: se muestran todas las formas completas.
                _state.value = _state.value.copy(
                    guiaPasoIdx = Int.MAX_VALUE,
                    guiaTexto = "Repasa las posiciones: " +
                        formas.joinToString(" → ") { it.nombre } +
                        ". Acomoda la mano en el primer acorde."
                )
                delay(MS_GUIA_CAMBIO)
            }
            empezarDeteccion()
        }
    }

    /** El usuario ya conoce el acorde: salta la guía y detecta ya. */
    fun saltarGuia() {
        if (_state.value.fase != FaseVivo.GUIA) return
        guiaJob?.cancel()
        guiaJob = null
        empezarDeteccion()
    }

    private fun empezarDeteccion() {
        if (_state.value.fase != FaseVivo.GUIA) return
        _state.value = _state.value.copy(
            fase = FaseVivo.PASO,
            guiaPasoIdx = Int.MAX_VALUE,
            guiaTexto = ""
        )
        alEntrarEnPaso()
    }

    /**
     * Arranca el reloj de ritmo del paso si el tipo lo necesita. RHYTHM y
     * SONG_FRAGMENT dejan de depender de animaciones sueltas de la UI: el
     * pulso vive aquí, la validación de golpes usa EL MISMO reloj y la vista
     * solo pinta [LiveState.pulsoIdx] / [LiveState.objetivoIdx].
     */
    private fun alEntrarEnPaso() {
        val paso = pasoActual()
        ritmoJob?.cancel()
        ritmoJob = null
        val bpm = paso.bpm ?: 60
        val msPorPulso = 60_000L / bpm.coerceAtLeast(20)
        when (paso.tipo) {
            "RHYTHM" -> {
                val patron = paso.objetivos.size.coerceAtLeast(1)
                inicioRitmoMs = System.currentTimeMillis()
                ritmoJob = viewModelScope.launch {
                    while (true) {
                        // El pulso se deriva SIEMPRE del reloj real (no de
                        // delays acumulados): cero deriva contra la validación.
                        val t = System.currentTimeMillis() - inicioRitmoMs
                        val pulso = (t / msPorPulso).toInt()
                        _state.value = _state.value.copy(pulsoIdx = pulso % patron)
                        delay(msPorPulso - (t % msPorPulso))
                    }
                }
            }
            "SONG_FRAGMENT" -> {
                val compases = paso.objetivos.size.coerceAtLeast(1)
                inicioRitmoMs = System.currentTimeMillis()
                ritmoJob = viewModelScope.launch {
                    while (true) {
                        val t = System.currentTimeMillis() - inicioRitmoMs
                        val pulso = (t / msPorPulso).toInt()
                        val compas = (pulso / 4) % compases
                        _state.value = _state.value.copy(
                            pulsoIdx = pulso % 4,
                            objetivoIdx = compas,
                            objetivoActual = paso.objetivos.getOrNull(compas)
                        )
                        delay(msPorPulso - (t % msPorPulso))
                    }
                }
            }
        }
    }

    private fun procesarFrame(frame: LivePracticeEngine.LiveFrame) {
        val s = _state.value
        if (s.fase != FaseVivo.PASO || pasoTerminado) return
        val paso = pasoActual()

        when {
            paso.tipo in TIPOS_POR_PITCH -> procesarPitch(frame, s, paso)
            paso.tipo in TIPOS_VALIDA_ACORDE -> procesarAcorde(frame, s, paso)
            paso.tipo == "RHYTHM" -> procesarRitmo(frame, s, paso)
            else -> procesarActividad(frame, s)
        }
    }

    /** NOTE/SEQUENCE/STRING/SCALE/ARPEGGIO/MELODY: nota a nota con MPM. */
    private fun procesarPitch(
        frame: LivePracticeEngine.LiveFrame,
        s: LiveState,
        paso: PasoEjercicioDto
    ) {
        val objetivo = paso.objetivos.getOrNull(objetivoIdx) ?: return
        // STRING es el paso de afinación: publica los cents para que la
        // vista muestre el mini-afinador aunque aún no haya acierto.
        val cents = if (paso.tipo == "STRING") centsVsObjetivo(frame.freqHz, objetivo) else null

        val nota = frame.nota
        if (nota == null) {
            framesEnObjetivo = 0
            if (cents != null) _state.value = s.copy(centsDetectados = null)
            return
        }
        val framesNecesarios = framesSostenidosDe(paso.tipo)
        if (cumpleObjetivo(frame, objetivo, paso.tipo)) {
            framesEnObjetivo++
            framesEnError = 0
            if (framesEnObjetivo >= framesNecesarios) {
                // Acierto: avanza al siguiente objetivo de la secuencia.
                framesEnObjetivo = 0
                objetivoIdx++
                val aciertos = s.aciertosPaso + 1
                _state.value = s.copy(
                    aciertosPaso = aciertos,
                    racha = s.racha + 1,
                    feedback = FeedbackVivo.ACIERTO,
                    notaDetectada = nota,
                    centsDetectados = cents?.toFloat(),
                    objetivoActual = paso.objetivos.getOrNull(objetivoIdx),
                    objetivoIdx = objetivoIdx,
                    progresoPaso = objetivoIdx.toFloat() / paso.objetivos.size,
                    puntuacionViva = puntuacionViva(aciertos)
                )
                if (objetivoIdx >= paso.objetivos.size) cerrarPaso()
            } else if (cents != null) {
                _state.value = s.copy(centsDetectados = cents.toFloat(), notaDetectada = nota)
            }
        } else {
            framesEnObjetivo = 0
            framesEnError++
            // Nota equivocada sostenida: feedback de fallo (sin castigar doble).
            if (framesEnError == framesNecesarios) {
                _state.value = s.copy(
                    feedback = FeedbackVivo.FALLO,
                    notaDetectada = nota,
                    centsDetectados = cents?.toFloat(),
                    racha = 0
                )
            } else if (cents != null) {
                _state.value = s.copy(centsDetectados = cents.toFloat(), notaDetectada = nota)
            }
        }
    }

    /**
     * CHORD/CHORD_CHANGE: reconocimiento REAL del acorde por croma.
     * Un acierto = el acorde objetivo sonando claro tras un rasgueo; entre
     * aciertos se exige un ataque nuevo (sostener el acorde no suma doble).
     * En CHORD_CHANGE el objetivo va rotando por la secuencia.
     */
    private fun procesarAcorde(
        frame: LivePracticeEngine.LiveFrame,
        s: LiveState,
        paso: PasoEjercicioDto
    ) {
        val objetivos = paso.objetivos.ifEmpty { return }
        val objetivoCrudo = objetivos[objetivoIdx % objetivos.size]
        val objetivo = ChordDetector.normalizarObjetivo(objetivoCrudo) ?: objetivoCrudo

        if (frame.ataque) acordeArmado = true

        val acorde = frame.acorde
        if (acorde == null) {
            framesEnAcorde = 0
            framesAcordeError = 0
            return
        }

        if (acorde == objetivo) {
            framesEnAcorde++
            framesAcordeError = 0
            if (framesEnAcorde >= FRAMES_ACORDE && acordeArmado) {
                framesEnAcorde = 0
                acordeArmado = false
                objetivoIdx++
                val aciertos = s.aciertosPaso + 1
                val esperados = s.esperadosPaso
                _state.value = s.copy(
                    aciertosPaso = aciertos,
                    racha = s.racha + 1,
                    feedback = FeedbackVivo.ACIERTO,
                    acordeDetectado = acorde,
                    notaDetectada = null,
                    objetivoActual = objetivos[objetivoIdx % objetivos.size],
                    objetivoIdx = objetivoIdx,
                    progresoPaso = progresoDe(paso, aciertos),
                    puntuacionViva = puntuacionViva(aciertos)
                )
                if (aciertos >= esperados) cerrarPaso()
            } else {
                _state.value = s.copy(acordeDetectado = acorde)
            }
        } else {
            framesEnAcorde = 0
            framesAcordeError++
            if (framesAcordeError == FRAMES_ACORDE + 1) {
                _state.value = s.copy(
                    feedback = FeedbackVivo.FALLO,
                    acordeDetectado = acorde,
                    racha = 0
                )
            } else {
                _state.value = s.copy(acordeDetectado = acorde)
            }
        }
    }

    /**
     * RHYTHM: cada ataque se compara contra el reloj de pulsos del paso.
     * A tiempo suma; adelantado/atrasado se marca como tal (el usuario ve
     * hacia dónde corregir) sin sumar el golpe.
     */
    private fun procesarRitmo(
        frame: LivePracticeEngine.LiveFrame,
        s: LiveState,
        paso: PasoEjercicioDto
    ) {
        if (!frame.ataque) return
        val bpm = (paso.bpm ?: 60).coerceAtLeast(20)
        val msPorPulso = 60_000.0 / bpm
        val t = (System.currentTimeMillis() - inicioRitmoMs).toDouble()
        val fase = t % msPorPulso
        // Distancia con signo al pulso más cercano: + atrasado, - adelantado.
        val dt = if (fase <= msPorPulso / 2) fase else fase - msPorPulso
        // La ventana de análisis (~93 ms) impone un piso a la tolerancia.
        val tolerancia = (msPorPulso * 0.25).coerceIn(140.0, 260.0)

        if (abs(dt) <= tolerancia) {
            val aciertos = (s.aciertosPaso + 1).coerceAtMost(s.esperadosPaso)
            _state.value = s.copy(
                aciertosPaso = aciertos,
                racha = s.racha + 1,
                feedback = FeedbackVivo.ACIERTO,
                timing = TimingVivo.A_TIEMPO,
                notaDetectada = frame.nota,
                progresoPaso = progresoDe(paso, aciertos),
                puntuacionViva = puntuacionViva(aciertos)
            )
            if (aciertos >= s.esperadosPaso) cerrarPaso()
        } else {
            _state.value = s.copy(
                feedback = FeedbackVivo.FALLO,
                timing = if (dt < 0) TimingVivo.ADELANTADO else TimingVivo.ATRASADO,
                racha = 0
            )
        }
    }

    /** SONG_FRAGMENT/CUSTOM: cuenta ataques y muestra el acorde reconocido. */
    private fun procesarActividad(frame: LivePracticeEngine.LiveFrame, s: LiveState) {
        val acorde = frame.acorde
        if (!frame.ataque) {
            if (acorde != null && acorde != s.acordeDetectado) {
                _state.value = s.copy(acordeDetectado = acorde)
            }
            return
        }
        ataquesPaso++
        val esperados = s.esperadosPaso
        val aciertos = ataquesPaso.coerceAtMost(esperados)
        _state.value = s.copy(
            aciertosPaso = aciertos,
            racha = s.racha + 1,
            feedback = FeedbackVivo.ACIERTO,
            notaDetectada = frame.nota,
            acordeDetectado = acorde ?: s.acordeDetectado,
            progresoPaso = progresoDe(pasoActual(), aciertos),
            puntuacionViva = puntuacionViva(aciertos)
        )
        if (aciertos >= esperados) cerrarPaso()
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
        ritmoJob?.cancel()
        ritmoJob = null
        val s = _state.value
        val paso = pasoActual()
        val esperados = s.esperadosPaso
        val completado = when {
            paso.tipo in TIPOS_POR_PITCH -> s.aciertosPaso >= paso.objetivos.size
            // Acordes: reconocimiento real -> exige al menos 3/4 del objetivo.
            paso.tipo in TIPOS_VALIDA_ACORDE ->
                esperados > 0 && s.aciertosPaso.toDouble() / esperados >= 0.75
            else -> esperados > 0 && s.aciertosPaso.toDouble() / esperados >= 0.5
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
        guiaJob?.cancel()
        ritmoJob?.cancel()
        framesJob = null
        sesionJob = null
        guiaJob = null
        ritmoJob = null
        engine?.discard()
        engine = null
        resultados.clear()
        objetivoIdx = 0
        framesEnObjetivo = 0
        framesEnError = 0
        framesEnAcorde = 0
        framesAcordeError = 0
        acordeArmado = true
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
