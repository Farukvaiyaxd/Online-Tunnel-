package com.tritunnel.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily

val CyberColorScheme = darkColorScheme(
    background      = CyberBg,
    surface         = CyberSurface,
    surfaceVariant  = CyberSurface2,
    primary         = NeonCyan,
    secondary       = NeonBlue,
    error           = NeonRed,
    onBackground    = TextPrimary,
    onSurface       = TextPrimary,
    onSurfaceVariant = TextSecondary,
    onPrimary       = CyberBg,
    outline         = DividerColor,
)

@Composable
fun TriTunnelTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CyberColorScheme,
        content = content,
    )
}
