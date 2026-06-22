package com.tritunnel.app.data

import org.json.JSONArray
import org.json.JSONObject

/** কোন ধরনের tunnel — তিনটা protocol */
enum class TunnelType { SSH, V2RAY, OPENVPN }

/**
 * একটা সার্ভারের সব তথ্য। তিন protocol-এর জন্য আলাদা field আছে,
 * type অনুযায়ী যেগুলো দরকার সেগুলোই ব্যবহার হবে।
 */
data class ServerConfig(
    val id: String,
    val name: String,
    val type: TunnelType,

    // ---- SSH ----
    val host: String = "",
    val port: Int = 22,
    val username: String = "",
    val password: String = "",

    // ---- V2Ray ---- (vmess:// / vless:// লিংক অথবা সম্পূর্ণ JSON config)
    val v2rayLink: String = "",

    // ---- OpenVPN ---- (.ovpn ফাইলের সম্পূর্ণ লেখা)
    val ovpnProfile: String = "",
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("type", type.name)
        put("host", host)
        put("port", port)
        put("username", username)
        put("password", password)
        put("v2rayLink", v2rayLink)
        put("ovpnProfile", ovpnProfile)
    }

    companion object {
        fun fromJson(o: JSONObject): ServerConfig = ServerConfig(
            id = o.optString("id"),
            name = o.optString("name"),
            type = runCatching { TunnelType.valueOf(o.optString("type", "SSH")) }
                .getOrDefault(TunnelType.SSH),
            host = o.optString("host"),
            port = o.optInt("port", 22),
            username = o.optString("username"),
            password = o.optString("password"),
            v2rayLink = o.optString("v2rayLink"),
            ovpnProfile = o.optString("ovpnProfile"),
        )

        fun listToJson(list: List<ServerConfig>): String {
            val arr = JSONArray()
            list.forEach { arr.put(it.toJson()) }
            return arr.toString()
        }

        fun listFromJson(text: String): List<ServerConfig> {
            if (text.isBlank()) return emptyList()
            return runCatching {
                val arr = JSONArray(text)
                (0 until arr.length()).map { fromJson(arr.getJSONObject(it)) }
            }.getOrDefault(emptyList())
        }
    }
}
