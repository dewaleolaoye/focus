package com.websiteblocker.app

import com.websiteblocker.app.vpn.DnsPacketHandler as D
import kotlin.random.Random
import org.junit.Assert.*
import org.junit.Test

class DnsPacketHandlerTest {
    private fun packet(): ByteArray {
        val dns =
            byteArrayOf(
                0x12,
                0x34,
                1,
                0,
                0,
                1,
                0,
                0,
                0,
                0,
                0,
                0,
                1,
                120,
                3,
                99,
                111,
                109,
                0,
                0,
                1,
                0,
                1,
            )
        return ByteArray(28 + dns.size).apply {
            this[0] = 0x45
            this[2] = 0
            this[3] = size.toByte()
            this[8] = 64
            this[9] = 17
            this[12] = 10
            this[13] = 111
            this[14] = 0
            this[15] = 1
            this[16] = 10
            this[17] = 111
            this[18] = 0
            this[19] = 2
            this[20] = 0x12
            this[21] = 0x34
            this[23] = 53
            this[25] = (dns.size + 8).toByte()
            dns.copyInto(this, 28)
        }
    }

    @Test
    fun extractsQuestion() {
        assertEquals("x.com", D.parse(packet())!!.hostname)
    }

    @Test
    fun nxdomainPreservesIdAndQuestion() {
        val q = D.parse(packet())!!
        val response = D.failure(q)
        assertEquals(3, response[3].toInt() and 15)
        assertTrue(D.validResponse(q, response))
        assertEquals(0, response[10].toInt())
        assertEquals(0, response[11].toInt())
    }

    @Test
    fun wrapsAddressesPortsAndChecksum() {
        val q = D.parse(packet())!!
        val response = D.wrap(q, D.failure(q))
        assertArrayEquals(byteArrayOf(10, 111, 0, 2), response.copyOfRange(12, 16))
        assertArrayEquals(byteArrayOf(10, 111, 0, 1), response.copyOfRange(16, 20))
        assertEquals(0x12, response[22].toInt())
        assertEquals(0x34, response[23].toInt())
        var sum = 0
        for (i in 0 until 20 step 2) sum +=
            ((response[i].toInt() and 255) shl 8) + (response[i + 1].toInt() and 255)
        while (sum ushr 16 != 0) sum = (sum and 65535) + (sum ushr 16)
        assertEquals(65535, sum)
    }

    @Test
    fun rejectsFragmentsWrongPortAndPointers() {
        assertNull(D.parse(packet().apply { this[6] = 0x20 }))
        assertNull(D.parse(packet().apply { this[23] = 80 }))
        assertNull(
            D.parse(
                packet().apply {
                    this[40] = 0xc0.toByte()
                    this[41] = 12
                }
            )
        )
    }

    @Test
    fun rejectsMismatchedResponse() {
        val q = D.parse(packet())!!
        assertFalse(D.validResponse(q, D.failure(q).apply { this[0] = 0 }))
    }

    @Test
    fun allTruncationsAndRandomPacketsAreSafe() {
        val packet = packet()
        for (n in 0 until packet.size) assertNull(D.parse(packet.copyOf(n)))
        val random = Random(42)
        repeat(10000) { D.parse(random.nextBytes(random.nextInt(0, 1000))) }
    }
}
