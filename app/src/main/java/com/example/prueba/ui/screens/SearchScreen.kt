package com.example.prueba.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.prueba.api.CancionResumenDto
import com.example.prueba.ui.theme.*
import com.example.prueba.viewmodel.SearchViewModel
import com.example.prueba.viewmodel.UiState

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
fun SearchScreen(
    viewModel: SearchViewModel = viewModel(),
    onSongClick: (Long) -> Unit = {}
) {
    var query by remember { mutableStateOf("") }
    val searchState by viewModel.searchState.collectAsState()

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
            onValueChange = {
                query = it
                viewModel.searchSongs(it)
            },
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

        when (val s = searchState) {
            UiState.Idle -> EstadoMensaje("Escribe para buscar canciones")
            UiState.Loading -> Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = FretGold)
            }
            is UiState.Error -> EstadoMensaje("⚠️ ${s.message}")
            is UiState.Success -> {
                val data = s.data
                if (data.canciones.isEmpty()) {
                    EstadoMensaje("No se encontraron canciones")
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(data.canciones) { cancion ->
                            SongCard(cancion = cancion, onClick = { onSongClick(cancion.songId) })
                        }

                        // Paginación: el backend indica si hay otra página.
                        if (data.hayMas) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (data.cargandoMas) {
                                        CircularProgressIndicator(
                                            color = FretGold,
                                            modifier = Modifier.size(28.dp),
                                            strokeWidth = 3.dp
                                        )
                                    } else {
                                        OutlinedButton(
                                            onClick = { viewModel.cargarMas() },
                                            shape = RoundedCornerShape(20.dp)
                                        ) {
                                            Text("Cargar más resultados", color = FretGold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EstadoMensaje(texto: String) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = texto,
            color = FretMuted,
            fontSize = 14.sp
        )
    }
}

@Composable
fun SongCard(cancion: CancionResumenDto, onClick: () -> Unit = {}) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
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

                cancion.dificultad?.let { nivel ->
                    Surface(
                        color = nivelBg[nivel] ?: FretSurface,
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = nivel,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            color = nivelColor[nivel] ?: FretGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (cancion.tieneAcordes) EtiquetaSong("Acordes")
                if (cancion.tienePlayer) EtiquetaSong("Reproductor")
                EtiquetaSong("${cancion.pistas} pistas")
            }
        }
    }
}

@Composable
private fun EtiquetaSong(texto: String) {
    Surface(
        color = Color(0xFF1A1F28),
        shape = RoundedCornerShape(50)
    ) {
        Text(
            text = texto,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            color = FretMuted,
            fontSize = 12.sp
        )
    }
}
