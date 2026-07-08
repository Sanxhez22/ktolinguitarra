package com.example.prueba.data.model

/**
 * Forma de un acorde en el diapasón. Modelo puro (sin dependencias de UI)
 * para poder reutilizarlo en práctica guiada, canciones, tutoriales,
 * onboarding y una futura biblioteca de acordes.
 *
 * Convención de cuerdas: 6 = Mi grave (la más gruesa) ... 1 = Mi aguda.
 * Los trastes son absolutos; [trasteBase] permite dibujar acordes lejos
 * de la cejuela (p. ej. una cejilla en el traste 5) sin trastes vacíos.
 */
data class DedoAcorde(
    val cuerda: Int,   // 6..1
    val traste: Int,   // 1..n (absoluto)
    val dedo: Int      // 1=índice, 2=medio, 3=anular, 4=meñique
)

/** Cejilla: un dedo que pisa varias cuerdas en el mismo traste. */
data class CejillaAcorde(
    val traste: Int,
    val cuerdaDesde: Int,  // cuerda más grave que pisa (6..1)
    val cuerdaHasta: Int,  // cuerda más aguda que pisa (6..1)
    val dedo: Int = 1
)

data class AcordeForma(
    val nombre: String,               // "Am"
    val nombreLargo: String,          // "La menor"
    val dedos: List<DedoAcorde>,      // ordenados como se enseñan (1º, 2º...)
    val cejilla: CejillaAcorde? = null,
    val cuerdasAlAire: List<Int> = emptyList(),  // suenan sin pisar (○)
    val cuerdasMudas: List<Int> = emptyList(),   // no se tocan (X)
    val trasteBase: Int = 1
) {
    /** Pasos de colocación en orden didáctico: cejilla primero, luego dedos. */
    val totalPasosColocacion: Int
        get() = dedos.size + (if (cejilla != null) 1 else 0)
}

/** Nombre hablado de cada cuerda para las instrucciones. */
fun nombreCuerda(cuerda: Int): String = when (cuerda) {
    6 -> "Mi grave (6ª)"
    5 -> "La (5ª)"
    4 -> "Re (4ª)"
    3 -> "Sol (3ª)"
    2 -> "Si (2ª)"
    1 -> "Mi aguda (1ª)"
    else -> "${cuerda}ª"
}

/**
 * Instrucción hablada del paso de colocación [idx] (0-based, cejilla incluida).
 * Devuelve null si el índice queda fuera de la forma.
 */
fun instruccionColocacion(forma: AcordeForma, idx: Int): String? {
    val cejilla = forma.cejilla
    if (cejilla != null) {
        if (idx == 0) {
            return "Acuesta el dedo ${cejilla.dedo} sobre el traste ${cejilla.traste}, " +
                "cubriendo de la ${cejilla.cuerdaDesde}ª a la ${cejilla.cuerdaHasta}ª cuerda (cejilla)."
        }
        val dedo = forma.dedos.getOrNull(idx - 1) ?: return null
        return "Coloca el dedo ${dedo.dedo} en el traste ${dedo.traste} de la cuerda ${nombreCuerda(dedo.cuerda)}."
    }
    val dedo = forma.dedos.getOrNull(idx) ?: return null
    return "Coloca el dedo ${dedo.dedo} en el traste ${dedo.traste} de la cuerda ${nombreCuerda(dedo.cuerda)}."
}

/**
 * Catálogo de formas de acordes abiertos (y cejillas básicas). Cubre todos
 * los acordes que hoy usa el motor de ejercicios (Am, C, G, F, cambios) y
 * los demás abiertos comunes para canciones/tutoriales futuros.
 */
object CatalogoAcordes {

