package com.tritunnel.app.core.ssh

import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import com.tritunnel.app.core.Tunnel
import com.tritunnel.app.data.ServerConfig
import java.util.Properties

/**
 * SSH tunnel — JSch দিয়ে রিমোট সার্ভারে কানেক্ট করে ও লগইন যাচাই করে।
 *
 * Milestone 1: কানেকশন ও authentication কাজ করে নিশ্চিত করা হয়।
 * পরের ধাপে এর উপর SOCKS5 proxy + VpnService রাউটিং যোগ হবে
 * (এর জন্য dynamic forwarding সাপোর্ট করে এমন SSH লাইব্রেরি ব্যবহার করা হবে)।
 */
class SshTunnel(
    private val config: ServerConfig,
    private val localSocksPort: Int = 10808,
) : Tunnel {

    private var session: Session? = null

    override suspend fun start(onLog: (String) -> Unit) {
        onLog("SSH: ${config.host}:${config.port} -এ কানেক্ট হচ্ছে...")
        val jsch = JSch()
        val s = jsch.getSession(config.username, config.host, config.port)
        s.setPassword(config.password)

        // টেস্টের সুবিধার জন্য host key যাচাই বন্ধ (পরে known_hosts যোগ করা ভালো)
        val props = Properties()
        props["StrictHostKeyChecking"] = "no"
        props["PreferredAuthentications"] = "password,keyboard-interactive"
        s.setConfig(props)

        s.connect(20_000)
        onLog("SSH: লগইন সফল ✓ (সার্ভারের সাথে সংযোগ তৈরি হয়েছে)")
        onLog("নোট: পুরো-ফোন রাউটিং পরের ধাপে যোগ হবে।")

        session = s
    }

    override fun stop() {
        runCatching { session?.disconnect() }
        session = null
    }
}
