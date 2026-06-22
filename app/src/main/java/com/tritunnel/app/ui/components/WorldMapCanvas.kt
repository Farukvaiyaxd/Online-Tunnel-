package com.tritunnel.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.tritunnel.app.ui.theme.DividerColor
import com.tritunnel.app.ui.theme.NeonBlue
import com.tritunnel.app.ui.theme.NeonCyan
import kotlin.math.abs

@Composable
fun WorldMapCanvas(
    modifier: Modifier = Modifier,
    serverLat: Double,
    serverLon: Double,
    isConnected: Boolean,
) {
    val inf = rememberInfiniteTransition(label = "map")

    val radarAlpha by inf.animateFloat(
        0.9f, 0f,
        infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart),
        label = "rAlpha"
    )
    val radarRadius by inf.animateFloat(
        0f, 60f,
        infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart),
        label = "rRadius"
    )
    val arcProgress by inf.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(3500, easing = LinearEasing), RepeatMode.Restart),
        label = "arc"
    )
    val devicePulse by inf.animateFloat(
        0f, 18f,
        infiniteRepeatable(tween(1500), RepeatMode.Reverse),
        label = "pulse"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        drawGrid()
        drawContinents()

        val srvX = ((serverLon + 180.0) / 360.0 * size.width).toFloat()
        val srvY = ((90.0 - serverLat) / 180.0 * size.height).toFloat()
        val devX = size.width * 0.5f
        val devY = size.height * 0.55f

        if (isConnected) {
            // radar rings at server
            for (i in 1..3) {
                drawCircle(
                    NeonCyan.copy(alpha = radarAlpha * (1f - i * 0.25f)),
                    radius = radarRadius * i * 0.5f,
                    center = Offset(srvX, srvY),
                    style = Stroke(2f)
                )
            }
            // animated arc dot
            val cp = Offset((devX + srvX) / 2f, ((devY + srvY) / 2f) - size.height * 0.2f)
            val dot = bezier(arcProgress, Offset(devX, devY), cp, Offset(srvX, srvY))
            drawArcPath(Offset(devX, devY), cp, Offset(srvX, srvY))
            drawCircle(NeonCyan, 6f, dot)
            drawCircle(NeonCyan.copy(alpha = 0.4f), 14f, dot)
        }

        // Device marker
        drawCircle(NeonBlue.copy(alpha = 0.25f), 10f + devicePulse, Offset(devX, devY))
        drawCircle(NeonBlue, 7f, Offset(devX, devY))
        drawCircle(Color.White, 3f, Offset(devX, devY))

        // Server marker
        val srvColor = if (isConnected) NeonCyan else NeonCyan.copy(alpha = 0.5f)
        drawCircle(srvColor.copy(alpha = 0.3f), 14f, Offset(srvX, srvY))
        drawCircle(srvColor, 7f, Offset(srvX, srvY))
        drawCircle(Color.White, 3f, Offset(srvX, srvY))
    }
}

private fun DrawScope.drawGrid() {
    val cols = 18; val rows = 9
    val dw = size.width / cols; val dh = size.height / rows
    val color = DividerColor.copy(alpha = 0.4f)
    repeat(cols + 1) { i -> drawLine(color, Offset(i * dw, 0f), Offset(i * dw, size.height)) }
    repeat(rows + 1) { j -> drawLine(color, Offset(0f, j * dh), Offset(size.width, j * dh)) }
}

private fun DrawScope.drawContinents() {
    val w = size.width; val h = size.height
    fun ll(lon: Double, lat: Double) = Offset(
        ((lon + 180) / 360 * w).toFloat(),
        ((90 - lat) / 180 * h).toFloat()
    )
    val fill = NeonCyan.copy(alpha = 0.08f)
    val stroke = NeonCyan.copy(alpha = 0.35f)

    // North America
    drawContinent(fill, stroke, listOf(
        ll(-165.0, 70.0), ll(-55.0, 70.0), ll(-55.0, 47.0), ll(-65.0, 44.0),
        ll(-80.0, 25.0), ll(-90.0, 10.0), ll(-105.0, 8.0), ll(-120.0, 15.0),
        ll(-135.0, 25.0), ll(-165.0, 60.0)
    ))
    // Greenland
    drawContinent(fill, stroke, listOf(
        ll(-55.0, 83.0), ll(-17.0, 83.0), ll(-17.0, 70.0), ll(-55.0, 70.0)
    ))
    // South America
    drawContinent(fill, stroke, listOf(
        ll(-82.0, 12.0), ll(-34.0, 5.0), ll(-35.0, -10.0),
        ll(-52.0, -55.0), ll(-75.0, -55.0), ll(-82.0, 5.0)
    ))
    // Europe
    drawContinent(fill, stroke, listOf(
        ll(-12.0, 71.0), ll(32.0, 71.0), ll(40.0, 55.0),
        ll(30.0, 35.0), ll(5.0, 35.0), ll(-12.0, 40.0)
    ))
    // Africa
    drawContinent(fill, stroke, listOf(
        ll(-18.0, 37.0), ll(52.0, 37.0), ll(52.0, 10.0),
        ll(42.0, -12.0), ll(18.0, -35.0), ll(14.0, -35.0),
        ll(-18.0, 15.0)
    ))
    // Asia
    drawContinent(fill, stroke, listOf(
        ll(28.0, 72.0), ll(170.0, 72.0), ll(145.0, 45.0),
        ll(130.0, 0.0), ll(100.0, -10.0), ll(65.0, 8.0),
        ll(52.0, 12.0), ll(28.0, 40.0)
    ))
    // Australia
    drawContinent(fill, stroke, listOf(
        ll(115.0, -18.0), ll(155.0, -18.0), ll(153.0, -30.0),
        ll(148.0, -43.0), ll(115.0, -38.0)
    ))
    // Japan/islands (dot)
    drawCircle(NeonCyan.copy(alpha = 0.25f), 8f, ll(138.0, 36.0))
    drawCircle(NeonCyan.copy(alpha = 0.25f), 6f, ll(125.0, 14.0))
}

private fun DrawScope.drawContinent(fill: Color, stroke: Color, points: List<Offset>) {
    if (points.size < 2) return
    val path = Path().apply {
        moveTo(points[0].x, points[0].y)
        points.drop(1).forEach { lineTo(it.x, it.y) }
        close()
    }
    drawPath(path, fill)
    drawPath(path, stroke, style = Stroke(1.5f))
}

private fun DrawScope.drawArcPath(p0: Offset, ctrl: Offset, p2: Offset) {
    val path = Path().apply {
        moveTo(p0.x, p0.y)
        quadraticBezierTo(ctrl.x, ctrl.y, p2.x, p2.y)
    }
    drawPath(path, NeonCyan.copy(alpha = 0.5f), style = Stroke(2f, cap = StrokeCap.Round))
}

private fun bezier(t: Float, p0: Offset, p1: Offset, p2: Offset): Offset {
    val mt = 1f - t
    return Offset(
        mt * mt * p0.x + 2 * mt * t * p1.x + t * t * p2.x,
        mt * mt * p0.y + 2 * mt * t * p1.y + t * t * p2.y
    )
}
