package com.usefocus.app.domain

import java.net.IDN
import java.net.URI
import java.util.Locale

object DomainNormalizer {
    fun normalize(input: String): String {
        val value = input.trim()
        require(
            value.isNotEmpty() &&
                value.none { it.isWhitespace() || it.isISOControl() || it == '\\' }
        ) {
            "Enter a valid website, such as x.com."
        }
        val uri =
            try {
                URI(if (value.contains("://")) value else "https://$value")
            } catch (_: Exception) {
                throw IllegalArgumentException("Enter a valid website URL.")
            }
        require(uri.scheme.lowercase(Locale.ROOT) in listOf("http", "https")) {
            "Use an HTTP or HTTPS website."
        }
        val authority =
            uri.rawAuthority ?: throw IllegalArgumentException("Enter a valid website hostname.")
        require(!authority.contains('@') && !authority.contains('%') && !authority.contains('[')) {
            "Use a hostname without credentials or IP addresses."
        }
        val pieces = authority.split(':')
        require(pieces.size <= 2 && (pieces.size == 1 || pieces[1].toIntOrNull() in 1..65535)) {
            "The website port is invalid."
        }
        var host =
            try {
                IDN.toASCII(pieces[0].trimEnd('.'), IDN.USE_STD3_ASCII_RULES).lowercase(Locale.ROOT)
            } catch (_: Exception) {
                throw IllegalArgumentException("Enter a valid website hostname.")
            }
        if (host.startsWith("www.") && host.removePrefix("www.").contains('.'))
            host = host.removePrefix("www.")
        val labels = host.split('.')
        require(
            host.length <= 253 &&
                labels.size >= 2 &&
                labels.all {
                    it.length in 1..63 && it.matches(Regex("[a-z0-9](?:[a-z0-9-]*[a-z0-9])?"))
                } &&
                labels.last().any { it in 'a'..'z' }
        ) {
            "Enter a valid website hostname, such as x.com."
        }
        return host
    }

    fun matches(requested: String, blocked: String): Boolean {
        val host = requested.trimEnd('.').lowercase(Locale.ROOT)
        return host == blocked || host.endsWith(".$blocked")
    }
}