    private val formas: Map<String, AcordeForma> = listOf(
        AcordeForma(
            nombre = "C", nombreLargo = "Do Mayor",
            dedos = listOf(
                DedoAcorde(cuerda = 2, traste = 1, dedo = 1),
                DedoAcorde(cuerda = 4, traste = 2, dedo = 2),
                DedoAcorde(cuerda = 5, traste = 3, dedo = 3)
            ),
            cuerdasAlAire = listOf(3, 1), cuerdasMudas = listOf(6)
        ),
        AcordeForma(
            nombre = "A", nombreLargo = "La Mayor",
            dedos = listOf(
                DedoAcorde(cuerda = 4, traste = 2, dedo = 1),
                DedoAcorde(cuerda = 3, traste = 2, dedo = 2),
                DedoAcorde(cuerda = 2, traste = 2, dedo = 3)
            ),
            cuerdasAlAire = listOf(5, 1), cuerdasMudas = listOf(6)
        ),
        AcordeForma(
            nombre = "G", nombreLargo = "Sol Mayor",
            dedos = listOf(
                DedoAcorde(cuerda = 5, traste = 2, dedo = 1),
                DedoAcorde(cuerda = 6, traste = 3, dedo = 2),
                DedoAcorde(cuerda = 1, traste = 3, dedo = 3)
            ),
            cuerdasAlAire = listOf(4, 3, 2)
        ),
        AcordeForma(
            nombre = "E", nombreLargo = "Mi Mayor",
            dedos = listOf(
                DedoAcorde(cuerda = 3, traste = 1, dedo = 1),
                DedoAcorde(cuerda = 5, traste = 2, dedo = 2),
                DedoAcorde(cuerda = 4, traste = 2, dedo = 3)
            ),
            cuerdasAlAire = listOf(6, 2, 1)
        ),
        AcordeForma(
            nombre = "D", nombreLargo = "Re Mayor",
            dedos = listOf(
                DedoAcorde(cuerda = 3, traste = 2, dedo = 1),
                DedoAcorde(cuerda = 1, traste = 2, dedo = 2),
                DedoAcorde(cuerda = 2, traste = 3, dedo = 3)
            ),
            cuerdasAlAire = listOf(4), cuerdasMudas = listOf(6, 5)
        ),
        AcordeForma(
            nombre = "F", nombreLargo = "Fa Mayor",
            dedos = listOf(
                DedoAcorde(cuerda = 3, traste = 2, dedo = 2),
                DedoAcorde(cuerda = 5, traste = 3, dedo = 3),
                DedoAcorde(cuerda = 4, traste = 3, dedo = 4)
            ),
            cejilla = CejillaAcorde(traste = 1, cuerdaDesde = 6, cuerdaHasta = 1)
        ),
        AcordeForma(
            nombre = "Am", nombreLargo = "La menor",
            dedos = listOf(
                DedoAcorde(cuerda = 2, traste = 1, dedo = 1),
                DedoAcorde(cuerda = 4, traste = 2, dedo = 2),
                DedoAcorde(cuerda = 3, traste = 2, dedo = 3)
            ),
            cuerdasAlAire = listOf(5, 1), cuerdasMudas = listOf(6)
        ),
        AcordeForma(
            nombre = "Em", nombreLargo = "Mi menor",
            dedos = listOf(
                DedoAcorde(cuerda = 5, traste = 2, dedo = 2),
                DedoAcorde(cuerda = 4, traste = 2, dedo = 3)
            ),
            cuerdasAlAire = listOf(6, 3, 2, 1)
        ),
        AcordeForma(
            nombre = "Dm", nombreLargo = "Re menor",
            dedos = listOf(
                DedoAcorde(cuerda = 1, traste = 1, dedo = 1),
                DedoAcorde(cuerda = 3, traste = 2, dedo = 2),
                DedoAcorde(cuerda = 2, traste = 3, dedo = 3)
            ),
            cuerdasAlAire = listOf(4), cuerdasMudas = listOf(6, 5)
        ),
        AcordeForma(
            nombre = "Bm", nombreLargo = "Si menor",
            dedos = listOf(
                DedoAcorde(cuerda = 2, traste = 3, dedo = 2),
                DedoAcorde(cuerda = 4, traste = 4, dedo = 3),
                DedoAcorde(cuerda = 3, traste = 4, dedo = 4)
            ),
            cejilla = CejillaAcorde(traste = 2, cuerdaDesde = 5, cuerdaHasta = 1),
            cuerdasMudas = listOf(6)
        ),
        AcordeForma(
            nombre = "A7", nombreLargo = "La séptima",
            dedos = listOf(
                DedoAcorde(cuerda = 4, traste = 2, dedo = 1),
                DedoAcorde(cuerda = 2, traste = 2, dedo = 2)
            ),
            cuerdasAlAire = listOf(5, 3, 1), cuerdasMudas = listOf(6)
        ),
        AcordeForma(
            nombre = "B7", nombreLargo = "Si séptima",
            dedos = listOf(
                DedoAcorde(cuerda = 4, traste = 1, dedo = 1),
                DedoAcorde(cuerda = 5, traste = 2, dedo = 2),
                DedoAcorde(cuerda = 3, traste = 2, dedo = 3),
                DedoAcorde(cuerda = 1, traste = 2, dedo = 4)
            ),
            cuerdasAlAire = listOf(2), cuerdasMudas = listOf(6)
        ),
        AcordeForma(
            nombre = "C7", nombreLargo = "Do séptima",
            dedos = listOf(
                DedoAcorde(cuerda = 2, traste = 1, dedo = 1),
                DedoAcorde(cuerda = 4, traste = 2, dedo = 2),
                DedoAcorde(cuerda = 5, traste = 3, dedo = 3),
                DedoAcorde(cuerda = 3, traste = 3, dedo = 4)
            ),
            cuerdasAlAire = listOf(1), cuerdasMudas = listOf(6)
        ),
        AcordeForma(
            nombre = "D7", nombreLargo = "Re séptima",
            dedos = listOf(
                DedoAcorde(cuerda = 2, traste = 1, dedo = 1),
                DedoAcorde(cuerda = 3, traste = 2, dedo = 2),
                DedoAcorde(cuerda = 1, traste = 2, dedo = 3)
            ),
            cuerdasAlAire = listOf(4), cuerdasMudas = listOf(6, 5)
        ),
        AcordeForma(
            nombre = "E7", nombreLargo = "Mi séptima",
            dedos = listOf(
                DedoAcorde(cuerda = 3, traste = 1, dedo = 1),
                DedoAcorde(cuerda = 5, traste = 2, dedo = 2)
            ),
            cuerdasAlAire = listOf(6, 4, 2, 1)
        ),
        AcordeForma(
            nombre = "G7", nombreLargo = "Sol séptima",
            dedos = listOf(
                DedoAcorde(cuerda = 1, traste = 1, dedo = 1),
                DedoAcorde(cuerda = 5, traste = 2, dedo = 2),
                DedoAcorde(cuerda = 6, traste = 3, dedo = 3)
            ),
            cuerdasAlAire = listOf(4, 3, 2)
        )
    ).associateBy { it.nombre }

    /** Busca la forma por nombre exacto ("Am", "G7"...); null si no está. */
    fun buscar(nombre: String): AcordeForma? = formas[nombre.trim()]

    /** Nombres disponibles (para una futura biblioteca de acordes). */
    fun nombres(): List<String> = formas.keys.toList()
}
