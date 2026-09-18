package com.websiteblocker.app.data.website

import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

internal object FaviconFetcher {
    private const val MAX_HTML_BYTES = 256 * 1024
    private const val MAX_ICON_BYTES = 2 * 1024 * 1024
    private val linkTag = Regex("<link\\b[^>]*>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val attribute =
        Regex(
            "([a-zA-Z_:][-a-zA-Z0-9_:.]*)\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\\s>]+))",
            RegexOption.DOT_MATCHES_ALL,
        )

    fun fetch(domain: String, isUsable: (ByteArray) -> Boolean): ByteArray? {
        val page = URL("https://$domain/")
        val html = download(page, MAX_HTML_BYTES, "text/html,application/xhtml+xml")
            ?.toString(Charsets.UTF_8)
        val candidates = buildList {
            if (html != null) addAll(iconUrls(html, page.toString()))
            add(URL(page, "/apple-touch-icon.png"))
            add(URL(page, "/favicon.ico"))
        }.distinctBy(URL::toString)

        return candidates.asSequence()
            .filter { it.protocol == "https" || it.protocol == "http" }
            .take(8)
            .mapNotNull { download(it, MAX_ICON_BYTES, "image/*") }
            .filter(isUsable)
            .firstOrNull()
    }

    internal fun iconUrls(html: String, pageUrl: String): List<URL> =
        linkTag.findAll(html).mapNotNull { match ->
            val attributes =
                attribute.findAll(match.value).associate {
                    it.groupValues[1].lowercase() to
                        it.groupValues.drop(2).firstOrNull { value -> value.isNotEmpty() }.orEmpty().trim()
                }
            val rel = attributes["rel"]?.lowercase().orEmpty()
            val href = attributes["href"].orEmpty()
            if (!rel.contains("icon") || href.isBlank() || href.startsWith("data:")) return@mapNotNull null
            runCatching { URI(pageUrl).resolve(href).toURL() }.getOrNull()
        }.toList()

    private fun download(url: URL, limit: Int, accept: String): ByteArray? {
        val connection = (url.openConnection() as? HttpURLConnection) ?: return null
        return try {
            connection.instanceFollowRedirects = true
            connection.connectTimeout = 3_000
            connection.readTimeout = 4_000
            connection.setRequestProperty("Accept", accept)
            connection.setRequestProperty("User-Agent", "Focus/1.2 Android favicon fetcher")
            if (connection.responseCode !in 200..299) return null
            if (connection.contentLengthLong > limit) return null
            connection.inputStream.use { input ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8 * 1024)
                var total = 0
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    if (total > limit) return null
                    output.write(buffer, 0, count)
                }
                output.toByteArray().takeIf { it.isNotEmpty() }
            }
        } catch (_: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }
}
