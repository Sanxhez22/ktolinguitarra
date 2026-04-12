package com.example.prueba.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.prueba.ui.theme.*
import kotlin.math.abs

data class Cuerda(val nombre: String, val freq: Float, val numero: String)

private val CUERDAS = listOf(
    Cuerda("E2", 82.41f, "6ª"),
    Cuerda("A2", 110.0f, "5ª"),
    Cuerda("D3", 146.83f, "4ª"),
    Cuerda("G3", 196.0f, "3ª"),
    Cuerda("B3", 246.94f, "2ª"),
    Cuerda("E4", 329.63f, "1ª")
)

enum class Modo { AFINADOR, ACORDES }

@Composable
fun TunerScreen() {
    var modo by remember { mutableStateOf(Modo.AFINADOR) }
    var activo by remember { mutableStateOf(false) }
    var nota by remember { mutableStateOf<String?>(null) }
    var octava by remember { mutableIntStateOf(4) }
    var frecuencia by remember { mutableStateOf<Float?>(null) }
    var cents by remember { mutableFloatStateOf(0f) }
    var cuerdaDetectada by remember { mutableStateOf<Cuerda?>(null) }
    var advice by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FretBlack)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Afinador",
            color = FretText,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp
        )
        Text(
            text = "Afinación inteligente con Wilfredo 🎸",
            color = FretMuted,
            fontSize = 14.sp
        )

        // Toggle modo
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(FretSurface, RoundedCornerShape(20.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(Modo.AFINADOR to "🎵 Afinador", Modo.ACORDES to "🎸 Acordes").forEach { (m, label) ->
                val isSelected = modo == m
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) FretGold else Color.Transparent)
                        .clickable { modo = m },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) FretBlack else FretMuted,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        if (modo == Modo.AFINADOR) {
            // Cuerdas referencia
            Text(
                text = "Cuerdas de referencia",
                color = FretMuted,
                fontSize = 12.sp
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CUERDAS.chunked(3).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { c ->
                            val isDetectada = cuerdaDetectada?.nombre == c.nombre
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isDetectada) FretGold.copy(alpha = 0.2f) else FretSurface
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = c.numero,
                                        color = FretMuted,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = c.nombre,
                                        color = FretText,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "${c.freq.toInt()} Hz",
                                        color = FretMuted,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Display principal
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = FretSurface),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (nota != null) {
                        val color by animateColorAsState(
                            targetValue = when {
                                abs(cents) < 5 -> Color(0xFF9EF01A)
                                abs(cents) < 20 -> Color(0xFFE7C66A)
                                else -> Color(0xFFE94584)
                            },
                            label = "notaColor"
                        )

                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = nota!!,
                                color = color,
                                fontWeight = FontWeight.Black,
                                fontSize = 64.sp
                            )
                            Text(
                                text = "$octava",
                                color = FretMuted,
                                fontWeight = FontWeight.Bold,
                                fontSize = 32.sp
                            )
                        }

                        Text(
                            text = "${frecuencia?.toInt()} Hz",
                            color = FretMuted,
                            fontSize = 14.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Indicador
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(16.dp)
                                .background(Color(0xFF1F252F), RoundedCornerShape(50))
                        ) {
                            val indicatorPos = (50f + (cents / 50f) * 40f).coerceIn(5f, 95f)

                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .width(2.dp)
                                    .fillMaxHeight()
                                    .background(FretMuted)
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .padding(vertical = 2.dp)
                                    .width(16.dp)
                                    .offset(x = ((indicatorPos / 100f) * 280).dp)
                                    .background(color, CircleShape)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val estado = when {
                            abs(cents) < 5 -> "✅ Afinado"
                            cents > 0 -> "▲ Alto"
                            else -> "▼ Bajo"
                        }

                        Text(
                            text = "$estado (${if (cents > 0) "+" else ""}${cents.toInt()} cents)",
                            color = color,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    } else {
                        Column(
                            modifier = Modifier.padding(vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🎵", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (activo) "Tocando una cuerda..." else "Presiona iniciar para comenzar",
                                color = FretMuted,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            // Botón iniciar/detener
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { activo = !activo },
                    modifier = Modifier.weight(1f),
                    colors = if (activo) ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE94584),
                        contentColor = Color.White
                    ) else ButtonDefaults.buttonColors(
                        containerColor = FretGold,
                        contentColor = FretBlack
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(if (activo) "■ Detener" else "▶ Iniciar")
                }

                if (nota != null) {
                    Button(
                        onClick = {
                            advice = "🎸 Ajusta la clavija poco a poco. Gira lentamente y escucha el cambio de tono."
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FretSurface,
                            contentColor = FretGold
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("�� Consejo")
                    }
                }
            }

            if (advice.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = FretGold.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = advice,
                        modifier = Modifier.padding(14.dp),
                        color = FretText,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
            }
        } else {
            // Modo acordes
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = FretSurface),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🎸", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (activo) "Toca un acorde..." else "Presiona iniciar para detectar acordes",
                        color = FretMuted,
                        fontSize = 14.sp
                    )
                }
            }

            Button(
                onClick = { activo = !activo },
                modifier = Modifier.fillMaxWidth(),
                colors = if (activo) ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE94584),
                    contentColor = Color.White
                ) else ButtonDefaults.buttonColors(
                    containerColor = FretGold,
                    contentColor = FretBlack
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(if (activo) "■ Detener" else "▶ Iniciar")
            }
        }
    }
}