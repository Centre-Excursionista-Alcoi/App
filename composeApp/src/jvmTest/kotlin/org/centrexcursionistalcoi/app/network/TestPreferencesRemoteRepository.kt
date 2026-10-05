package org.centrexcursionistalcoi.app.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.resources.Resources
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import java.io.IOException
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class TestPreferencesRemoteRepository {
    private val requests = mutableListOf<HttpRequestData>()
    private var bodies = mutableListOf<String>()

    private fun use(handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData): PreferencesRemoteRepository {
        _httpClient = HttpClient(MockEngine { request ->
            requests += request
            bodies += request.body.toByteArray().decodeToString()
            handler(request)
        }) { install(Resources) }
        return PreferencesRemoteRepository()
    }

    @AfterTest
    fun tearDown() {
        _httpClient?.close()
        _httpClient = null
    }

    @Test
    fun `the language of the server is fetched`() = runTest {
        val repository = use { respond("""{"language":"ca-ES"}""", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json")) }

        assertEquals("ca-ES", repository.getLanguage())
        assertEquals("/profile/preferences", requests.single().url.encodedPath)
        assertEquals(HttpMethod.Get, requests.single().method)
    }

    @Test
    fun `the server not having a language is not a language`() = runTest {
        val repository = use { respond("{}", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json")) }
        assertNull(repository.getLanguage())
    }

    @Test
    fun `a request that fails is silent`() = runTest {
        for (status in listOf(HttpStatusCode.Unauthorized, HttpStatusCode.InternalServerError, HttpStatusCode.NotFound)) {
            assertNull(use { respondError(status) }.getLanguage(), "$status")
            assertFalse(use { respondError(status) }.setLanguage("ca"), "$status")
        }
    }

    @Test
    fun `no connection is silent`() = runTest {
        assertNull(use { throw IOException("No connection") }.getLanguage())
        assertFalse(use { throw IOException("No connection") }.setLanguage("ca"))
    }

    @Test
    fun `a body that is not understood is silent`() = runTest {
        assertNull(use { respond("<html>", HttpStatusCode.OK) }.getLanguage())
    }

    @Test
    fun `the language is sent`() = runTest {
        val repository = use { respond("", HttpStatusCode.NoContent) }

        assertTrue(repository.setLanguage("ca"))
        assertEquals(HttpMethod.Patch, requests.single().method)
        assertEquals("/profile/preferences", requests.single().url.encodedPath)
        assertEquals("""{"language":"ca"}""", bodies.single())
    }
}
