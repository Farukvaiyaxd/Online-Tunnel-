package com.tritunnel.app.data

import android.content.Context

/** সার্ভার লিস্ট ফোনে সেভ/লোড করে (SharedPreferences-এ JSON হিসেবে)। */
class ConfigStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("tritunnel_servers", Context.MODE_PRIVATE)

    fun load(): List<ServerConfig> =
        ServerConfig.listFromJson(prefs.getString(KEY, "") ?: "")

    fun save(list: List<ServerConfig>) {
        prefs.edit().putString(KEY, ServerConfig.listToJson(list)).apply()
    }

    companion object {
        private const val KEY = "servers"
    }
}
