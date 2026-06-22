package com.tritunnel.app.core

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tritunnel.app.core.openvpn.OpenVpnTunnel
import com.tritunnel.app.core.ssh.SshTunnel
import com.tritunnel.app.core.v2ray.V2RayTunnel
import com.tritunnel.app.data.ServerConfig
import com.tritunnel.app.data.TunnelType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

enum class TunnelStatus { DISCONNECTED, CONNECTING, CONNECTED, ERROR }

/**
 * পুরো অ্যাপের কানেকশন অবস্থা এক জায়গায় রাখে।
 * Compose UI সরাসরি এই state পড়ে আপডেট হবে।
 */
object TunnelManager {

    var status by mutableStateOf(TunnelStatus.DISCONNECTED)
        private set

    var activeServer by mutableStateOf<ServerConfig?>(null)
        private set

    var logText by mutableStateOf("")
        private set

    private var tunnel: Tunnel? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun connect(config: ServerConfig) {
        if (status == TunnelStatus.CONNECTING || status == TunnelStatus.CONNECTED) return
        status = TunnelStatus.CONNECTING
        activeServer = config
        logText = ""
        appendLog("কানেক্ট হচ্ছে: ${config.name} (${config.type})")

        scope.launch {
            try {
                val t: Tunnel = when (config.type) {
                    TunnelType.SSH -> SshTunnel(config)
                    TunnelType.V2RAY -> V2RayTunnel(config)
                    TunnelType.OPENVPN -> OpenVpnTunnel(config)
                }
                t.start { line -> appendLog(line) }
                tunnel = t
                status = TunnelStatus.CONNECTED
                appendLog("কানেক্টেড ✓")
            } catch (e: Throwable) {
                status = TunnelStatus.ERROR
                appendLog("ত্রুটি: ${e.message}")
            }
        }
    }

    fun disconnect() {
        appendLog("ডিসকানেক্ট হচ্ছে...")
        runCatching { tunnel?.stop() }
        tunnel = null
        status = TunnelStatus.DISCONNECTED
        activeServer = null
        appendLog("ডিসকানেক্টেড")
    }

    private fun appendLog(line: String) {
        logText = if (logText.isEmpty()) line else "$logText\n$line"
    }
}
