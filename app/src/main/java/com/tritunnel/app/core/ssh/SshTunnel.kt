package com.tritunnel.app.core.ssh

import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import com.tritunnel.app.core.Tunnel
import com.tritunnel.app.data.ServerConfig
import java.util.Properties

/**
 * SSH tunnel — JSch দিয়ে রিমোট সার্ভারে কানেক্ট করে এবং
 * dynamic port forwarding (-D) দিয়ে একটা লোকাল SOCKS5 proxy খোলে।
 *
 * এর ফলে 127.0.0.1:<localSocksPort>-এ একটা SOCKS5 proxy পাওয়া যায়,
 * যার সব ট্রাফিক এনক্রিপ্ট হয়ে SSH সার্ভার দিয়ে যায়।
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
        onLog("SSH: লগইন সফল ✓")

        // dynamic SOCKS5 proxy চালু করা
        s.setPortForwardingD(localSocksPort)
        onLog("SOCKS5 proxy চালু: 127.0.0.1:$localSocksPort")

        session = s
    }

    override fun stop() {
        runCatching { session?.disconnect() }
        session = null
    }
}
