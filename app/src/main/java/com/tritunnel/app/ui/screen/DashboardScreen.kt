package com.tritunnel.app.ui.screen

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tritunnel.app.service.VpnTunnelService.ServiceState
import com.tritunnel.app.ui.components.WorldMapCanvas
import com.tritunnel.app.ui.theme.CyberBg
import com.tritunnel.app.ui.theme.CyberSurface
import com.tritunnel.app.ui.theme.CyberSurface2
import com.tritunnel.app.ui.theme.DividerColor
import com.tritunnel.app.ui.theme.GlowCyan
import com.tritunnel.app.ui.theme.GlowRed
import com.tritunnel.app.ui.theme.NeonBlue
import com.tritunnel.app.ui.theme.NeonCyan
import com.tritunnel.app.ui.theme.NeonRed
import com.tritunnel.app.ui.theme.TextPrimary
import com.tritunnel.app.ui.theme.TextSecondary
import com.tritunnel.app.ui.viewmodel.VpnViewModel
import com.tritunnel.app.ui.viewmodel.toSpeedString

@Composable
fun DashboardScreen(vm: VpnViewModel) {
    val context = LocalContext.current
    val state by vm.serviceState.collectAsState()
    val selected by vm.selected.collectAsState()
    val publicIp by vm.publicIp.collectAsState()
    val latency by vm.latency.collectAsState()
    val dlSpeed by vm.downloadSpeed.collectAsState()
    val ulSpeed by vm.uploadSpeed.collectAsState()
    val hasNetwork by vm.networkAvailable.collectAsState()

    val vpnLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) vm.connect(context)
    }

    Column(
        Modifier.fillMaxSize().background(CyberBg),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Header ──────────────────────────────────────────────────────────
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "ONLINE TUNNEL",
                color = NeonCyan,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 3.sp,
            )
            AnimatedVisibility(!hasNetwork, enter = fadeIn(), exit = fadeOut()) {
                Icon(Icons.Default.SignalWifiOff, null, tint = NeonRed, modifier = Modifier.size(22.dp))
            }
        }

        // ── World Map ────────────────────────────────────────────────────────
        Box(
            Modifier
                .fillMaxWidth()
                .height(220.dp)
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(CyberSurface)
                .border(1.dp, DividerColor, RoundedCornerShape(12.dp))
        ) {
            WorldMapCanvas(
                serverLat = selected?.latitude ?: 37.09,
                serverLon = selected?.longitude ?: -95.71,
                isConnected = state == ServiceState.CONNECTED,
            )
        }

        Spacer(Modifier.height(16.dp))

        // ── Connect Button ───────────────────────────────────────────────────
        ConnectButton(state = state, onToggle = {
            when (state) {
                ServiceState.CONNECTED, ServiceState.CONNECTING -> vm.disconnect(context)
                ServiceState.DISCONNECTED -> {
                    val intent = vm.getVpnPermissionIntent(context)
                    if (intent != null) vpnLauncher.launch(intent) else vm.connect(context)
                }
            }
        })

        // ── Selected Server ──────────────────────────────────────────────────
        AnimatedVisibility(selected != null, enter = fadeIn(), exit = fadeOut()) {
            selected?.let { srv ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CyberSurface2)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(srv.flag, fontSize = 22.sp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(srv.name, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("${srv.protocol} • ${srv.host}:${srv.port}  •  SNI: ${srv.sni.ifBlank { "none" }}",
                            color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                    Text(latency, color = NeonCyan, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // ── Stats Row ────────────────────────────────────────────────────────
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Public IP card
            StatCard(
                modifier = Modifier.weight(1f),
                label = "PUBLIC IP",
                value = publicIp,
                icon = { Icon(Icons.Default.Public, null, tint = NeonBlue, modifier = Modifier.size(18.dp)) },
                onRefresh = { vm.fetchPublicIp() }
            )

            // Download speed
            SpeedCard(
                Modifier.weight(1f), "DOWNLOAD", dlSpeed.toSpeedString(),
                icon = Icons.Default.ArrowDownward, color = NeonCyan
            )

            // Upload speed
            SpeedCard(
                Modifier.weight(1f), "UPLOAD", ulSpeed.toSpeedString(),
                icon = Icons.Default.ArrowUpward, color = NeonBlue
            )
        }
    }
}

@Composable
private fun ConnectButton(
    state: ServiceState,
    onToggle: () -> Unit,
) {
    val inf = rememberInfiniteTransition(label = "btn")
    val glow by inf.animateFloat(
        0.3f, 1f,
        infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label = "glow"
    )
    val rotation by inf.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(3000, easing = LinearEasing)),
        label = "rot"
    )

    val (ring, label, center) = when (state) {
        ServiceState.CONNECTED -> Triple(NeonCyan, "DISCONNECT", NeonCyan.copy(alpha = 0.15f))
        ServiceState.CONNECTING -> Triple(NeonBlue, "CONNECTING", NeonBlue.copy(alpha = 0.15f * glow))
        ServiceState.DISCONNECTED -> Triple(DividerColor, "CONNECT", Color.Transparent)
    }

    Box(
        Modifier.padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .size(130.dp)
                .drawBehind {
                    val r = size.minDimension / 2f - 4f
                    drawCircle(center)
                    drawCircle(ring.copy(alpha = if (state == ServiceState.CONNECTED) glow else 1f),
                        radius = r, style = Stroke(3f))
                    if (state == ServiceState.CONNECTING) {
                        drawArc(
                            color = NeonBlue,
                            startAngle = rotation,
                            sweepAngle = 120f,
                            useCenter = false,
                            style = Stroke(4f)
                        )
                    }
                    if (state == ServiceState.CONNECTED) {
                        drawCircle(GlowCyan.copy(alpha = glow * 0.5f), r + 20f)
                    }
                }
                .clip(CircleShape)
                .clickable { onToggle() },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (state == ServiceState.CONNECTED) "●" else "◌",
                    color = if (state == ServiceState.CONNECTED) NeonCyan else TextSecondary,
                    fontSize = 24.sp
                )
                Text(
                    label,
                    color = if (state == ServiceState.DISCONNECTED) TextSecondary else NeonCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp,
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier,
    label: String,
    value: String,
    icon: @Composable () -> Unit,
    onRefresh: (() -> Unit)? = null,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(CyberSurface)
            .border(1.dp, DividerColor, RoundedCornerShape(10.dp))
            .padding(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            icon()
            Spacer(Modifier.width(4.dp))
            Text(label, color = TextSecondary, fontSize = 9.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.sp)
            if (onRefresh != null) {
                Spacer(Modifier.weight(1f))
                Icon(Icons.Default.Refresh, null, tint = TextSecondary,
                    modifier = Modifier.size(14.dp).clickable { onRefresh() })
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(value, color = TextPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun SpeedCard(
    modifier: Modifier,
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(CyberSurface)
            .border(1.dp, DividerColor, RoundedCornerShape(10.dp))
            .padding(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = color, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text(label, color = TextSecondary, fontSize = 9.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.sp)
        }
        Spacer(Modifier.height(4.dp))
        Text(value, color = color, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
    }
}
