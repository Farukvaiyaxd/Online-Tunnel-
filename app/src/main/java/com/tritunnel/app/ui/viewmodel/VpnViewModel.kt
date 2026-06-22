package com.tritunnel.app.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.TrafficStats
import android.net.VpnService
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tritunnel.app.data.SubscriptionRepository
import com.tritunnel.app.data.VpnConfig
import com.tritunnel.app.data.VpnConfigRepository
import com.tritunnel.app.service.VpnTunnelService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL

class VpnViewModel(application: Application) : AndroidViewModel(application) {

    private val configRepo = VpnConfigRepository(application)
    private val subRepo = SubscriptionRepository(application)

    val serviceState = VpnTunnelService.state

    private val _servers = MutableStateFlow<List<VpnConfig>>(emptyList())
    val servers: StateFlow<List<VpnConfig>> = _servers.asStateFlow()

    private val _selected = MutableStateFlow<VpnConfig?>(null)
    val selected: StateFlow<VpnConfig?> = _selected.asStateFlow()

    private val _publicIp = MutableStateFlow("---.---.---.---")
    val publicIp: StateFlow<String> = _publicIp.asStateFlow()

    private val _latency = MutableStateFlow("-- ms")
    val latency: StateFlow<String> = _latency.asStateFlow()

    private val _downloadSpeed = MutableStateFlow(0L)
    val downloadSpeed: StateFlow<Long> = _downloadSpeed.asStateFlow()

    private val _uploadSpeed = MutableStateFlow(0L)
    val uploadSpeed: StateFlow<Long> = _uploadSpeed.asStateFlow()

    private val _networkAvailable = MutableStateFlow(true)
    val networkAvailable: StateFlow<Boolean> = _networkAvailable.asStateFlow()

    private val _subUrl = MutableStateFlow(subRepo.getUrl())
    val subUrl: StateFlow<String> = _subUrl.asStateFlow()

    private val _subStatus = MutableStateFlow("")
    val subStatus: StateFlow<String> = _subStatus.asStateFlow()

    private var speedJob: Job? = null
    private var lastRx = 0L
    private var lastTx = 0L

    init {
        loadServers()
        fetchPublicIp()
        monitorNetwork()

        viewModelScope.launch {
            serviceState.collect { state ->
                when (state) {
                    VpnTunnelService.ServiceState.CONNECTED -> startSpeedMonitor()
                    else -> stopSpeedMonitor()
                }
            }
        }
    }

    // ─── Servers ─────────────────────────────────────────────────────────────

    fun loadServers() {
        _servers.value = configRepo.getAll()
        if (_selected.value == null) _selected.value = _servers.value.firstOrNull()
    }

    fun selectServer(config: VpnConfig) {
        _selected.value = config
        pingLatency(config)
    }

    fun addOrUpdateServer(config: VpnConfig) {
        configRepo.save(config)
        loadServers()
        if (_selected.value == null || _selected.value?.id == config.id) {
            _selected.value = config
        }
    }

    fun deleteServer(id: String) {
        configRepo.delete(id)
        if (_selected.value?.id == id) _selected.value = null
        loadServers()
    }

    fun exportServers(secure: Boolean) = configRepo.exportJson(_servers.value, secure)

    // ─── VPN Connection ───────────────────────────────────────────────────────

    fun connect(context: Context) {
        val config = _selected.value ?: return
        context.startForegroundService(
            Intent(context, VpnTunnelService::class.java).apply {
                action = VpnTunnelService.ACTION_CONNECT
                putExtra(VpnTunnelService.EXTRA_CONFIG, config.toJson().toString())
            }
        )
    }

    fun disconnect(context: Context) {
        context.startService(
            Intent(context, VpnTunnelService::class.java).apply {
                action = VpnTunnelService.ACTION_DISCONNECT
            }
        )
    }

    fun getVpnPermissionIntent(context: Context): Intent? = VpnService.prepare(context)

    // ─── Latency ─────────────────────────────────────────────────────────────

    fun pingLatency(config: VpnConfig) {
        viewModelScope.launch(Dispatchers.IO) {
            _latency.value = "..."
            val t0 = System.currentTimeMillis()
            try {
                Socket().use { s ->
                    s.connect(InetSocketAddress(config.host, config.port), 5_000)
                    _latency.value = "${System.currentTimeMillis() - t0} ms"
                }
            } catch (_: Exception) {
                _latency.value = "timeout"
            }
        }
    }

    // ─── Public IP ───────────────────────────────────────────────────────────

    fun fetchPublicIp() {
        viewModelScope.launch(Dispatchers.IO) {
            _publicIp.value = "Fetching..."
            runCatching {
                URL("https://api.ipify.org").openConnection()
                    .apply { connectTimeout = 8_000; readTimeout = 8_000 }
                    .getInputStream().bufferedReader().readText().trim()
            }.onSuccess { _publicIp.value = it }.onFailure { _publicIp.value = "N/A" }
        }
    }

    // ─── Subscription (Web Panel Sync) ───────────────────────────────────────

    fun setSubUrl(url: String) {
        _subUrl.value = url
        subRepo.setUrl(url)
    }

    fun syncSubscription() {
        viewModelScope.launch {
            _subStatus.value = "Syncing..."
            subRepo.fetchConfigs()
                .onSuccess { configs ->
                    configs.forEach { configRepo.save(it) }
                    loadServers()
                    _subStatus.value = "✓ Synced ${configs.size} servers"
                    subRepo.setLastSync(System.currentTimeMillis())
                }
                .onFailure { _subStatus.value = "✗ ${it.message}" }
        }
    }

    // ─── Speed Monitor ────────────────────────────────────────────────────────

    private fun startSpeedMonitor() {
        speedJob?.cancel()
        lastRx = TrafficStats.getTotalRxBytes()
        lastTx = TrafficStats.getTotalTxBytes()
        speedJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(1_000)
                val rx = TrafficStats.getTotalRxBytes()
                val tx = TrafficStats.getTotalTxBytes()
                _downloadSpeed.value = maxOf(0L, rx - lastRx)
                _uploadSpeed.value = maxOf(0L, tx - lastTx)
                lastRx = rx; lastTx = tx
            }
        }
    }

    private fun stopSpeedMonitor() {
        speedJob?.cancel()
        _downloadSpeed.value = 0L
        _uploadSpeed.value = 0L
    }

    // ─── Network Monitor ─────────────────────────────────────────────────────

    private fun monitorNetwork() {
        val cm = getApplication<Application>()
            .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val req = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build()
        cm.registerNetworkCallback(req, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { _networkAvailable.value = true }
            override fun onLost(network: Network) { _networkAvailable.value = false }
        })
    }

    override fun onCleared() { stopSpeedMonitor(); super.onCleared() }
}

/** Human-readable speed string e.g. "1.2 MB/s" */
fun Long.toSpeedString(): String = when {
    this < 1_024 -> "$this B/s"
    this < 1_048_576 -> "${"%.1f".format(this / 1_024.0)} KB/s"
    else -> "${"%.2f".format(this / 1_048_576.0)} MB/s"
}
