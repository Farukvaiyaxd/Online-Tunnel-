package com.tritunnel.app.ui.screen

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tritunnel.app.data.VpnConfig
import com.tritunnel.app.ui.theme.CyberBg
import com.tritunnel.app.ui.theme.CyberSurface
import com.tritunnel.app.ui.theme.CyberSurface2
import com.tritunnel.app.ui.theme.DividerColor
import com.tritunnel.app.ui.theme.NeonCyan
import com.tritunnel.app.ui.theme.NeonRed
import com.tritunnel.app.ui.theme.TextPrimary
import com.tritunnel.app.ui.theme.TextSecondary
import com.tritunnel.app.ui.viewmodel.VpnViewModel

@Composable
fun ServerListScreen(
    vm: VpnViewModel,
    onAddServer: () -> Unit,
    onEditServer: (VpnConfig) -> Unit,
) {
    val servers by vm.servers.collectAsState()
    val selected by vm.selected.collectAsState()
    var deleteTarget by remember { mutableStateOf<VpnConfig?>(null) }
    var showExport by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = CyberBg,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddServer,
                containerColor = NeonCyan,
                contentColor = CyberBg
            ) { Icon(Icons.Default.Add, null) }
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).background(CyberBg)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("SERVERS", color = NeonCyan, fontSize = 16.sp,
                    fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, letterSpacing = 4.sp)
                IconButton(onClick = { showExport = true }) {
                    Icon(Icons.Default.IosShare, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                }
            }

            if (servers.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("NO SERVERS", color = TextSecondary, fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace, letterSpacing = 4.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("Tap + to add or sync from web panel",
                            color = TextSecondary.copy(alpha = 0.6f), fontSize = 12.sp)
                    }
                }
            } else {
                LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { Spacer(Modifier.height(4.dp)) }
                    items(servers, key = { it.id }) { srv ->
                        ServerRow(
                            server = srv,
                            isSelected = srv.id == selected?.id,
                            onClick = {
                                vm.selectServer(srv)
                                vm.pingLatency(srv)
                            },
                            onEdit = { onEditServer(srv) },
                            onDelete = { deleteTarget = srv }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }

    deleteTarget?.let { srv ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete Server?", color = TextPrimary) },
            text = { Text("\"${srv.name}\" মুছে ফেলবেন?", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = { vm.deleteServer(srv.id); deleteTarget = null }) {
                    Text("Delete", color = NeonRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            },
            containerColor = CyberSurface,
        )
    }

    if (showExport) {
        ExportDialog(
            onDismiss = { showExport = false },
            onExport = { secure ->
                showExport = false
                // In a real app: share as file/clipboard
            }
        )
    }
}

@Composable
private fun ServerRow(
    server: VpnConfig,
    isSelected: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) CyberSurface2 else CyberSurface)
            .border(
                1.dp,
                if (isSelected) NeonCyan.copy(alpha = 0.6f) else DividerColor,
                RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(server.flag, fontSize = 26.sp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(server.name, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                if (isSelected) {
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Default.CheckCircle, null, tint = NeonCyan, modifier = Modifier.size(14.dp))
                }
            }
            Text(
                "${server.protocol.name} • ${server.host}:${server.port}",
                color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace
            )
            AnimatedVisibility(server.sni.isNotBlank(), enter = fadeIn(), exit = fadeOut()) {
                Text("SNI: ${server.sni}", color = NeonCyan.copy(alpha = 0.7f),
                    fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Delete, null, tint = NeonRed.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun ExportDialog(onDismiss: () -> Unit, onExport: (Boolean) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Servers", color = TextPrimary) },
        text = { Text("কোন format-এ export করবেন?", color = TextSecondary) },
        confirmButton = {
            TextButton(onClick = { onExport(false) }) { Text("Plaintext JSON", color = NeonCyan) }
        },
        dismissButton = {
            TextButton(onClick = { onExport(true) }) { Text("Secure JSON (IP Hidden)", color = TextSecondary) }
        },
        containerColor = CyberSurface,
    )
}
