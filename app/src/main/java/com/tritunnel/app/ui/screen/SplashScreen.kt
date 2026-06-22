package com.tritunnel.app.ui.screen

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tritunnel.app.ui.theme.CyberBg
import com.tritunnel.app.ui.theme.NeonCyan
import com.tritunnel.app.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val alpha = remember { Animatable(0f) }
    val scale = remember { Animatable(0.7f) }

    LaunchedEffect(Unit) {
        alpha.animateTo(1f, tween(800))
        scale.animateTo(1f, tween(800))
        delay(1600)
        alpha.animateTo(0f, tween(500))
        onFinished()
    }

    Box(
        Modifier.fillMaxSize().background(CyberBg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier.alpha(alpha.value).scale(scale.value),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "ONLINE",
                color = NeonCyan,
                fontSize = 48.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 8.sp,
            )
            Text(
                "TUNNEL",
                color = NeonCyan.copy(alpha = 0.7f),
                fontSize = 24.sp,
                fontWeight = FontWeight.Light,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 16.sp,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "SECURE  •  FAST  •  FREE",
                color = TextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 4.sp,
            )
        }
    }
}
