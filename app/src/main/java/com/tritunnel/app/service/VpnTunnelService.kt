package com.tritunnel.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.tritunnel.app.MainActivity
import com.tritunnel.app.data.VpnConfig
import com.tritunnel.app.tunnel.TcpTunnel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.net.DatagramSocket

class VpnTunnelService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var tunnel: TcpTunnel? = null
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(serviceJob + Dispatchers.IO)

    enum class ServiceState { DISCONNECTED, CONNECTING, CONNECTED }

    companion object {
        const val ACTION_CONNECT = "com.onlinetunnel.CONNECT"
        const val ACTION_DISCONNECT = "com.onlinetunnel.DISCONNECT"
        const val EXTRA_CONFIG = "extra_config_json"
        const val CHANNEL_ID = "online_tunnel_vpn"
        const val NOTIF_ID = 1001

        private val _state = MutableStateFlow(ServiceState.DISCONNECTED)
        val state = _state.asStateFlow()
        var activeConfig: VpnConfig? = null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return when (intent?.action) {
            ACTION_DISCONNECT -> { doDisconnect(); START_NOT_STICKY }
            ACTION_CONNECT -> {
                val config = runCatching {
                    VpnConfig.fromJson(JSONObject(intent.getStringExtra(EXTRA_CONFIG) ?: "{}"))
                }.getOrNull()
                if (config != null) doConnect(config) else doDisconnect()
                START_STICKY
            }
            else -> START_STICKY
        }
    }

    private fun doConnect(config: VpnConfig) {
        _state.value = ServiceState.CONNECTING
        activeConfig = config
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification(
            "Connecting...", "Online Tunnel • ${config.name}", ServiceState.CONNECTING
        ))

        try {
            val builder = Builder()
                .setSession("Online Tunnel")
                .addAddress("10.8.0.2", 24)
                .addRoute("0.0.0.0", 0)       // Route all IPv4
                .addDnsServer("1.1.1.1")
                .addDnsServer("8.8.8.8")
                .setMtu(1500)
                .addDisallowedApplication(packageName)

            val iface = builder.establish()
            vpnInterface = iface

            if (iface != null) {
                // Start packet tunnel
                tunnel = TcpTunnel(
                    fd = iface.fileDescriptor,
                    config = config,
                    protectTcp = { socket -> protect(socket) },
                    protectUdp = { socket -> protectDatagram(socket) },
                    scope = serviceScope,
                )
                tunnel?.start()

                _state.value = ServiceState.CONNECTED
                val nm = getSystemService(NotificationManager::class.java)
                nm.notify(NOTIF_ID, buildNotification(
                    "Connected",
                    "${config.name} • ${config.host}:${config.port}",
                    ServiceState.CONNECTED
                ))
            } else {
                _state.value = ServiceState.DISCONNECTED
                activeConfig = null
                stopSelf()
            }
        } catch (_: Exception) {
            _state.value = ServiceState.DISCONNECTED
            activeConfig = null
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun doDisconnect() {
        tunnel?.stop()
        tunnel = null
        runCatching { vpnInterface?.close() }
        vpnInterface = null
        _state.value = ServiceState.DISCONNECTED
        activeConfig = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun protectDatagram(socket: DatagramSocket): Boolean = protect(socket)

    override fun onRevoke() { doDisconnect(); super.onRevoke() }

    override fun onDestroy() {
        serviceJob.cancel()
        doDisconnect()
        super.onDestroy()
    }

    // ── Notification ─────────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                CHANNEL_ID, "Online Tunnel VPN",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "VPN connection status"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }

    private fun buildNotification(title: String, text: String, state: ServiceState): Notification {
        val openPi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val disconnectPi = PendingIntent.getService(
            this, 1,
            Intent(this, VpnTunnelService::class.java).apply { action = ACTION_DISCONNECT },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val cancelPi = PendingIntent.getService(
            this, 2,
            Intent(this, VpnTunnelService::class.java).apply { action = ACTION_DISCONNECT },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(openPi)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)

        when (state) {
            ServiceState.CONNECTING -> {
                builder.setProgress(0, 0, true)
                builder.addAction(
                    android.R.drawable.ic_menu_close_clear_cancel,
                    "Cancel", cancelPi
                )
            }
            ServiceState.CONNECTED -> {
                builder.addAction(
                    android.R.drawable.ic_lock_lock,
                    "Disconnect", disconnectPi
                )
                builder.addAction(
                    android.R.drawable.ic_menu_close_clear_cancel,
                    "Cancel", cancelPi
                )
            }
            ServiceState.DISCONNECTED -> {}
        }

        return builder.build()
    }
}
