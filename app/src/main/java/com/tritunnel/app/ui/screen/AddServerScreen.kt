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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tritunnel.app.data.Network
import com.tritunnel.app.data.Protocol
import com.tritunnel.app.data.VpnConfig
import com.tritunnel.app.ui.theme.CyberBg
import com.tritunnel.app.ui.theme.CyberSurface
import com.tritunnel.app.ui.theme.DividerColor
import com.tritunnel.app.ui.theme.NeonBlue
import com.tritunnel.app.ui.theme.NeonCyan
import com.tritunnel.app.ui.theme.NeonRed
import com.tritunnel.app.ui.theme.TextPrimary
import com.tritunnel.app.ui.theme.TextSecondary
import com.tritunnel.app.ui.viewmodel.VpnViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddServerScreen(
    vm: VpnViewModel,
    existingConfig: VpnConfig? = null,
    onBack: () -> Unit,
) {
    var name by remember { mutableStateOf(existingConfig?.name ?: "") }
    var host by remember { mutableStateOf(existingConfig?.host ?: "") }
    var port by remember { mutableStateOf(existingConfig?.port?.toString() ?: "443") }
    var protocol by remember { mutableStateOf(existingConfig?.protocol ?: Protocol.VMESS) }
    var uuid by remember { mutableStateOf(existingConfig?.uuid ?: "") }
    var password by remember { mutableStateOf(existingConfig?.password ?: "") }
    var sni by remember { mutableStateOf(existingConfig?.sni ?: "") }
    var path by remember { mutableStateOf(existingConfig?.path ?: "/") }
    var network by remember { mutableStateOf(existingConfig?.network ?: Network.TCP) }
    var tls by remember { mutableStateOf(existingConfig?.tls ?: true) }
    var country by remember { mutableStateOf(existingConfig?.country ?: "Unknown") }
    var flag by remember { mutableStateOf(existingConfig?.flag ?: "🌐") }
    var lat by remember { mutableStateOf(existingConfig?.latitude?.toString() ?: "0.0") }
    var lon by remember { mutableStateOf(existingConfig?.longitude?.toString() ?: "0.0") }

    // Import URL state
    var showImportDialog by remember { mutableStateOf(false) }
    var importUrl by remember { mutableStateOf("") }
    var importError by remember { mutableStateOf("") }

    // Server check state
    var checkStatus by remember { mutableStateOf("") }
    var checkLoading by remember { mutableStateOf(false) }

    val colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = NeonCyan, unfocusedBorderColor = DividerColor,
        focusedLabelColor = NeonCyan, unfocusedLabelColor = TextSecondary,
        cursorColor = NeonCyan, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
    )

    // Import URL dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false; importError = "" },
            containerColor = CyberSurface,
            title = {
                Text("Import Server URL", color = NeonCyan,
                    fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "vless://, vmess://, trojan:// URL পেস্ট করুন",
                        color = TextSecondary, fontSize = 12.sp
                    )
                    OutlinedTextField(
                        value = importUrl,
                        onValueChange = { importUrl = it; importError = "" },
                        placeholder = { Text("vless://...", color = TextSecondary.copy(alpha = 0.4f), fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = colors,
                        shape = RoundedCornerShape(8.dp),
                        minLines = 3,
                        maxLines = 5,
                    )
                    if (importError.isNotBlank()) {
                        Text(importError, color = NeonRed, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val parsed = vm.importFromUrl(importUrl.trim())
                    if (parsed != null) {
                        name = parsed.name
                        host = parsed.host
                        port = parsed.port.toString()
                        protocol = parsed.protocol
                        uuid = parsed.uuid
                        password = parsed.password
                        sni = parsed.sni
                        path = parsed.path
                        network = parsed.network
                        tls = parsed.tls
                        checkStatus = ""
                        showImportDialog = false
                        importUrl = ""
                        importError = ""
                    } else {
                        importError = "URL পার্স করা গেল না। সঠিক VLESS/VMess/Trojan URL দিন।"
                    }
                }) {
                    Text("Import", color = NeonCyan, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false; importError = "" }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    Scaffold(
        containerColor = CyberBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (existingConfig != null) "Edit Server" else "Add Server",
                        color = NeonCyan, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = TextSecondary) }
                },
                actions = {
                    IconButton(onClick = { showImportDialog = true }) {
                        Icon(Icons.Default.ContentPaste, null, tint = NeonCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CyberBg)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                val cfg = VpnConfig(
                    id = existingConfig?.id ?: java.util.UUID.randomUUID().toString(),
                    name = name.ifBlank { "Server" },
                    host = host.trim(), port = port.toIntOrNull() ?: 443,
                    protocol = protocol, uuid = uuid.trim(), password = password,
                    sni = sni.trim(), path = path.trim(), network = network, tls = tls,
                    country = country.trim(), flag = flag.trim(),
                    latitude = lat.toDoubleOrNull() ?: 0.0, longitude = lon.toDoubleOrNull() ?: 0.0,
                )
                vm.addOrUpdateServer(cfg)
                onBack()
            }, containerColor = NeonCyan, contentColor = CyberBg) {
                Icon(Icons.Default.Check, null)
            }
        }
    ) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).background(CyberBg)
                .verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ── Import URL Banner ───────────────────────────────────────────────
            Button(
                onClick = { showImportDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonCyan.copy(alpha = 0.15f),
                    contentColor = NeonCyan
                ),
                shape = RoundedCornerShape(10.dp),
            ) {
                Icon(Icons.Default.ContentPaste, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.size(8.dp))
                Text(
                    "URL থেকে Import করুন  (VLESS / VMess / Trojan)",
                    fontSize = 12.sp, fontFamily = FontFamily.Monospace
                )
            }

            // ── Server Check ────────────────────────────────────────────────────
            if (host.isNotBlank()) {
                Row(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CyberSurface)
                        .border(1.dp, DividerColor, RoundedCornerShape(10.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("SERVER CHECK", color = TextSecondary, fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
                        if (checkStatus.isNotBlank()) {
                            Text(
                                checkStatus,
                                color = when {
                                    checkStatus.startsWith("Online") -> NeonCyan
                                    checkStatus.startsWith("Offline") -> NeonRed
                                    else -> NeonBlue
                                },
                                fontSize = 12.sp, fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    if (checkLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = NeonCyan, strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(
                            onClick = {
                                val p = port.toIntOrNull() ?: 443
                                if (host.isNotBlank()) {
                                    checkLoading = true
                                    checkStatus = "Checking..."
                                    vm.checkServerOnline(host.trim(), p) { online, ms ->
                                        checkLoading = false
                                        checkStatus = if (online) "Online ✓  ($ms ms)" else "Offline ✗  (timeout)"
                                    }
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.NetworkCheck, null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            // ── Protocol ─────────────────────────────────────────────────────────
            SectionLabel("PROTOCOL")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Protocol.entries.forEach { p ->
                    FilterChip(
                        selected = protocol == p, onClick = { protocol = p },
                        label = { Text(p.name, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonCyan.copy(alpha = 0.2f),
                            selectedLabelColor = NeonCyan, labelColor = TextSecondary,
                        )
                    )
                }
            }

            CField("Server Name", name, { name = it }, colors = colors)
            CField("Host / IP", host, { host = it; checkStatus = "" }, "e.g. sg1.example.com", colors = colors)
            CField("Port", port, { port = it; checkStatus = "" }, "443", KeyboardType.Number, colors = colors)

            when (protocol) {
                Protocol.VMESS, Protocol.VLESS -> CField("UUID", uuid, { uuid = it }, "xxxxxxxx-...", colors = colors)
                Protocol.TROJAN -> CField("Password", password, { password = it }, colors = colors)
                Protocol.SSH -> CField("Password", password, { password = it }, colors = colors)
                Protocol.OVPN -> CField(".ovpn content", password, { password = it }, colors = colors)
            }

            SectionLabel("SNI / BYPASS HOST")
            CField("SNI Host (bypass)", sni, { sni = it }, "e.g. t.me", colors = colors)

            if (protocol != Protocol.SSH && protocol != Protocol.OVPN) {
                SectionLabel("TRANSPORT")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Network.entries.forEach { n ->
                        FilterChip(
                            selected = network == n, onClick = { network = n },
                            label = { Text(n.name, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonCyan.copy(alpha = 0.2f),
                                selectedLabelColor = NeonCyan, labelColor = TextSecondary,
                            )
                        )
                    }
                }
                if (network == Network.WS || network == Network.H2)
                    CField("Path", path, { path = it }, "/", colors = colors)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = tls, onClick = { tls = true },
                        label = { Text("TLS ON", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonCyan.copy(alpha = 0.2f),
                            selectedLabelColor = NeonCyan, labelColor = TextSecondary))
                    FilterChip(selected = !tls, onClick = { tls = false },
                        label = { Text("TLS OFF", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonCyan.copy(alpha = 0.2f),
                            selectedLabelColor = NeonCyan, labelColor = TextSecondary))
                }
            }

            SectionLabel("MAP LOCATION")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CField("Flag 🌐", flag, { flag = it }, modifier = Modifier.weight(0.4f), colors = colors)
                CField("Country", country, { country = it }, modifier = Modifier.weight(1f), colors = colors)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CField("Latitude", lat, { lat = it }, "37.0", KeyboardType.Decimal, Modifier.weight(1f), colors = colors)
                CField("Longitude", lon, { lon = it }, "-95.7", KeyboardType.Decimal, Modifier.weight(1f), colors = colors)
            }

            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, color = TextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
}

@Composable
private fun CField(
    label: String, value: String, onValue: (String) -> Unit,
    placeholder: String = "", keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier.fillMaxWidth(),
    colors: androidx.compose.material3.TextFieldColors,
) {
    OutlinedTextField(
        value = value, onValueChange = onValue, label = { Text(label) },
        placeholder = { Text(placeholder, color = TextSecondary.copy(alpha = 0.4f)) },
        modifier = modifier, colors = colors, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(8.dp),
    )
}
