package com.example.prueba.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.prueba.FretMindApp
import com.example.prueba.api.PracticaGuiadaCancionDto
import com.example.prueba.audio.ChordDetector
import com.example.prueba.audio.LivePracticeEngine
import com.example.prueba.data.repository.AuthRepository
import com.example.prueba.data.repository.PracticeRepository
import com.example.prueba.data.repository.SongRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/** Segundos extra que suena la última línea antes de cerrar la práctica. */
private const val SEG_CIERRE = 8.0

/** Frames consecutivos con el acorde correcto para marcar la línea. */
private const val FRAMES_ACORDE_CANCION = 2

enum class FaseCancion { CARGANDO, LISTA, CUENTA, TOCANDO, PAUSA, FINALIZADA, ERROR }

/** Una línea de la canción aplanada y con tiempos garantizados. */
data class LineaCancionVivo(
    val seccion: String,
    val esInicioSeccion: Boolean,
    val texto: String,
    val acorde: String?,
    val tSeg: Double
)

data class SongPracticeState(
    val fase: FaseCancion = FaseCancion.CARGANDO,
    val cuenta: Int = 3,
    val titulo: String = "",
    val artista: String = "",
    val portada: String? = null,
    val tieneLetra: Boolean = false,
    val consejo: String = "",
    val notaTransparencia: String = "",
    val progresion: List<String> = emptyList(),
    val lineas: List<LineaCancionVivo> = emptyList(),
    val lineaActual: Int = -1,
    val acordeDetectado: String? = null,
    val lineasAcertadas: Set<Int> = emptySet(),
    val lineasPerdidas: Set<Int> = emptySet(),
    val aciertos: Int = 0,
    val totalAcordes: Int = 0,
    val segundos: Int = 0,
    val duracionTotalSeg: Int = 0,
    val error: String? = null,
    // Resultado final
    val puntuacion: Double = 0.0,
    val estrellas: Int = 0,
    val envioEstado: String = ""   // "", "enviando", "ok", "error"
)

/**
 * Práctica guiada de canción (estilo Ultimate Guitar + Yousician):
 * la letra avanza sola con el reloj de la canción, el micrófono reconoce
 * el acorde que suena (croma) y cada línea se marca como lograda cuando
 * su acorde se tocó dentro de su ventana de tiempo.
 */
class SongPracticeViewModel : ViewModel() {

    private val songRepository = SongRepository()
    private val practiceRepository = PracticeRepository()

    private val _state = MutableStateFlow(SongPracticeState())
    val state: StateFlow<SongPracticeState> = _state.asStateFlow()

