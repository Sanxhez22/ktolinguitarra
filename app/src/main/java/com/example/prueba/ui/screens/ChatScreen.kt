package com.example.prueba.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.prueba.ui.theme.*

data class Mensaje(val contenido: String, val esWilfredo: Boolean)

private val SUGERENCIAS = listOf(
    "¿Cómo mejorar mi técnica de rasgueo?",
    "Dame un ejercicio de dedos",
    "Quiero aprender",
    "Cómo afinar mi guitarra?"
)

@Composable
fun ChatScreen() {
    var input by remember { mutableStateOf("") }
    var mensajes by remember { mutableStateOf(listOf<Mensaje>()) }
    var cargando by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FretBlack)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(FretGold, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("W", color = FretBlack, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    text = "Wilfredo",
                    color = FretText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = "🟢 Tu instructor de guitarra",
                    color = FretMuted,
                    fontSize = 13.sp
                )
            }
        }

        if (mensajes.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(FretGold.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🎸", fontSize = 28.sp)
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "¡Hola! Soy Wilfredo",
                    color = FretText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = "Tu instructor personal de guitarra acústica",
                    color = FretMuted,
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(20.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SUGERENCIAS.take(3).forEach { sugerencia ->
                        Surface(
                            modifier = Modifier.clickable {
                                mensajes = mensajes + Mensaje(sugerencia, false)
                                cargando = true
                            },
                            color = FretSurface,
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                text = sugerencia,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                color = FretText,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(mensajes) { msg ->
                    MessageBubble(mensaje = msg)
                }

                if (cargando) {
                    item {
                        Row {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(FretGold, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("W", color = FretBlack, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Spacer(Modifier.width(8.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = FretSurface),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text(
                                    text = "Escribiendo...",
                                    modifier = Modifier.padding(12.dp),
                                    color = FretMuted,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Input
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Pregúntale algo a Wilfredo...", color = FretMuted) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = FretText,
                    unfocusedTextColor = FretText,
                    focusedBorderColor = FretGold,
                    unfocusedBorderColor = FretSurface,
                    cursorColor = FretGold
                ),
                shape = RoundedCornerShape(20.dp),
                singleLine = false,
                maxLines = 3
            )
            Spacer(Modifier.width(10.dp))
            Button(
                onClick = {
                    if (input.isNotBlank()) {
                        mensajes = mensajes + Mensaje(input, false)
                        input = ""
                        cargando = true
                    }
                },
                enabled = input.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FretGold,
                    contentColor = FretBlack
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.Send, contentDescription = "Enviar")
            }
        }
    }
}

@Composable
fun MessageBubble(mensaje: Mensaje) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (mensaje.esWilfredo) Arrangement.Start else Arrangement.End
    ) {
        if (mensaje.esWilfredo) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(FretGold, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("W", color = FretBlack, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Spacer(Modifier.width(8.dp))
        }

        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (mensaje.esWilfredo) FretSurface else FretGold
            ),
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = if (mensaje.esWilfredo) 20.dp else 4.dp,
                bottomEnd = if (mensaje.esWilfredo) 4.dp else 20.dp
            )
        ) {
            Text(
                text = mensaje.contenido,
                modifier = Modifier.padding(12.dp),
                color = if (mensaje.esWilfredo) FretText else FretBlack,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}