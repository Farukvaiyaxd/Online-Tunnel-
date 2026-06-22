package com.tritunnel.app.data

import org.json.JSONArray
import org.json.JSONObject
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
    }
}
