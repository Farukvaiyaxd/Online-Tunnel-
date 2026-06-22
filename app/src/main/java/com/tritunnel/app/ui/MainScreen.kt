package com.tritunnel.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tritunnel.app.core.TunnelManager
import com.tritunnel.app.core.TunnelStatus
import com.tritunnel.app.data.ConfigStore
import com.tritunnel.app.data.ServerConfig
import com.tritunnel.app.data.TunnelType
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val store = remember { ConfigStore(context) }

    var servers by remember { mutableStateOf(store.load()) }
    var selectedId by remember { mutableStateOf(servers.firstOrNull()?.id) }
    var showAdd by remember { mutableStateOf(false) }

    val status = TunnelManager.status
    val active = TunnelManager.activeServer
    val selected = servers.firstOrNull { it.id == selectedId }

    fun persist(newList: List<ServerConfig>) {
        servers = newList
        store.save(newList)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("TriTunnel") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Default.Add, contentDescription = "যোগ করুন")
            }
        }
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            StatusCard(status = status, activeName = active?.name)

            ConnectButton(
                status = status,
                canConnect = selected != null,
                onConnect = { selected?.let { TunnelManager.connect(it) } },
                onDisconnect = { TunnelManager.disconnect() },
            )

            Text(
                "সার্ভার তালিকা",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )

            if (servers.isEmpty()) {
                Text(
                    "এখনো কোনো সার্ভার নেই। নিচের + বাটনে চাপ দিয়ে যোগ করুন।",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(servers, key = { it.id }) { s ->
                    ServerRow(
                        server = s,
                        selected = s.id == selectedId,
                        onClick = { selectedId = s.id },
                        onDelete = {
                            persist(servers.filterNot { it.id == s.id })
                            if (selectedId == s.id) selectedId = servers.firstOrNull()?.id
                        }
                    )
                }
            }

            LogBox(TunnelManager.logText)
        }
    }

    if (showAdd) {
        AddServerDialog(
            onDismiss = { showAdd = false },
            onSave = { cfg ->
                persist(servers + cfg)
                selectedId = cfg.id
                showAdd = false
            }
        )
    }
}

@Composable
private fun StatusCard(status: TunnelStatus, activeName: String?) {
    val (color, label) = when (status) {
        TunnelStatus.CONNECTED -> Color(0xFF2E7D32) to "কানেক্টেড"
        TunnelStatus.CONNECTING -> Color(0xFFF9A825) to "কানেক্ট হচ্ছে..."
        TunnelStatus.ERROR -> Color(0xFFC62828) to "ত্রুটি"
        TunnelStatus.DISCONNECTED -> Color(0xFF757575) to "ডিসকানেক্টেড"
    }
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(14.dp)
                    .background(color, CircleShape)
            )
            Column(Modifier.padding(start = 12.dp)) {
                Text(label, fontWeight = FontWeight.Bold)
                Text(
                    activeName ?: "কোনো সার্ভার নয়",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ConnectButton(
    status: TunnelStatus,
    canConnect: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
) {
    val connected = status == TunnelStatus.CONNECTED || status == TunnelStatus.CONNECTING
    Button(
        onClick = { if (connected) onDisconnect() else onConnect() },
        enabled = connected || canConnect,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
    ) {
        Text(if (connected) "ডিসকানেক্ট" else "কানেক্ট")
    }
}

@Composable
private fun ServerRow(
    server: ServerConfig,
    selected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = if (selected)
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        else CardDefaults.cardColors()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(server.name, fontWeight = FontWeight.Bold)
                Text(
                    "${server.type} • ${if (server.host.isNotEmpty()) server.host else "—"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "মুছুন")
            }
        }
    }
}

@Composable
private fun LogBox(text: String) {
    if (text.isBlank()) return
    Card(
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .verticalScroll(rememberScrollState()),
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddServerDialog(
    onDismiss: () -> Unit,
    onSave: (ServerConfig) -> Unit,
) {
    var type by remember { mutableStateOf(TunnelType.SSH) }
    var name by remember { mutableStateOf("") }
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("22") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var v2rayLink by remember { mutableStateOf("") }
    var ovpn by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val cfg = ServerConfig(
                    id = UUID.randomUUID().toString(),
                    name = name.ifBlank { "Server" },
                    type = type,
                    host = host.trim(),
                    port = port.toIntOrNull() ?: 22,
                    username = username.trim(),
                    password = password,
                    v2rayLink = v2rayLink.trim(),
                    ovpnProfile = ovpn,
                )
                onSave(cfg)
            }) { Text("সেভ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("বাতিল") } },
        title = { Text("নতুন সার্ভার") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TunnelType.entries.forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(t.name) }
                        )
                    }
                }
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("নাম") }, modifier = Modifier.fillMaxWidth()
                )
                when (type) {
                    TunnelType.SSH -> {
                        OutlinedTextField(
                            value = host, onValueChange = { host = it },
                            label = { Text("Host / IP") }, modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = port, onValueChange = { port = it },
                            label = { Text("Port") }, modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = username, onValueChange = { username = it },
                            label = { Text("Username") }, modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = password, onValueChange = { password = it },
                            label = { Text("Password") }, modifier = Modifier.fillMaxWidth()
                        )
                    }
                    TunnelType.V2RAY -> {
                        OutlinedTextField(
                            value = v2rayLink, onValueChange = { v2rayLink = it },
                            label = { Text("vmess:// / vless:// লিংক") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    TunnelType.OPENVPN -> {
                        OutlinedTextField(
                            value = ovpn, onValueChange = { ovpn = it },
                            label = { Text(".ovpn ফাইলের লেখা") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    )
}
