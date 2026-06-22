package com.tritunnel.app.core.openvpn

import com.tritunnel.app.core.Tunnel
import com.tritunnel.app.data.ServerConfig

/**
 * OpenVPN tunnel।
 *
 * ⚠️ এখনো অসম্পূর্ণ — এটা চালু করতে OpenVPN native core (ics-openvpn library) লাগবে।
 * পরের ধাপে আমরা ics-openvpn module যোগ করে এখানে আসল কোড বসাব।
 *
 * পরিকল্পনা:
 *   1. ics-openvpn-র core module প্রজেক্টে যোগ করা হবে
 *   2. .ovpn profile parse করে OpenVPN core চালু করা হবে
 *   3. OpenVPN নিজেই VpnService চালায়, তাই এটা আলাদাভাবে handle হবে
 */
class OpenVpnTunnel(
    private val config: ServerConfig,
) : Tunnel {

    override suspend fun start(onLog: (String) -> Unit) {
        onLog("OpenVPN core এখনো যোগ করা হয়নি।")
        throw NotImplementedError(
            "OpenVPN কানেকশন এখনো তৈরি হয়নি — পরের ধাপে ics-openvpn core যোগ করা হবে।"
        )
    }

    override fun stop() {}
}
