package com.tritunnel.app.core.v2ray

import com.tritunnel.app.core.Tunnel
import com.tritunnel.app.data.ServerConfig

/**
 * V2Ray (VMess/VLESS) tunnel।
 *
 * ⚠️ এখনো অসম্পূর্ণ — এটা চালু করতে Xray native core (libv2ray AAR) লাগবে।
 * পরের ধাপে আমরা এই AAR যোগ করে এখানে আসল কোড বসাব।
 *
 * পরিকল্পনা:
 *   1. app/libs/ ফোল্ডারে libv2ray.aar রাখা হবে
 *   2. এখানে Xray-কে config (JSON) দিয়ে চালু করা হবে
 *   3. Xray একটা লোকাল SOCKS5 inbound খুলবে (127.0.0.1:10808)
 */
class V2RayTunnel(
    private val config: ServerConfig,
    private val localSocksPort: Int = 10808,
) : Tunnel {

    override suspend fun start(onLog: (String) -> Unit) {
        onLog("V2Ray core এখনো যোগ করা হয়নি।")
        throw NotImplementedError(
            "V2Ray কানেকশন এখনো তৈরি হয়নি — পরের ধাপে Xray core যোগ করা হবে।"
        )
    }

    override fun stop() {}
}
