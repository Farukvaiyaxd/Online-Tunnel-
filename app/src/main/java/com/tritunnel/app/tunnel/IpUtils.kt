package com.tritunnel.app.tunnel

object IpUtils {
    const val PROTO_TCP = 6
    const val PROTO_UDP = 17

    const val TCP_FIN = 0x01
    const val TCP_SYN = 0x02
    const val TCP_RST = 0x04
    const val TCP_PSH = 0x08
    const val TCP_ACK = 0x10

    fun version(buf: ByteArray) = (buf[0].toInt() and 0xF0) shr 4
    fun ipHeaderLen(buf: ByteArray) = (buf[0].toInt() and 0x0F) * 4
    fun totalLength(buf: ByteArray) = u16(buf, 2)
    fun protocol(buf: ByteArray) = buf[9].toInt() and 0xFF
    fun srcIp(buf: ByteArray): ByteArray = buf.copyOfRange(12, 16)
    fun dstIp(buf: ByteArray): ByteArray = buf.copyOfRange(16, 20)

    fun tcpSrcPort(buf: ByteArray, ihl: Int) = u16(buf, ihl)
    fun tcpDstPort(buf: ByteArray, ihl: Int) = u16(buf, ihl + 2)
    fun tcpSeq(buf: ByteArray, ihl: Int) = u32(buf, ihl + 4)
    fun tcpAck(buf: ByteArray, ihl: Int) = u32(buf, ihl + 8)
    fun tcpDataOffset(buf: ByteArray, ihl: Int) = ((buf[ihl + 12].toInt() and 0xF0) shr 4) * 4
    fun tcpFlags(buf: ByteArray, ihl: Int) = buf[ihl + 13].toInt() and 0xFF

    fun tcpPayload(buf: ByteArray, ihl: Int): ByteArray {
        val thl = tcpDataOffset(buf, ihl)
        val dataStart = ihl + thl
        val dataEnd = totalLength(buf)
        return if (dataEnd > dataStart) buf.copyOfRange(dataStart, dataEnd) else ByteArray(0)
    }

    fun buildIpTcpPacket(
        srcIp: ByteArray, dstIp: ByteArray,
        srcPort: Int, dstPort: Int,
        seq: Long, ack: Long,
        flags: Int,
        windowSize: Int = 0xFFFF,
        payload: ByteArray = ByteArray(0),
    ): ByteArray {
        val ipHLen = 20
        val tcpHLen = 20
        val totalLen = ipHLen + tcpHLen + payload.size
        val pkt = ByteArray(totalLen)

        // IPv4 header
        pkt[0] = 0x45.toByte()   // version=4, IHL=5
        pkt[2] = (totalLen shr 8).toByte()
        pkt[3] = totalLen.toByte()
        pkt[6] = 0x40             // DF flag
        pkt[8] = 64               // TTL
        pkt[9] = PROTO_TCP.toByte()
        srcIp.copyInto(pkt, 12)
        dstIp.copyInto(pkt, 16)
        val ipCsum = internetChecksum(pkt, 0, ipHLen)
        pkt[10] = (ipCsum shr 8).toByte(); pkt[11] = ipCsum.toByte()

        // TCP header
        val t = ipHLen
        pkt[t + 0] = (srcPort shr 8).toByte(); pkt[t + 1] = srcPort.toByte()
        pkt[t + 2] = (dstPort shr 8).toByte(); pkt[t + 3] = dstPort.toByte()
        pkt[t + 4] = (seq shr 24).toByte(); pkt[t + 5] = (seq shr 16).toByte()
        pkt[t + 6] = (seq shr 8).toByte();  pkt[t + 7] = seq.toByte()
        pkt[t + 8] = (ack shr 24).toByte(); pkt[t + 9] = (ack shr 16).toByte()
        pkt[t + 10] = (ack shr 8).toByte(); pkt[t + 11] = ack.toByte()
        pkt[t + 12] = ((tcpHLen / 4) shl 4).toByte()
        pkt[t + 13] = flags.toByte()
        pkt[t + 14] = (windowSize shr 8).toByte(); pkt[t + 15] = windowSize.toByte()
        payload.copyInto(pkt, t + tcpHLen)

        val tcpLen = tcpHLen + payload.size
        val tcpCsum = tcpChecksum(srcIp, dstIp, pkt, t, tcpLen)
        pkt[t + 16] = (tcpCsum shr 8).toByte(); pkt[t + 17] = tcpCsum.toByte()

        return pkt
    }

    fun buildUdpPacket(
        srcIp: ByteArray, dstIp: ByteArray,
        srcPort: Int, dstPort: Int,
        payload: ByteArray,
    ): ByteArray {
        val udpLen = 8 + payload.size
        val totalLen = 20 + udpLen
        val pkt = ByteArray(totalLen)
        pkt[0] = 0x45.toByte(); pkt[6] = 0x40; pkt[8] = 64; pkt[9] = PROTO_UDP.toByte()
        pkt[2] = (totalLen shr 8).toByte(); pkt[3] = totalLen.toByte()
        srcIp.copyInto(pkt, 12); dstIp.copyInto(pkt, 16)
        val ipCsum = internetChecksum(pkt, 0, 20)
        pkt[10] = (ipCsum shr 8).toByte(); pkt[11] = ipCsum.toByte()
        pkt[20] = (srcPort shr 8).toByte(); pkt[21] = srcPort.toByte()
        pkt[22] = (dstPort shr 8).toByte(); pkt[23] = dstPort.toByte()
        pkt[24] = (udpLen shr 8).toByte(); pkt[25] = udpLen.toByte()
        payload.copyInto(pkt, 28)
        return pkt
    }

    private fun tcpChecksum(srcIp: ByteArray, dstIp: ByteArray, buf: ByteArray, tcpOff: Int, tcpLen: Int): Int {
        val pseudo = ByteArray(12 + tcpLen)
        srcIp.copyInto(pseudo, 0); dstIp.copyInto(pseudo, 4)
        pseudo[8] = 0; pseudo[9] = PROTO_TCP.toByte()
        pseudo[10] = (tcpLen shr 8).toByte(); pseudo[11] = tcpLen.toByte()
        buf.copyInto(pseudo, 12, tcpOff, tcpOff + tcpLen)
        pseudo[28] = 0; pseudo[29] = 0  // zero out checksum field within TCP header
        return internetChecksum(pseudo, 0, pseudo.size)
    }

    private fun internetChecksum(buf: ByteArray, off: Int, len: Int): Int {
        var sum = 0L
        var i = off
        while (i < off + len - 1) {
            sum += ((buf[i].toInt() and 0xFF) shl 8) or (buf[i + 1].toInt() and 0xFF)
            i += 2
        }
        if ((len and 1) != 0) sum += (buf[off + len - 1].toInt() and 0xFF) shl 8
        while (sum shr 16 != 0L) sum = (sum and 0xFFFF) + (sum shr 16)
        return (sum.inv() and 0xFFFF).toInt()
    }

    private fun u16(buf: ByteArray, off: Int) =
        ((buf[off].toInt() and 0xFF) shl 8) or (buf[off + 1].toInt() and 0xFF)

    private fun u32(buf: ByteArray, off: Int): Long {
        var v = 0L
        for (i in 0..3) v = (v shl 8) or (buf[off + i].toLong() and 0xFF)
        return v
    }

    fun ip4Int(b: ByteArray) =
        ((b[0].toInt() and 0xFF) shl 24) or ((b[1].toInt() and 0xFF) shl 16) or
        ((b[2].toInt() and 0xFF) shl 8) or (b[3].toInt() and 0xFF)
}
