package com.example.prueba.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.prueba.api.BibliotecaItemDto
import com.example.prueba.ui.theme.FretBlack
import com.example.prueba.ui.theme.FretGold
import com.example.prueba.ui.theme.FretMuted
import com.example.prueba.ui.theme.FretSurface
import com.example.prueba.ui.theme.FretText
import com.example.prueba.viewmodel.BibliotecaViewModel
import com.example.prueba.viewmodel.UiState

/**
 * Biblioteca del usuario: tabs Guardadas / Favoritas, 100% datos reales
 * (GET /biblioteca). Cierra el flujo Buscar → Guardar → Mongo → Mostrar.
 */
@Composable
fun BibliotecaScreen(
    bibliotecaViewModel: BibliotecaViewModel = viewModel(),
    onSongClick: (Long) -> Unit = {},
    onBuscar: () -> Unit = {}
) {
    val state by bibliotecaViewModel.state.collectAsState()
    var tab by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { bibliotecaViewModel.load() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FretBlack)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Mi biblioteca",
            color = FretText,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp
        )

        TabRow(
            selectedTabIndex = tab,
            containerColor = FretBlack,
            contentColor = FretGold
        ) {
            Tab(selected = tab == 0, onClick = { tab = 0 }) {
                Text(
                    "Guardadas",
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = if (tab == 0) FretGold else FretMuted,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Tab(selected = tab == 1, onClick = { tab = 1 }) {
                Text(
                    "Favoritas",
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = if (tab == 1) FretGold else FretMuted,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        when (val s = state) {
            is UiState.Success -> {
                val items = if (tab == 1) s.data.filter { it.favorita } else s.data.filter { it.guardada }
                if (items.isEmpty()) {
                    BibliotecaVacia(
                        texto = if (tab == 1) "Aún no tienes canciones favoritas."
                        else "Aún no has guardado canciones.",
                        onBuscar = onBuscar
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(items, key = { it.songId }) { item ->
                            BibliotecaItemCard(
                                item = item,
                                onClick = { onSongClick(item.songId) },
                                onToggleFavorito = { bibliotecaViewModel.toggleFavorito(item) },
                                onQuitar = { bibliotecaViewModel.quitar(item) }
                            )
                        }
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }

            is UiState.Error -> Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("⚠️ ${s.message}", color = FretMuted, fontSize = 14.sp)
                Button(
                    onClick = { bibliotecaViewModel.load() },
                    colors = ButtonDefaults.buttonColors(containerColor = FretGold, contentColor = FretBlack)
                ) { Text("Reintentar") }
            }

            else -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 60.dp),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = FretGold) }
        }
    }
}

@Composable
private fun BibliotecaVacia(texto: String, onBuscar: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("🎵", fontSize = 44.sp)
        Text(texto, color = FretMuted, fontSize = 14.sp)
        Button(
            onClick = onBuscar,
            colors = ButtonDefaults.buttonColors(containerColor = FretGold, contentColor = FretBlack),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Buscar canciones", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun BibliotecaItemCard(
    item: BibliotecaItemDto,
    onClick: () -> Unit,
    onToggleFavorito: () -> Unit,
    onQuitar: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = FretSurface),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (item.portada != null) {
                AsyncImage(
                    model = item.portada,
                    contentDescription = null,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(FretBlack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.MusicNote, contentDescription = null, tint = FretGold)
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.titulo,
                    color = FretText,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
                Text(text = item.artista, color = FretMuted, fontSize = 13.sp)
                if (item.practicas > 0) {
                    Text(
                        text = "🎸 ${item.practicas} práctica(s)",
                        color = FretMuted,
                        fontSize = 11.sp
                    )
                }
            }

            IconButton(onClick = onToggleFavorito) {
                Icon(
                    imageVector = if (item.favorita) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorito",
                    tint = if (item.favorita) Color(0xFFE94584) else FretMuted
                )
            }
            TextButton(onClick = onQuitar) {
                Text("Quitar", color = FretMuted, fontSize = 12.sp)
            }
        }
    }
}
