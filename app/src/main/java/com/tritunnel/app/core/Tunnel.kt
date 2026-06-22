package com.tritunnel.app.core

/**
 * সব protocol-এর common interface।
 * প্রতিটা tunnel একটা লোকাল SOCKS5 proxy খোলে (127.0.0.1:localSocksPort),
 * পরে VpnService সেই proxy দিয়ে পুরো ফোনের ট্রাফিক পাঠাবে।
 */
interface Tunnel {
    /** কানেকশন তৈরি করে। সমস্যা হলে Exception throw করবে। */
    suspend fun start(onLog: (String) -> Unit)

    /** কানেকশন বন্ধ করে। */
    fun stop()
}
