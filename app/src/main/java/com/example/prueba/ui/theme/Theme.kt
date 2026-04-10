package com.example.prueba.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = FretGold,
    secondary = FretGold2,
    tertiary = FretGold,

    background = FretBlack,
    surface = FretSurface,

    onPrimary = FretBlack,
    onSecondary = FretBlack,
    onTertiary = FretBlack,

    onBackground = FretText,
    onSurface = FretText
)

private val LightColorScheme = lightColorScheme(
    primary = FretGold,
    secondary = FretGold2,
    tertiary = FretGold,

    background = Color(0xFFF6F7FB),
    surface = Color.White,

    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.Black,

    onBackground = Color(0xFF14161A),
    onSurface = Color(0xFF14161A)
)

@Composable
fun PruebaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // <- lo dejamos off para que respete FretMind
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // si algún día quieres dynamicColor, lo activamos después
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}