    private val frames = MutableSharedFlow<LivePracticeEngine.LiveFrame>(
        extraBufferCapacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private var engine: LivePracticeEngine? = null
    private var relojJob: kotlinx.coroutines.Job? = null
    private var framesJob: kotlinx.coroutines.Job? = null
    private var contenido: PracticaGuiadaCancionDto? = null
    private var songId: Long = 0L
    private var inicioMs = 0L
    private var pausaDesdeMs = 0L
    private var pausadoTotalMs = 0L
    private var inicioIso = ""
    private var framesEnAcorde = 0

    private var cargado = false

    fun cargar(songId: Long) {
        // Ya cargada esta canción (recomposición/rotación): no recargar.
        if (this.songId == songId && cargado) return
        this.songId = songId
        cargado = false
        _state.value = SongPracticeState(fase = FaseCancion.CARGANDO)
        viewModelScope.launch {
            val session = AuthRepository.restoreSession().getOrNull()
            if (session == null) {
                _state.value = SongPracticeState(
                    fase = FaseCancion.ERROR, error = "Inicia sesión para practicar canciones."
                )
                return@launch
            }
            songRepository.practicaGuiada(songId, session.id)
                .onSuccess { dto ->
                    // Sin hoja de acordes real no hay práctica: mensaje
                    // honesto en vez de una progresión inventada.
                    if (!dto.disponible) {
                        _state.value = SongPracticeState(
                            fase = FaseCancion.ERROR,
                            error = dto.nota.ifBlank {
                                "Esta canción aún no tiene sus acordes disponibles."
                            }
                        )
                        return@onSuccess
                    }
                    contenido = dto
                    cargado = true
                    val lineas = aplanar(dto)
                    _state.value = SongPracticeState(
                        fase = FaseCancion.LISTA,
                        titulo = dto.cancion.titulo,
                        artista = dto.cancion.artista,
                        portada = dto.cancion.portada,
                        tieneLetra = dto.tieneLetra,
                        consejo = dto.consejo,
                        notaTransparencia = dto.nota,
                        progresion = dto.progresion,
                        lineas = lineas,
                        totalAcordes = lineas.count { it.acorde != null }
                    )
                }
                .onFailure { e ->
                    _state.value = SongPracticeState(
                        fase = FaseCancion.ERROR,
                        error = e.message ?: "No se pudo cargar la práctica de la canción."
                    )
                }
        }
    }

    /** Aplana secciones->líneas garantizando tiempos crecientes. */
    private fun aplanar(dto: PracticaGuiadaCancionDto): List<LineaCancionVivo> {
        val lineas = mutableListOf<LineaCancionVivo>()
        var tAnterior = 0.0
        dto.secciones.forEach { seccion ->
            seccion.lineas.forEachIndexed { i, l ->
                val t = l.tSeg ?: (tAnterior + 8.0)
                tAnterior = maxOf(t, tAnterior)
                lineas.add(
                    LineaCancionVivo(
                        seccion = seccion.nombre,
                        esInicioSeccion = i == 0,
                        texto = l.texto,
                        acorde = l.acorde,
                        tSeg = tAnterior
                    )
                )
            }
        }
        return lineas
    }

    /** El usuario pulsa Comenzar (el permiso de micrófono ya está concedido). */
    fun comenzar() {
        val s = _state.value
        if (s.fase != FaseCancion.LISTA && s.fase != FaseCancion.FINALIZADA) return
        if (s.lineas.isEmpty()) return

        framesEnAcorde = 0
        _state.value = s.copy(
            fase = FaseCancion.CUENTA,
            cuenta = 3,
            lineaActual = -1,
            lineasAcertadas = emptySet(),
            lineasPerdidas = emptySet(),
            aciertos = 0,
            segundos = 0,
            puntuacion = 0.0,
            estrellas = 0,
            envioEstado = ""
        )
        engine = LivePracticeEngine(FretMindApp.instance.cacheDir, maxSeconds = 90) { frame ->
            frames.tryEmit(frame)
        }
        framesJob?.cancel()
        framesJob = viewModelScope.launch { frames.collect { procesarFrame(it) } }

        relojJob?.cancel()
        relojJob = viewModelScope.launch {
            for (n in 3 downTo 1) {
                _state.value = _state.value.copy(cuenta = n)
                delay(1000)
            }
            if (engine?.start() != true) {
                _state.value = _state.value.copy(
                    fase = FaseCancion.ERROR, error = "No se pudo iniciar el micrófono."
                )
                return@launch
            }
            inicioIso = ahoraIso()
            inicioMs = System.currentTimeMillis()
            pausadoTotalMs = 0L
            _state.value = _state.value.copy(fase = FaseCancion.TOCANDO)
            correrReloj()
        }
    }

    /** Reloj de la canción: avanza la línea activa y cierra al final. */
    private suspend fun correrReloj() {
        while (true) {
            delay(200)
            val s = _state.value
            if (s.fase != FaseCancion.TOCANDO) {
                if (s.fase == FaseCancion.PAUSA) continue else return
            }
            val transcurrido = (System.currentTimeMillis() - inicioMs - pausadoTotalMs) / 1000.0
            var idx = -1
            for (i in s.lineas.indices) {
                if (s.lineas[i].tSeg <= transcurrido) idx = i else break
            }
            // Líneas que quedaron atrás sin acierto -> perdidas (feedback honesto).
            val perdidas = s.lineasPerdidas.toMutableSet()
            for (i in 0 until idx) {
                if (s.lineas[i].acorde != null && i !in s.lineasAcertadas) perdidas.add(i)
            }
            _state.value = s.copy(
                lineaActual = idx,
                lineasPerdidas = perdidas,
                segundos = transcurrido.toInt()
            )
            val fin = s.lineas.last().tSeg + SEG_CIERRE
            if (transcurrido >= fin) {
                finalizar()
                return
            }
        }
    }

    private fun procesarFrame(frame: LivePracticeEngine.LiveFrame) {
        val s = _state.value
        if (s.fase != FaseCancion.TOCANDO) return
        val acorde = frame.acorde ?: run {
            framesEnAcorde = 0
            return
        }
        if (acorde != s.acordeDetectado) {
            _state.value = s.copy(acordeDetectado = acorde)
        }

        val idx = s.lineaActual
        val linea = s.lineas.getOrNull(idx) ?: return
        val objetivo = linea.acorde?.let { ChordDetector.normalizarObjetivo(it) } ?: return
        if (idx in s.lineasAcertadas) return

        if (acorde == objetivo) {
            framesEnAcorde++
            if (framesEnAcorde >= FRAMES_ACORDE_CANCION) {
                framesEnAcorde = 0
                _state.value = _state.value.copy(
                    lineasAcertadas = s.lineasAcertadas + idx,
                    aciertos = s.aciertos + 1,
                    acordeDetectado = acorde
                )
            }
        } else {
            framesEnAcorde = 0
        }
    }

    fun pausar() {
        if (_state.value.fase != FaseCancion.TOCANDO) return
        pausaDesdeMs = System.currentTimeMillis()
        _state.value = _state.value.copy(fase = FaseCancion.PAUSA)
    }

    fun reanudar() {
        if (_state.value.fase != FaseCancion.PAUSA) return
        pausadoTotalMs += System.currentTimeMillis() - pausaDesdeMs
        _state.value = _state.value.copy(fase = FaseCancion.TOCANDO)
    }

    /** Termina la práctica ya (botón), o al agotar la canción. */
    fun terminar() {
        val f = _state.value.fase
        if (f == FaseCancion.TOCANDO || f == FaseCancion.PAUSA) finalizar()
    }

    private fun finalizar() {
        relojJob?.cancel()
        relojJob = null
        val wav = engine?.stop()
        engine = null

        val s = _state.value
        val total = s.totalAcordes.coerceAtLeast(1)
        val puntuacion = s.aciertos * 100.0 / total
        val estrellas = when {
            puntuacion >= 85 -> 3
            puntuacion >= 65 -> 2
            puntuacion >= 40 -> 1
            else -> 0
        }
        _state.value = s.copy(
            fase = FaseCancion.FINALIZADA,
            puntuacion = (puntuacion * 10).toInt() / 10.0,
            estrellas = estrellas,
            envioEstado = if (wav != null) "enviando" else ""
        )

        // Registra la sesión como práctica real asociada a la canción.
        if (wav == null) return
        val ejercicioId = contenido?.ejercicioId ?: "primera_cancion"
        val detalle = JSONArray().put(
            JSONObject()
                .put("id", "cancion_$songId")
                .put("tipo", "SONG_GUIDED")
                .put("skill", "cambios_acordes")
                .put("aciertos", s.aciertos)
                .put("total", s.totalAcordes)
                .put("completado", puntuacion >= 50)
                .put("puntuacion", (puntuacion * 10).toInt() / 10.0)
        ).toString()
        viewModelScope.launch {
            val session = AuthRepository.restoreSession().getOrNull()
            if (session == null) {
                _state.value = _state.value.copy(envioEstado = "error")
                return@launch
            }
            practiceRepository.submitPractice(
                userId = session.id,
                file = wav,
                duracionSeg = _state.value.segundos,
                ejercicio = ejercicioId,
                cancionId = songId,
                puntuacion = _state.value.puntuacion,
                estrellas = _state.value.estrellas,
                notasAcertadas = _state.value.aciertos,
                notasTotales = _state.value.totalAcordes,
                inicioIso = inicioIso,
                detallePasosJson = detalle
            )
                .onSuccess { _state.value = _state.value.copy(envioEstado = "ok") }
                .onFailure { _state.value = _state.value.copy(envioEstado = "error") }
        }
    }

    /** Cancela y descarta (salir de la pantalla en medio de la práctica). */
    fun cancelar() {
        relojJob?.cancel()
        relojJob = null
        framesJob?.cancel()
        framesJob = null
        engine?.discard()
        engine = null
        val s = _state.value
        if (s.fase == FaseCancion.TOCANDO || s.fase == FaseCancion.PAUSA || s.fase == FaseCancion.CUENTA) {
            _state.value = s.copy(fase = FaseCancion.LISTA)
        }
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
