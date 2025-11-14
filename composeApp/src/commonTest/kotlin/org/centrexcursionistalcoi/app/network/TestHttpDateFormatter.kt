package org.centrexcursionistalcoi.app.network

import kotlin.test.Test
import kotlin.test.assertEquals

class TestHttpDateFormatter {
    @Test
    fun test_format_rfc1123() {
        val formatted = HttpDateFormatter.format(1445299200000, HttpDateFormatter.rfc1123)
        assertEquals(
            "Tue, 20 Oct 2015 00:00:00 GMT",
            formatted
        )
    }
}
