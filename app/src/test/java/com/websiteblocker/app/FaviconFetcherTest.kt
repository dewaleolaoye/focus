package com.websiteblocker.app

import com.websiteblocker.app.data.website.FaviconFetcher
import org.junit.Assert.assertEquals
import org.junit.Test

class FaviconFetcherTest {
    @Test
    fun discoversAbsoluteRelativeAndUnquotedIcons() {
        val html = """
            <link rel="apple-touch-icon" href="/touch.png">
            <link href='https://cdn.example.com/icon.png' rel='shortcut icon'>
            <link rel=icon href=small.png>
        """.trimIndent()

        assertEquals(
            listOf(
                "https://example.com/touch.png",
                "https://cdn.example.com/icon.png",
                "https://example.com/path/small.png",
            ),
            FaviconFetcher.iconUrls(html, "https://example.com/path/page").map { it.toString() },
        )
    }

    @Test
    fun ignoresStylesheetsAndInlineDataIcons() {
        val html = """
            <link rel="stylesheet" href="site.css">
            <link rel="icon" href="data:image/png;base64,abc">
        """.trimIndent()

        assertEquals(emptyList<java.net.URL>(), FaviconFetcher.iconUrls(html, "https://example.com"))
    }
}
