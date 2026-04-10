package com.example.prueba.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import com.example.prueba.R

@Composable
fun SplashScreen(navController: NavController) {

    // Animación suave: aparece y crece poquito
    val infinite = rememberInfiniteTransition(label = "splash")
    val alpha by infinite.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    val scaleAnim = remember { Animatable(0.92f) }
    LaunchedEffect(true) {
        scaleAnim.animateTo(1f, animationSpec = tween(650, easing = FastOutSlowInEasing))
        delay(1200)
        navController.navigate("home") {
            popUpTo("splash") { inclusive = true }
        }
    }

    val bg = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0B0D10),
            Color(0xFF151A22)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {

            Image(
                painter = painterResource(id = R.drawable.fretmind_logo),
                contentDescription = "FretMind Logo",
                modifier = Modifier
                    .size(300.dp)           // 👈 más grande
                    .scale(scaleAnim.value) // 👈 animación
                    .alpha(alpha)
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "FretMind",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFE7C66A) // doradito
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Domina la guitarra. Entrena tu mente musical.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFB8BDC7)
            )
        }
    }
}