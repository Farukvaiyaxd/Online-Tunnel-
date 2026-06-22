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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

class VpnTunnelService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null

    enum class ServiceState { DISCONNECTED, CONNECTING, CONNECTED }

    companion object {
        const val ACTION_CONNECT = "com.tritunnel.CONNECT"
        const val ACTION_DISCONNECT = "com.tritunnel.DISCONNECT"
        const val EXTRA_CONFIG = "extra_config_json"
        const val CHANNEL_ID = "tritunnel_vpn"
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
        startForeground(NOTIF_ID, buildNotification("Connecting...", config.name))

        try {
            val builder = Builder()
                .setSession("TriTunnel")
                .addAddress("10.8.0.2", 24)
                .addRoute("0.0.0.0", 0)       // Route all IPv4
                .addDnsServer("1.1.1.1")
                .addDnsServer("8.8.8.8")
                .setMtu(1500)
                .addDisallowedApplication(packageName) // prevent routing loop

            vpnInterface = builder.establish()

            if (vpnInterface != null) {
                _state.value = ServiceState.CONNECTED
                startForeground(NOTIF_ID, buildNotification("🔒 Connected", "${config.name} • ${config.host}"))
            } else {
                _state.value = ServiceState.DISCONNECTED
                activeConfig = null
                stopSelf()
            }
            // NOTE: Actual packet forwarding (SNI bypass, VMESS/Trojan tunneling)
            // requires integrating a native core (Xray/sing-box AAR).
            // The TUN interface is established here — traffic routing point is ready.
        } catch (e: Exception) {
            _state.value = ServiceState.DISCONNECTED
            activeConfig = null
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun doDisconnect() {
        runCatching { vpnInterface?.close() }
        vpnInterface = null
        _state.value = ServiceState.DISCONNECTED
        activeConfig = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onRevoke() { doDisconnect(); super.onRevoke() }
    override fun onDestroy() { doDisconnect(); super.onDestroy() }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL_ID, "VPN Status", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }

    private fun buildNotification(title: String, text: String): Notification {
        val pi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopPi = PendingIntent.getService(
            this, 1,
            Intent(this, VpnTunnelService::class.java).apply { action = ACTION_DISCONNECT },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(pi)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Disconnect", stopPi)
            .setOngoing(true)
            .build()
    }

}
