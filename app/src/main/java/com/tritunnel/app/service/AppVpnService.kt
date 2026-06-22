package com.tritunnel.app.service

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor

/**
 * VpnService — পরের ধাপে এখানে TUN ইন্টারফেস বানিয়ে পুরো ফোনের ট্রাফিক
 * লোকাল SOCKS5 proxy (tunnel) দিয়ে পাঠানো হবে (tun2socks ব্যবহার করে)।
 *
 * এখন এটা শুধু কাঠামো হিসেবে আছে — milestone 1-এ অ্যাপ SSH কানেকশন
 * তৈরি করে SOCKS5 proxy খোলে, কিন্তু পুরো-ফোন রাউটিং এখনো যোগ হয়নি।
 */
class AppVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // TODO (পরের ধাপ): Builder() দিয়ে TUN তৈরি করা + tun2socks চালু করা
        return START_STICKY
    }

    override fun onDestroy() {
        runCatching { vpnInterface?.close() }
        vpnInterface = null
        super.onDestroy()
    }
}
