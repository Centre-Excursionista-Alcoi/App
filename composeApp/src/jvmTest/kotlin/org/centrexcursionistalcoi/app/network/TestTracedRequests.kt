package org.centrexcursionistalcoi.app.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.resources.Resources
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.sentry.NoOpTransportFactory
import io.sentry.Sentry
import io.sentry.protocol.SentryTransaction
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import org.centrexcursionistalcoi.app.log.TraceOperation
import org.centrexcursionistalcoi.app.log.traceTransaction
import org.centrexcursionistalcoi.app.routes.Api

class TestTracedRequests {
    private val sent = mutableListOf<SentryTransaction>()

    private val client = HttpClient(MockEngine { respond("[]", HttpStatusCode.OK) }) { install(Resources) }

    @BeforeTest
    fun setUp() {
        Sentry.init { options ->
            options.dsn = "https://key@sentry.invalid/1"
            options.tracesSampleRate = 1.0
            options.setTransportFactory(NoOpTransportFactory.getInstance())
            options.setBeforeSendTransaction { transaction, _ -> synchronized(sent) { sent += transaction }; transaction }
        }
    }

    @AfterTest
    fun tearDown() {
        Sentry.close()
        sent.clear()
        client.close()
    }

    @Test
    fun test_getTraced_recordsRequestSpan() = runTest {
        val body = traceTransaction("test", TraceOperation.SYNC) {
            client.getTraced { url("/departments") }.bodyAsText()
        }

        assertEquals("[]", body)
        val span = sent.single().spans.single()
        assertEquals(TraceOperation.HTTP_CLIENT.value, span.op)
        assertEquals("GET /departments", span.description)
        assertEquals(200L, span.data?.get("http.response.status_code"))
    }

    @Test
    fun test_getTraced_resource_describedByItsPath() = runTest {
        traceTransaction("test", TraceOperation.SYNC) {
            client.getTraced(Api.Profile()) { }
        }

        assertEquals("GET /profile", sent.single().spans.single().description)
    }
}
