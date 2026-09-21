package com.websiteblocker.app.vpn

import java.net.URL
import java.net.URLConnection
import javax.net.ssl.HttpsURLConnection

/** DNS leaves the device only inside authenticated HTTPS. No redirects or plaintext fallback. */
internal class DnsOverHttps(private val open: (URL) -> URLConnection) {
    fun resolve(query: DnsPacketHandler.Query): ByteArray? {
        for (endpoint in ENDPOINTS) {
            var connection: HttpsURLConnection? = null
            try {
                connection = open(URL(endpoint)) as? HttpsURLConnection ?: continue
                connection.apply {
                    connectTimeout = 2_500
                    readTimeout = 2_500
                    instanceFollowRedirects = false
                    useCaches = false
                    requestMethod = "POST"
                    doOutput = true
                    setRequestProperty("Content-Type", MIME)
                    setRequestProperty("Accept", MIME)
                    setFixedLengthStreamingMode(query.dns.size)
                }
                connection.outputStream.use { it.write(query.dns) }
                if (connection.responseCode != 200) continue
                if (connection.contentType?.substringBefore(';')?.trim() != MIME) continue
                if (connection.contentLengthLong > MAX_RESPONSE) continue
                val answer = connection.inputStream.use { input ->
                    val buffer = java.io.ByteArrayOutputStream()
                    val chunk = ByteArray(1024)
                    while (true) {
                        val count = input.read(chunk)
                        if (count == -1) break
                        if (buffer.size() + count > MAX_RESPONSE) return@use null
                        buffer.write(chunk, 0, count)
                    }
                    buffer.toByteArray()
                } ?: continue
                if (DnsPacketHandler.validResponse(query, answer)) return answer
            } catch (_: Exception) {
                // Do not log domains, DNS payloads, or URLs containing user data.
            } finally {
                connection?.disconnect()
            }
        }
        return null
    }

    companion object {
        // IP-literal HTTPS avoids leaking the resolver bootstrap name through unencrypted DNS.
        // Cloudflare's certificate covers these IPs. Default certificate/hostname checks stay on.
        internal val ENDPOINTS = listOf("https://1.1.1.1/dns-query", "https://1.0.0.1/dns-query")
        private const val MIME = "application/dns-message"
        private const val MAX_RESPONSE = 4096
    }
}
