package com.tritunnel.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tritunnel.app.ui.theme.CyberBg
import com.tritunnel.app.ui.theme.CyberSurface
import com.tritunnel.app.ui.theme.DividerColor
import com.tritunnel.app.ui.theme.NeonBlue
import com.tritunnel.app.ui.theme.NeonCyan
import com.tritunnel.app.ui.theme.TextPrimary
import com.tritunnel.app.ui.theme.TextSecondary
import com.tritunnel.app.ui.viewmodel.VpnViewModel

@Composable
fun SettingsScreen(vm: VpnViewModel) {
    val subUrl by vm.subUrl.collectAsState()
    val subStatus by vm.subStatus.collectAsState()
    var urlField by remember(subUrl) { mutableStateOf(subUrl) }

    val tfColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = NeonCyan, unfocusedBorderColor = DividerColor,
        focusedLabelColor = NeonCyan, unfocusedLabelColor = TextSecondary,
        cursorColor = NeonCyan, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
    )

    Column(
        Modifier.fillMaxSize().background(CyberBg)
            .verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("SETTINGS", color = NeonCyan, fontSize = 16.sp,
            fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, letterSpacing = 4.sp)

        // ── Web Panel Subscription ────────────────────────────────────────────
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .background(CyberSurface)
                .border(1.dp, DividerColor, RoundedCornerShape(12.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CloudSync, null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                Text("  WEB PANEL SYNC", color = NeonCyan, fontSize = 12.sp,
                    fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
            }
            Text(
                "আপনার web panel-এর server list URL দিন। App এখান থেকে server গুলো নামিয়ে নেবে।",
                color = TextSecondary, fontSize = 12.sp
            )
            OutlinedTextField(
                value = urlField,
                onValueChange = { urlField = it },
                label = { Text("Subscription URL") },
                placeholder = { Text("https://panel.example.com/api/servers", color = TextSecondary.copy(alpha = 0.4f)) },
                modifier = Modifier.fillMaxWidth(),
                colors = tfColors, shape = RoundedCornerShape(8.dp), singleLine = true,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { vm.setSubUrl(urlField) },
                    colors = ButtonDefaults.buttonColors(containerColor = DividerColor),
                    modifier = Modifier.weight(1f)
                ) { Text("Save URL", color = TextPrimary, fontSize = 12.sp) }

                Button(
                    onClick = { vm.setSubUrl(urlField); vm.syncSubscription() },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    modifier = Modifier.weight(1f)
                ) { Text("Sync Now", color = CyberBg, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            }
            if (subStatus.isNotBlank()) {
                Text(subStatus, color = if (subStatus.startsWith("✓")) NeonCyan else TextSecondary,
                    fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            }
        }

        // ── JSON Format Guide ─────────────────────────────────────────────────
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .background(CyberSurface)
                .border(1.dp, DividerColor, RoundedCornerShape(12.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, null, tint = NeonBlue, modifier = Modifier.size(18.dp))
                Text("  API FORMAT", color = NeonBlue, fontSize = 12.sp,
                    fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
            }
            Text(
                "আপনার web panel API endpoint এই JSON format return করতে হবে:",
                color = TextSecondary, fontSize = 12.sp
            )
            Text(
                """[
  {
    "name": "SG-01",
    "host": "sg.example.com",
    "port": 443,
    "protocol": "VMESS",
    "uuid": "xxxx-...",
    "sni": "cdn.cloudflare.com",
    "path": "/ws",
    "network": "WS",
    "tls": true,
    "country": "Singapore",
    "flag": "🇸🇬",
    "latitude": 1.35,
    "longitude": 103.82
  }
]""",
                color = NeonCyan.copy(alpha = 0.8f),
                fontSize = 10.sp, fontFamily = FontFamily.Monospace,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp))
                    .background(CyberBg).padding(10.dp)
            )
        }

        // ── App Info ──────────────────────────────────────────────────────────
        Text("TriTunnel v0.2.0", color = TextSecondary.copy(alpha = 0.5f),
            fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        Spacer(Modifier.height(40.dp))
    }
}
