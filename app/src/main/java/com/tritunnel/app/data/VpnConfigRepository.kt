package com.tritunnel.app.data

import android.content.Context
import org.json.JSONArray

class VpnConfigRepository(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("vpn_configs_v2", Context.MODE_PRIVATE)

    fun getAll(): List<VpnConfig> =
        VpnConfig.listFromJson(prefs.getString(KEY, "[]") ?: "[]")

    fun save(config: VpnConfig) {
        val list = getAll().toMutableList()
        val idx = list.indexOfFirst { it.id == config.id }
        if (idx >= 0) list[idx] = config else list.add(config)
        persist(list)
    }

    fun delete(id: String) = persist(getAll().filterNot { it.id == id })

    fun exportJson(configs: List<VpnConfig>, secure: Boolean): String {
        val arr = JSONArray()
        configs.forEach { arr.put(if (secure) it.toSecureJson() else it.toJson()) }
        return arr.toString(2)
    }

    private fun persist(list: List<VpnConfig>) =
        prefs.edit().putString(KEY, VpnConfig.listToJson(list)).apply()

    companion object { private const val KEY = "servers" }
}
