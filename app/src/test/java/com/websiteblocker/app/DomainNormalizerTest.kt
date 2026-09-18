package com.websiteblocker.app

import com.websiteblocker.app.domain.DomainNormalizer as D
import org.junit.Assert.*
import org.junit.Test

class DomainNormalizerTest {
    @Test
    fun plain() {
        assertEquals("x.com", D.normalize("x.com"))
    }

    @Test
    fun https() {
        assertEquals("x.com", D.normalize("https://x.com"))
    }

    @Test
    fun pathsQueriesFragmentsAndPort() {
        assertEquals("x.com", D.normalize("https://www.x.com:443/home?source=test#part"))
    }

    @Test
    fun uppercase() {
        assertEquals("x.com", D.normalize("HTTPS://X.COM"))
    }

    @Test
    fun leadingWwwOnly() {
        assertEquals("x.com", D.normalize("www.x.com"))
        assertEquals("mobile.www.x.com", D.normalize("mobile.www.x.com"))
    }

    @Test
    fun trailingDot() {
        assertEquals("x.com", D.normalize(" x.com. "))
    }

    @Test
    fun unicode() {
        assertEquals("xn--bcher-kva.de", D.normalize("https://bücher.de/path"))
    }

    @Test
    fun invalidInputs() {
        listOf(
                "",
                " ",
                "hello world",
                "localhost",
                "x..com",
                "-x.com",
                "x_.com",
                "https://",
                "https://user:secret@x.com",
                "https://x.com:bad",
                "x.com:65536",
                "ftp://x.com",
                "127.0.0.1",
                "[::1]",
                "x.com\\evil.com",
                "https://%78.com",
                "a".repeat(64) + ".com",
            )
            .forEach { value ->
                assertThrows(value, IllegalArgumentException::class.java) { D.normalize(value) }
            }
    }

    @Test
    fun subdomainBoundaries() {
        listOf("x.com", "www.x.com", "mobile.x.com", "api.x.com", "API.X.COM.").forEach {
            assertTrue(D.matches(it, "x.com"))
        }
        listOf("notx.com", "examplex.com", "x.com.evil.com").forEach {
            assertFalse(D.matches(it, "x.com"))
        }
    }
}
