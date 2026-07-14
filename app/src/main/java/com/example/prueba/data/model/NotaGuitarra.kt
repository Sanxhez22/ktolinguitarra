package com.example.prueba.data.model

/**
 * Posición física en el diapasón de la guitarra. Modelo puro (sin UI) para
 * reutilizarlo en práctica guiada, canciones, Play Along y lecciones.
 *
 * Convención de cuerdas (igual que [AcordeForma]): 6 = Mi grave ... 1 = Mi aguda.
 * Traste 0 = cuerda al aire.
 */
data class PosicionDiapason(
    val cuerda: Int,        // 6..1
    val traste: Int,        // 0..n (0 = al aire)
    val dedo: Int? = null   // 1..4 sugerido; null = al aire o sin sugerencia
)

/**
 * Mapea nombres de nota con octava ("E2", "F#3", "Bb2") a posiciones en el
 * diapasón en afinación estándar, prefiriendo la primera posición (cuerdas
 * al aire y trastes bajos), que es donde viven todos los ejercicios del
 * catálogo para principiantes.
 */
object NotasGuitarra {

    private val INDICE_NOTA = mapOf(
        "C" to 0, "C#" to 1, "DB" to 1, "D" to 2, "D#" to 3, "EB" to 3,
        "E" to 4, "F" to 5, "F#" to 6, "GB" to 6, "G" to 7, "G#" to 8,
        "AB" to 8, "A" to 9, "A#" to 10, "BB" to 10, "B" to 11
    )

    /** MIDI de cada cuerda al aire, de la 6ª a la 1ª (afinación estándar). */
    private val AFINACION = mapOf(6 to 40, 5 to 45, 4 to 50, 3 to 55, 2 to 59, 1 to 64)

    /** Número MIDI de una nota escrita ("A2" → 45); null si no se puede parsear. */
    fun midiDe(nota: String): Int? {
        val limpia = nota.trim()
        val m = Regex("^([A-Ga-g][#b]?)(-?\\d)$").find(limpia) ?: return null
        val idx = INDICE_NOTA[m.groupValues[1].uppercase()] ?: return null
        val octava = m.groupValues[2].toIntOrNull() ?: return null
        return (octava + 1) * 12 + idx
    }

    /** La cuerda cuya nota al aire es exactamente [nota], o null. */
    fun cuerdaAlAire(nota: String): Int? {
        val midi = midiDe(nota) ?: return null
        return AFINACION.entries.firstOrNull { it.value == midi }?.key
    }

    /**
     * Mejor posición para tocar [nota]: cuerda al aire si existe; si no, el
     * traste más bajo disponible (preferencia por primera posición). El dedo
     * sugerido sigue la regla clásica "un dedo por traste" (1º-4º traste →
     * dedos 1-4).
     */
    fun posicionDe(nota: String, trasteMax: Int = 12): PosicionDiapason? {
        val midi = midiDe(nota) ?: return null
        var mejor: PosicionDiapason? = null
        for ((cuerda, abierta) in AFINACION) {
            val traste = midi - abierta
            if (traste < 0 || traste > trasteMax) continue
            if (traste == 0) return PosicionDiapason(cuerda, 0, dedo = null)
            if (mejor == null || traste < mejor.traste) {
                mejor = PosicionDiapason(cuerda, traste, dedo = traste.coerceAtMost(4))
            }
        }
        return mejor
    }

    /** Posiciones de una secuencia de notas (se omiten las no parseables). */
    fun posicionesDe(notas: List<String>): List<PosicionDiapason> =
        notas.mapNotNull { posicionDe(it) }
}
