package com.tritunnel.app.tunnel

import com.tritunnel.app.data.VpnConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap

class TcpTunnel(
    fd: FileDescriptor,
    private val config: VpnConfig,
    private val protectTcp: (java.net.Socket) -> Boolean,
    private val protectUdp: (DatagramSocket) -> Boolean,
    private val scope: CoroutineScope,
) {
    private val tunIn = FileInputStream(fd)
    private val tunOut = FileOutputStream(fd)
    private val writeLock = Any()

    data class ConnId(val srcIp: Int, val srcPort: Int, val dstIp: Int, val dstPort: Int)

    inner class TcpConn(
        val id: ConnId,
        val srcIpB: ByteArray,
        val dstIpB: ByteArray,
        initClientSeq: Long,
        initServerSeq: Long,
    ) {
        // next seq we expect from client (after SYN consumes 1)
        @Volatile var clientNextSeq: Long = (initClientSeq + 1) and 0xFFFFFFFFL
        // next seq we send to client (after our SYN-ACK consumes 1)
        @Volatile var serverNextSeq: Long = (initServerSeq + 1) and 0xFFFFFFFFL

        @Volatile var vless: VlessClient? = null
        @Volatile var ready: Boolean = false

        val pendingLock = Any()
        val pending = mutableListOf<ByteArray>()
    }

    private val conns = ConcurrentHashMap<ConnId, TcpConn>()

    // ── Start / Stop ─────────────────────────────────────────────────────────

    fun start() {
        scope.launch(Dispatchers.IO) {
            val buf = ByteArray(32767)
            while (isActive) {
                val n = runCatching { tunIn.read(buf) }.getOrDefault(-1)
                if (n <= 0) { delay(1); continue }
                handlePacket(buf.copyOf(n))
            }
        }
    }

    fun stop() {
        conns.values.forEach { it.vless?.close() }
        conns.clear()
        runCatching { tunIn.close() }
        runCatching { tunOut.close() }
    }

    // ── Packet dispatcher ─────────────────────────────────────────────────────

    private fun handlePacket(pkt: ByteArray) {
        if (pkt.size < 20 || IpUtils.version(pkt) != 4) return
        val ihl = IpUtils.ipHeaderLen(pkt)
        if (pkt.size < ihl + 8) return
        when (IpUtils.protocol(pkt)) {
            IpUtils.PROTO_TCP -> if (pkt.size >= ihl + 20) handleTcp(pkt, ihl)
            IpUtils.PROTO_UDP -> handleUdp(pkt, ihl)
        }
    }

    // ── TCP ───────────────────────────────────────────────────────────────────

    private fun handleTcp(pkt: ByteArray, ihl: Int) {
        val srcIpB  = IpUtils.srcIp(pkt)
        val dstIpB  = IpUtils.dstIp(pkt)
        val srcPort = IpUtils.tcpSrcPort(pkt, ihl)
        val dstPort = IpUtils.tcpDstPort(pkt, ihl)
        val seq     = IpUtils.tcpSeq(pkt, ihl)
        val flags   = IpUtils.tcpFlags(pkt, ihl)
        val payload = IpUtils.tcpPayload(pkt, ihl)
        val id = ConnId(IpUtils.ip4Int(srcIpB), srcPort, IpUtils.ip4Int(dstIpB), dstPort)

        when {
            flags and IpUtils.TCP_RST != 0 -> {
                conns.remove(id)?.vless?.close()
            }
            flags and IpUtils.TCP_SYN != 0 && flags and IpUtils.TCP_ACK == 0 -> {
                onSyn(id, srcIpB, dstIpB, srcPort, dstPort, seq)
            }
            flags and IpUtils.TCP_FIN != 0 -> {
                onFin(id, seq)
            }
            flags and IpUtils.TCP_ACK != 0 && payload.isNotEmpty() -> {
                onData(id, seq, payload)
            }
        }
    }

    private fun onSyn(
        id: ConnId,
        srcIpB: ByteArray, dstIpB: ByteArray,
        srcPort: Int, dstPort: Int,
        clientSeq: Long,
    ) {
        // Handle SYN retransmission: just resend SYN-ACK
        conns[id]?.let { existing ->
            sendToTun(IpUtils.buildIpTcpPacket(
                srcIp = dstIpB, dstIp = srcIpB,
                srcPort = dstPort, dstPort = srcPort,
                seq = (existing.serverNextSeq - 1) and 0xFFFFFFFFL,
                ack = existing.clientNextSeq,
                flags = IpUtils.TCP_SYN or IpUtils.TCP_ACK,
            ))
            return
        }

        val serverInitSeq = (System.nanoTime() / 1000L) and 0xFFFFFFFFL
        val conn = TcpConn(id, srcIpB, dstIpB, clientSeq, serverInitSeq)
        conns[id] = conn

        // Immediately reply with SYN-ACK so the client doesn't timeout
        sendToTun(IpUtils.buildIpTcpPacket(
            srcIp = dstIpB, dstIp = srcIpB,
            srcPort = dstPort, dstPort = srcPort,
            seq = serverInitSeq,           // initial server seq (SYN consumes it → next is +1)
            ack = conn.clientNextSeq,      // = clientSeq + 1
            flags = IpUtils.TCP_SYN or IpUtils.TCP_ACK,
        ))

        // Establish VLESS connection in background
        scope.launch(Dispatchers.IO) {
            val vless = VlessClient(config.host, config.port, config.uuid, config.sni)
            val ok = vless.connect(dstIpB, dstPort, protectTcp)

            if (!ok || !isActive || !conns.containsKey(id)) {
                // Connection to proxy failed → send RST to client
                sendToTun(IpUtils.buildIpTcpPacket(
                    srcIp = dstIpB, dstIp = srcIpB,
                    srcPort = dstPort, dstPort = srcPort,
                    seq = conn.serverNextSeq, ack = conn.clientNextSeq,
                    flags = IpUtils.TCP_RST or IpUtils.TCP_ACK,
                ))
                conns.remove(id)
                return@launch
            }

            conn.vless = vless
            conn.ready = true

            // Flush any data the client sent before VLESS was ready
            val buffered = synchronized(conn.pendingLock) {
                conn.pending.toList().also { conn.pending.clear() }
            }
            val out = vless.output
            if (out != null) {
                buffered.forEach { d -> runCatching { out.write(d) } }
                runCatching { out.flush() }
            }

            // Read loop: VLESS server → TUN client
            val inp = vless.input ?: return@launch
            val rbuf = ByteArray(8192)
            try {
                while (isActive && conns.containsKey(id)) {
                    val n = inp.read(rbuf)
                    if (n <= 0) break
                    val chunk = rbuf.copyOf(n)
                    sendToTun(IpUtils.buildIpTcpPacket(
                        srcIp = dstIpB, dstIp = srcIpB,
                        srcPort = dstPort, dstPort = srcPort,
                        seq = conn.serverNextSeq,
                        ack = conn.clientNextSeq,
                        flags = IpUtils.TCP_PSH or IpUtils.TCP_ACK,
                        payload = chunk,
                    ))
                    conn.serverNextSeq = (conn.serverNextSeq + n) and 0xFFFFFFFFL
                }
            } finally {
                // Remote closed → send FIN to client
                if (conns.remove(id) != null) {
                    sendToTun(IpUtils.buildIpTcpPacket(
                        srcIp = dstIpB, dstIp = srcIpB,
                        srcPort = dstPort, dstPort = srcPort,
                        seq = conn.serverNextSeq, ack = conn.clientNextSeq,
                        flags = IpUtils.TCP_FIN or IpUtils.TCP_ACK,
                    ))
                }
                vless.close()
            }
        }
    }

    private fun onData(id: ConnId, seq: Long, payload: ByteArray) {
        val conn = conns[id] ?: return
        conn.clientNextSeq = (seq + payload.size) and 0xFFFFFFFFL

        // ACK immediately
        sendToTun(IpUtils.buildIpTcpPacket(
            srcIp = conn.dstIpB, dstIp = conn.srcIpB,
            srcPort = id.dstPort, dstPort = id.srcPort,
            seq = conn.serverNextSeq, ack = conn.clientNextSeq,
            flags = IpUtils.TCP_ACK,
        ))

        if (!conn.ready) {
            synchronized(conn.pendingLock) { conn.pending.add(payload) }
        } else {
            scope.launch(Dispatchers.IO) {
                runCatching {
                    conn.vless?.output?.apply { write(payload); flush() }
                }
            }
        }
    }

    private fun onFin(id: ConnId, seq: Long) {
        val conn = conns.remove(id) ?: return
        conn.clientNextSeq = (seq + 1) and 0xFFFFFFFFL
        // ACK + FIN
        sendToTun(IpUtils.buildIpTcpPacket(
            srcIp = conn.dstIpB, dstIp = conn.srcIpB,
            srcPort = id.dstPort, dstPort = id.srcPort,
            seq = conn.serverNextSeq, ack = conn.clientNextSeq,
            flags = IpUtils.TCP_FIN or IpUtils.TCP_ACK,
        ))
        conn.vless?.close()
    }

    // ── UDP DNS ───────────────────────────────────────────────────────────────

    private fun handleUdp(pkt: ByteArray, ihl: Int) {
        if (pkt.size < ihl + 8) return
        val dstPort = IpUtils.tcpDstPort(pkt, ihl)  // same offset as TCP port
        if (dstPort != 53) return

        val srcIpB  = IpUtils.srcIp(pkt)
        val srcPort = IpUtils.tcpSrcPort(pkt, ihl)
        val udpLen  = ((pkt[ihl + 4].toInt() and 0xFF) shl 8) or (pkt[ihl + 5].toInt() and 0xFF)
        val dnsData = pkt.copyOfRange(ihl + 8, ihl + udpLen)

        scope.launch(Dispatchers.IO) {
            val sock = DatagramSocket()
            protectUdp(sock)
            try {
                sock.soTimeout = 4_000
                sock.send(DatagramPacket(dnsData, dnsData.size, InetAddress.getByName("1.1.1.1"), 53))
                val resp = ByteArray(512)
                val dp = DatagramPacket(resp, resp.size)
                sock.receive(dp)
                sendToTun(IpUtils.buildUdpPacket(
                    srcIp  = InetAddress.getByName("1.1.1.1").address,
                    dstIp  = srcIpB,
                    srcPort = 53, dstPort = srcPort,
                    payload = resp.copyOf(dp.length),
                ))
            } catch (_: Exception) {
            } finally {
                runCatching { sock.close() }
            }
        }
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private fun sendToTun(data: ByteArray) {
        synchronized(writeLock) {
            runCatching { tunOut.write(data) }
        }
    }
}
