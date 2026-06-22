package com.tritunnel.app.tunnel

import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.util.UUID
import javax.net.ssl.SSLSocketFactory

class VlessClient(
    private val host: String,
    private val port: Int,
    private val uuidStr: String,
    private val sni: String,
) {
    var input: InputStream? = null
    var output: OutputStream? = null
    private var socket: javax.net.ssl.SSLSocket? = null

    fun connect(
        dstIp: ByteArray,   // 4-byte IPv4
        dstPort: Int,
        protect: (java.net.Socket) -> Boolean,
    ): Boolean = try {
        val rawSocket = java.net.Socket()
        protect(rawSocket)
        rawSocket.connect(InetSocketAddress(host, port), 6_000)
        rawSocket.soTimeout = 0

        val factory = SSLSocketFactory.getDefault() as SSLSocketFactory
        val ssl = factory.createSocket(rawSocket, sni.ifBlank { host }, port, true)
                as javax.net.ssl.SSLSocket
        ssl.startHandshake()

        val out = ssl.outputStream

        // VLESS v0 request header
        out.write(0x00)                  // version
        out.write(uuidToBytes(uuidStr))  // UUID (16 bytes)
        out.write(0x00)                  // addons length (none)
        out.write(0x01)                  // command: TCP stream
        out.write((dstPort shr 8) and 0xFF)
        out.write(dstPort and 0xFF)
        out.write(0x01)                  // address type: IPv4
        out.write(dstIp)                 // 4 bytes
        out.flush()

        // Read server response: version(1) + addonLen(1) + [addons]
        val ins = ssl.inputStream
        ins.read()  // skip version byte
        val addonLen = ins.read()
        if (addonLen > 0) ins.skip(addonLen.toLong())

        input = ins
        output = out
        socket = ssl
        true
    } catch (_: Exception) {
        false
    }

    fun close() = runCatching { socket?.close() }

    private fun uuidToBytes(s: String): ByteArray {
        val uuid = UUID.fromString(s)
        val bb = ByteBuffer.wrap(ByteArray(16))
        bb.putLong(uuid.mostSignificantBits)
        bb.putLong(uuid.leastSignificantBits)
        return bb.array()
    }
}
