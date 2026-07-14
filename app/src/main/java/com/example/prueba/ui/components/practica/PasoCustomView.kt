package com.example.prueba.ui.components.practica

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.prueba.data.model.CatalogoAcordes
import com.example.prueba.data.model.NotasGuitarra
import com.example.prueba.ui.components.DiagramaAcorde
import com.example.prueba.ui.components.EstadoNota
import com.example.prueba.ui.components.FretboardView
import com.example.prueba.ui.components.NotaDiapason
import com.example.prueba.ui.theme.FretGold
import com.example.prueba.ui.theme.FretMuted
import com.example.prueba.ui.theme.FretSurface
import com.example.prueba.ui.theme.FretText
import com.example.prueba.viewmodel.LiveState

/**
 * CUSTOM — el backend define la representación con los propios objetivos,
 * sin romper la arquitectura: si un objetivo es un acorde del catálogo se
 * dibuja su diagrama; si es una nota, el diapasón; si es una etiqueta
 * libre ("lectura"), una tarjeta con la consigna del paso.
 */
@Composable
fun GuiaCustom(state: LiveState) {
    RepresentacionCustom(state)
}

/** CUSTOM en vivo: misma representación + progreso de actividad. */
@Composable
fun PasoCustom(state: LiveState) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        RepresentacionCustom(state)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "${state.aciertosPaso} / ${state.esperadosPaso}",
            color = FretGold,
            fontWeight = FontWeight.Black,
            fontSize = 40.sp
        )
        Text("actividad detectada · sigue tocando", color = FretMuted, fontSize = 13.sp)
    }
}

@Composable
private fun RepresentacionCustom(state: LiveState) {
    val objetivo = state.objetivos.firstOrNull()
    val forma = remember(objetivo) { objetivo?.let { CatalogoAcordes.buscar(it) } }
    val posicion = remember(objetivo) { objetivo?.let { NotasGuitarra.posicionDe(it) } }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        when {
            forma != null -> DiagramaAcorde(forma = forma, modifier = Modifier.width(190.dp))
            posicion != null -> FretboardView(
                notas = listOf(NotaDiapason(posicion, EstadoNota.OBJETIVO)),
                modifier = Modifier.fillMaxWidth()
            )
            else -> Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = FretSurface),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🎯", fontSize = 34.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = state.instruccion,
                        color = FretText,
                        fontSize = 15.sp,
                        lineHeight = 21.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
