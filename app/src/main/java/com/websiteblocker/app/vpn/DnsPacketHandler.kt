package com.websiteblocker.app.vpn

import java.util.Locale

/** Pure packet codec. Only unfragmented IPv4 UDP DNS to our virtual endpoint is routed. */
object DnsPacketHandler {
    data class Query(
        val packet: ByteArray,
        val dns: ByteArray,
        val hostname: String,
        val questionEnd: Int,
    )

    private fun u16(a: ByteArray, p: Int) =
        ((a[p].toInt() and 255) shl 8) or (a[p + 1].toInt() and 255)

    private fun put16(a: ByteArray, p: Int, n: Int) {
        a[p] = (n ushr 8).toByte()
        a[p + 1] = n.toByte()
    }

    fun parse(packet: ByteArray): Query? {
        if (packet.size < 40 || (packet[0].toInt() and 240) != 64) return null
        val ihl = (packet[0].toInt() and 15) * 4
        if (
            ihl < 20 ||
                packet.size < ihl + 20 ||
                u16(packet, 2) != packet.size ||
                packet[9].toInt() != 17 ||
                u16(packet, 6) and 0x3fff != 0
        )
            return null
        if (
            !packet.copyOfRange(16, 20).contentEquals(byteArrayOf(10, 111, 0, 2)) ||
                u16(packet, ihl + 2) != 53
        )
            return null
        val length = u16(packet, ihl + 4)
        if (length < 20 || ihl + length != packet.size) return null
        val dns = packet.copyOfRange(ihl + 8, packet.size)
        val question = question(dns) ?: return null
        if (u16(dns, 2) and 0xf800 != 0 || u16(dns, 6) != 0 || u16(dns, 8) != 0) return null
        return Query(packet, dns, question.first, question.second)
    }

    /** Reject query name pointers and non-host labels; never follow attacker-controlled offsets. */
    private fun question(dns: ByteArray): Pair<String, Int>? {
        if (dns.size < 17 || u16(dns, 4) != 1) return null
        var p = 12
        val labels = mutableListOf<String>()
        while (p < dns.size) {
            val size = dns[p++].toInt() and 255
            if (size == 0) break
            if (size > 63 || p + size >= dns.size) return null
            val label = dns.copyOfRange(p, p + size)
            if (
                label.any {
                    val n = it.toInt() and 255
                    n !in 33..126 || n == 46
                }
            )
                return null
            labels += String(label, Charsets.US_ASCII)
            p += size
            if (p - 12 > 254) return null
        }
        if (labels.isEmpty() || p + 4 > dns.size || u16(dns, p + 2) != 1) return null
        return labels.joinToString(".").lowercase(Locale.ROOT) to (p + 4)
    }

    fun failure(query: Query, rcode: Int = 3): ByteArray {
        val dns = query.dns.copyOf(query.questionEnd)
        put16(dns, 2, 0x8080 or (u16(query.dns, 2) and 0x0100) or rcode)
        put16(dns, 6, 0)
        put16(dns, 8, 0)
        put16(dns, 10, 0)
        return dns
    }

    fun validResponse(query: Query, response: ByteArray): Boolean {
        if (
            response.size < 12 ||
                response.size > 4096 ||
                u16(response, 0) != u16(query.dns, 0) ||
                u16(response, 2) and 0x8000 == 0
        )
            return false
        val q = question(response) ?: return false
        return q.first == query.hostname &&
            response
                .copyOfRange(12, q.second)
                .contentEquals(query.dns.copyOfRange(12, query.questionEnd))
    }

    fun wrap(query: Query, dns: ByteArray): ByteArray {
        require(dns.size <= 4096)
        val ihl = (query.packet[0].toInt() and 15) * 4
        val result = ByteArray(28 + dns.size)
        result[0] = 0x45
        put16(result, 2, result.size)
        result[8] = 64
        result[9] = 17
        query.packet.copyInto(result, 12, 16, 20)
        query.packet.copyInto(result, 16, 12, 16)
        put16(result, 20, 53)
        put16(result, 22, u16(query.packet, ihl))
        put16(result, 24, 8 + dns.size)
        // UDP checksum zero is valid for IPv4. IPv4 header checksum is mandatory.
        var sum = 0
        for (i in 0 until 20 step 2) sum += u16(result, i)
        while (sum ushr 16 != 0) sum = (sum and 65535) + (sum ushr 16)
        put16(result, 10, sum.inv() and 65535)
        dns.copyInto(result, 28)
        return result
    }
}
