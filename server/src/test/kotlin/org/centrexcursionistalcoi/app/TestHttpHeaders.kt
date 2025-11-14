package org.centrexcursionistalcoi.app

import io.ktor.http.HttpHeaders
import io.ktor.http.headers
import io.ktor.server.request.ApplicationRequest
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TestHttpHeaders {
    @Test
    fun test_ifModifiedSince() {
        val request = mockk<ApplicationRequest>()
        every { request.headers } returns headers {
            // Source: https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers/If-Modified-Since#examples
            append(HttpHeaders.IfModifiedSince, "Wed, 21 Oct 2015 07:28:00 GMT")
        }
        val ifModifiedSince = request.ifModifiedSince()
        assertNotNull(ifModifiedSince)
        assertEquals(
            ZonedDateTime.of(LocalDate.of(2015, 10, 21), LocalTime.of(7, 28, 0), ZoneOffset.UTC),
            ifModifiedSince
        )
    }
}
