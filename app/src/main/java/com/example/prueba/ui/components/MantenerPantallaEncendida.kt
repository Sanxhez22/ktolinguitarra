package com.example.prueba.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView

/**
 * Mantiene la pantalla encendida mientras el composable que lo invoca esté
 * en composición: afinador y sesiones de práctica, donde el usuario toca la
 * guitarra durante minutos sin tocar el teléfono y el bloqueo por
 * inactividad cortaría la sesión.
 *
 * Usa View.keepScreenOn (equivale a FLAG_KEEP_SCREEN_ON pero acotado a la
 * vista): al salir de la composición se restaura el valor previo, así el
 * resto de la app conserva el comportamiento normal del sistema.
 */
@Composable
fun MantenerPantallaEncendida() {
    val view = LocalView.current
    DisposableEffect(view) {
        val previo = view.keepScreenOn
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = previo }
    }
}
