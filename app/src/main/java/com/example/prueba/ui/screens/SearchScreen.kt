package com.example.prueba.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.prueba.ui.theme.*

data class Song(
    val titulo: String,
    val artista: String,
    val nivel: String,
    val acordes: List<String>
)

private val CANCIONES = listOf(
    Song("Do I Wanna Know?", "Arctic Monkeys", "Intermedio", listOf("Am", "C", "G", "Em")),
    Song("Come As You Are", "Nirvana", "Fácil", listOf("Em", "D", "C")),
    Song("505", "Arctic Monkeys", "Intermedio", listOf("Dm", "Am", "C", "G")),
    Song("Wonderwall", "Oasis", "Fácil", listOf("Em7", "G", "Dsus4", "A7sus4")),
    Song("Hotel California", "Eagles", "Avanzado", listOf("Bm", "F#", "A", "E", "G", "D", "Em")),
    Song("Stairway to Heaven", "Led Zeppelin", "Avanzado", listOf("Am", "G", "F", "C")),
    Song("Knockin on Heavens Door", "Bob Dylan", "Fácil", listOf("G", "D", "Am")),
    Song("Nothing Else Matters", "Metallica", "Avanzado", listOf("Em", "Am", "C", "D", "G"))
)

private val nivelColor = mapOf(
    "Fácil" to Color(0xFF9EF01A),
    "Intermedio" to FretGold,
    "Avanzado" to Color(0xFFE94584)
)

private val nivelBg = mapOf(
    "Fácil" to Color(0xFF1A2A10),
    "Intermedio" to Color(0xFF2A2A10),
    "Avanzado" to Color(0xFF2A101A)
)

@Composable
fun SearchScreen() {
    var query by remember { mutableStateOf("") }

    val filtradas = CANCIONES.filter { c ->
        c.titulo.contains(query, ignoreCase = true) ||
        c.artista.contains(query, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FretBlack)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Buscar",
            color = FretText,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp
        )

        Text(
            text = "Encuentra canciones para practicar",
            color = FretMuted,
            fontSize = 14.sp
        )

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Buscar canciones o artistas...", color = FretMuted) },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = FretMuted
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = FretText,
                unfocusedTextColor = FretText,
                focusedBorderColor = FretGold,
                unfocusedBorderColor = FretSurface,
                cursorColor = FretGold
            ),
            shape = RoundedCornerShape(20.dp),
            singleLine = true
        )

        if (filtradas.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No se encontraron canciones",
                    color = FretMuted,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtradas) { cancion ->
                    SongCard(cancion = cancion)
                }
            }
        }
    }
}

@Composable
fun SongCard(cancion: Song) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = FretSurface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = cancion.titulo,
                        color = FretText,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = cancion.artista,
                        color = FretMuted,
                        fontSize = 13.sp
                    )
                }

                Surface(
                    color = nivelBg[cancion.nivel] ?: FretSurface,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = cancion.nivel,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = nivelColor[cancion.nivel] ?: FretGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                cancion.acordes.forEach { acorde ->
                    Surface(
                        color = Color(0xFF1A1F28),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = acorde,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            color = FretMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}