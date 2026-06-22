package com.tritunnel.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

/** Web panel subscription — URL থেকে server list fetch করে। */
class SubscriptionRepository(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("subscription", Context.MODE_PRIVATE)

    fun getUrl(): String = prefs.getString(KEY_URL, "") ?: ""
    fun setUrl(url: String) = prefs.edit().putString(KEY_URL, url).apply()

    fun getLastSync(): Long = prefs.getLong(KEY_SYNC, 0L)
    fun setLastSync(t: Long) = prefs.edit().putLong(KEY_SYNC, t).apply()

    /**
     * Web panel URL থেকে JSON server list নামিয়ে parse করে।
     * JSON format: [{ name, host, port, protocol, uuid, sni, ... }, ...]
     */
    suspend fun fetchConfigs(): Result<List<VpnConfig>> = withContext(Dispatchers.IO) {
        val url = getUrl()
        if (url.isBlank()) return@withContext Result.failure(Exception("Subscription URL not set"))
        runCatching {
            val json = URL(url).openConnection().apply {
                connectTimeout = 10_000; readTimeout = 15_000
            }.getInputStream().bufferedReader().readText()
            VpnConfig.listFromJson(json)
        }
    }

    companion object {
        private const val KEY_URL = "sub_url"
        private const val KEY_SYNC = "last_sync"
    }
}
