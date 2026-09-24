package org.centrexcursionistalcoi.app.utils

import kotlin.test.Test
import kotlin.test.assertEquals

class TestStringUtils {

    @Test
    fun `escapeHtml replaces every HTML-special character`() {
        assertEquals("&amp;", "&".escapeHtml())
        assertEquals("&lt;", "<".escapeHtml())
        assertEquals("&gt;", ">".escapeHtml())
        assertEquals("&quot;", "\"".escapeHtml())
        assertEquals("&#39;", "'".escapeHtml())
    }

    @Test
    fun `escapeHtml escapes ampersands first so entities are not double-escaped`() {
        assertEquals("&amp;lt;", "&lt;".escapeHtml())
    }

    @Test
    fun `escapeHtml leaves plain text untouched`() {
        assertEquals("Lending", "Lending".escapeHtml())
    }

    @Test
    fun `escapeHtml neutralizes an attribute breakout attempt`() {
        val malicious = """"><script>alert(1)</script>"""
        val escaped = malicious.escapeHtml()

        assertEquals("&quot;&gt;&lt;script&gt;alert(1)&lt;/script&gt;", escaped)
    }
}
