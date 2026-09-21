package com.websiteblocker.app

import com.websiteblocker.app.vpn.DnsOverHttps
import com.websiteblocker.app.vpn.DnsPacketHandler as D
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.URL
import java.security.cert.Certificate
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLHandshakeException
import org.junit.Assert.*
import org.junit.Test

class DnsOverHttpsTest {
    private val dns = byteArrayOf(0x12, 0x34, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 120, 3, 99, 111, 109, 0, 0, 1, 0, 1)
    private val query = D.Query(byteArrayOf(), dns, "x.com", dns.size)
    private val valid = D.failure(query, 0)

    private class Reply(url: URL, val body: ByteArray, val status: Int = 200,
        val mime: String = "application/dns-message", val length: Long = body.size.toLong()) : HttpsURLConnection(url) {
        val sent = ByteArrayOutputStream()
        var closed = false
        override fun connect() = Unit
        override fun disconnect() { closed = true }
        override fun usingProxy() = false
        override fun getCipherSuite() = "TLS_AES_128_GCM_SHA256"
        override fun getLocalCertificates(): Array<Certificate>? = null
        override fun getServerCertificates(): Array<Certificate> = emptyArray()
        override fun getOutputStream() = sent
        override fun getInputStream() = ByteArrayInputStream(body)
        override fun getResponseCode() = status
        override fun getContentType() = mime
        override fun getContentLengthLong() = length
    }

    @Test fun sendsDnsInEncryptedPostBodyWithoutQueryUrlOrRedirects() {
        lateinit var reply: Reply
        val answer = DnsOverHttps { url -> Reply(url, valid).also { reply = it } }.resolve(query)
        assertArrayEquals(valid, answer)
        assertEquals("https", reply.url.protocol)
        assertNull(reply.url.query)
        assertEquals("POST", reply.requestMethod)
        assertEquals("application/dns-message", reply.getRequestProperty("Content-Type"))
        assertFalse(reply.instanceFollowRedirects)
        assertArrayEquals(dns, reply.sent.toByteArray())
        assertTrue(reply.closed)
    }

    @Test fun tlsFailureNeverDowngradesToPlaintextDns() {
        val attempted = mutableListOf<String>()
        assertNull(DnsOverHttps {
            attempted += it.toString()
            throw SSLHandshakeException("test certificate failure")
        }.resolve(query))
        assertEquals(DnsOverHttps.ENDPOINTS, attempted)
        assertTrue(attempted.all { it.startsWith("https://") })
    }

    @Test fun rejectsRedirectWrongMimeWrongIdAndOversizedBodies() {
        val badId = valid.clone().apply { this[0] = 0x44 }
        val cases: List<(URL) -> Reply> = listOf(
            { Reply(it, valid, status = 302) },
            { Reply(it, valid, mime = "text/html") },
            { Reply(it, badId) },
            { Reply(it, valid, length = 50_000) },
            { Reply(it, ByteArray(4097), length = -1) },
        )
        cases.forEach { factory ->
            val opened = mutableListOf<Reply>()
            assertNull(DnsOverHttps { factory(it).also(opened::add) }.resolve(query))
            assertEquals(2, opened.size)
            assertTrue(opened.all { it.closed })
        }
    }

    @Test fun validSecondaryEncryptedResponseRecoversPrimaryFailure() {
        var attempts = 0
        assertArrayEquals(valid, DnsOverHttps {
            Reply(it, valid, status = if (++attempts == 1) 503 else 200)
        }.resolve(query))
        assertEquals(2, attempts)
    }
}
