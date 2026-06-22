package com.tritunnel.app.data

import android.net.Uri
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLDecoder
import java.util.UUID

enum class Protocol { VMESS, VLESS, TROJAN, SSH, OVPN }
enum class Network { TCP, WS, GRPC, H2 }

data class VpnConfig(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Server",
    val host: String = "",
    val port: Int = 443,
    val protocol: Protocol = Protocol.VMESS,
    val uuid: String = "",           // VMess/VLESS
    val password: String = "",       // Trojan/SSH
    val sni: String = "",            // SNI bypass hostname
    val path: String = "/",          // WebSocket path
    val network: Network = Network.TCP,
    val tls: Boolean = true,
    val country: String = "Unknown",
    val flag: String = "🌐",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id); put("name", name); put("host", host); put("port", port)
        put("protocol", protocol.name); put("uuid", uuid); put("password", password)
        put("sni", sni); put("path", path); put("network", network.name)
        put("tls", tls); put("country", country); put("flag", flag)
        put("latitude", latitude); put("longitude", longitude)
    }

    fun toSecureJson(): JSONObject = toJson().also {
        it.put("host", "***"); it.put("uuid", "***"); it.put("password", "***")
    }

    companion object {
        fun fromJson(o: JSONObject) = VpnConfig(
            id = o.optString("id", UUID.randomUUID().toString()),
            name = o.optString("name", "Server"),
            host = o.optString("host"),
            port = o.optInt("port", 443),
            protocol = runCatching { Protocol.valueOf(o.optString("protocol", "VMESS")) }
                .getOrDefault(Protocol.VMESS),
            uuid = o.optString("uuid"),
            password = o.optString("password"),
            sni = o.optString("sni"),
            path = o.optString("path", "/"),
            network = runCatching { Network.valueOf(o.optString("network", "TCP")) }
                .getOrDefault(Network.TCP),
            tls = o.optBoolean("tls", true),
            country = o.optString("country", "Unknown"),
            flag = o.optString("flag", "🌐"),
            latitude = o.optDouble("latitude", 0.0),
            longitude = o.optDouble("longitude", 0.0),
        )

        fun listFromJson(text: String): List<VpnConfig> = runCatching {
            val arr = JSONArray(text)
            (0 until arr.length()).map { fromJson(arr.getJSONObject(it)) }
        }.getOrDefault(emptyList())

        fun listToJson(list: List<VpnConfig>): String {
            val arr = JSONArray(); list.forEach { arr.put(it.toJson()) }
            return arr.toString()
        }

        // ── URL Import ─────────────────────────────────────────────────────────

        fun parseUrl(url: String): VpnConfig? {
            val trimmed = url.trim()
            return when {
                trimmed.startsWith("vless://") -> parseVlessUrl(trimmed)
                trimmed.startsWith("vmess://") -> parseVmessUrl(trimmed)
                trimmed.startsWith("trojan://") -> parseTrojanUrl(trimmed)
                else -> null
            }
        }

        private fun networkFromType(type: String?): Network = when (type?.lowercase()) {
            "ws" -> Network.WS
            "grpc" -> Network.GRPC
            "h2" -> Network.H2
            else -> Network.TCP
        }

        private fun decodeName(raw: String?): String = runCatching {
            URLDecoder.decode(raw ?: "", "UTF-8").ifBlank { "Imported Server" }
        }.getOrDefault(raw ?: "Imported Server")

        private fun parseVlessUrl(url: String): VpnConfig? = runCatching {
            // vless://UUID@host:port?type=tcp&security=tls&sni=x.com#ConfigName
            val uri = Uri.parse(url)
            val uuid = uri.userInfo ?: return@runCatching null
            val host = uri.host ?: return@runCatching null
            val port = uri.port.takeIf { it > 0 } ?: 443
            val name = decodeName(uri.fragment)
            val sni = uri.getQueryParameter("sni") ?: ""
            val security = uri.getQueryParameter("security") ?: ""
            val tls = security.equals("tls", ignoreCase = true) ||
                    security.equals("reality", ignoreCase = true)
            val type = uri.getQueryParameter("type") ?: "tcp"
            val network = networkFromType(type)
            val path = uri.getQueryParameter("path") ?: "/"

            VpnConfig(
                name = name,
                host = host,
                port = port,
                protocol = Protocol.VLESS,
                uuid = uuid,
                sni = sni,
                path = path,
                network = network,
                tls = tls,
            )
        }.getOrNull()

        private fun parseVmessUrl(url: String): VpnConfig? = runCatching {
            // vmess://BASE64(JSON)
            val b64 = url.removePrefix("vmess://").trim()
            val json = JSONObject(String(Base64.decode(b64, Base64.DEFAULT or Base64.URL_SAFE)))
            val name = json.optString("ps", "VMess Server").ifBlank { "VMess Server" }
            val host = json.optString("add").ifBlank { return@runCatching null }
            val port = json.optString("port", "443").toIntOrNull() ?: 443
            val uuid = json.optString("id")
            val net = json.optString("net", "tcp")
            val tls = json.optString("tls").equals("tls", ignoreCase = true)
            val sni = json.optString("sni", json.optString("host", ""))
            val path = json.optString("path", "/")
            val network = networkFromType(net)

            VpnConfig(
                name = name,
                host = host,
                port = port,
                protocol = Protocol.VMESS,
                uuid = uuid,
                sni = sni,
                path = path,
                network = network,
                tls = tls,
            )
        }.getOrNull()

        private fun parseTrojanUrl(url: String): VpnConfig? = runCatching {
            // trojan://password@host:port?security=tls&sni=x.com#ConfigName
            val uri = Uri.parse(url)
            val password = uri.userInfo ?: ""
            val host = uri.host ?: return@runCatching null
            val port = uri.port.takeIf { it > 0 } ?: 443
            val name = decodeName(uri.fragment)
            val sni = uri.getQueryParameter("sni") ?: ""
            val type = uri.getQueryParameter("type") ?: "tcp"
            val network = networkFromType(type)

            VpnConfig(
                name = name,
                host = host,
                port = port,
                protocol = Protocol.TROJAN,
                password = password,
                sni = sni,
                network = network,
                tls = true,
            )
        }.getOrNull()
    }
}
