package org.centrexcursionistalcoi.app.tracing

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.writeFully
import io.ktor.utils.io.writer
import io.sentry.NoOpTransportFactory
import io.sentry.Sentry
import io.sentry.SpanStatus
import io.sentry.protocol.SentrySpan
import io.sentry.protocol.SentryTransaction
import java.io.IOException
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.table.Files
import org.jetbrains.exposed.v1.jdbc.selectAll

class TestServerTracing {
    private val sent = mutableListOf<SentryTransaction>()

    @BeforeTest
    fun setUp() {
        Database.initForTests()
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
        Database.clear()
    }

    private fun client(engine: MockEngine) = HttpClient(engine) {
        install(SentryHttpClientTracing)
        defaultRequest { url("https://example.com") }
    }

    @Test
    fun test_request_recordedAsSpan_withoutQuery() = runTest {
        client(MockEngine { respond("ok", HttpStatusCode.OK) }).use { client ->
            traceTransaction("Task", TraceOperation.TASK) {
                // On another thread, like the real integrations' engines
                withContext(Dispatchers.IO) { client.get("/print/licence.php?id=secret") }
            }
        }

        val span = sent.single().spans.single { it.op == TraceOperation.HTTP_CLIENT.value }
        assertEquals("GET /print/licence.php", span.description)
        assertEquals("example.com", span.data?.get("server.address"))
        assertEquals(200, span.data?.get("http.response.status_code"))
        assertEquals(SpanStatus.OK, span.status)
    }

    @Test
    fun test_bodyDownload_includedAndMeasured() = runBlocking {
        // runBlocking, not runTest: the delay below must really happen, since spans are timed with the real clock
        val engine = MockEngine {
            // A body that takes a while to arrive, after the headers
            val body = CoroutineScope(Dispatchers.IO).writer {
                delay(50)
                channel.writeFully("slow body".encodeToByteArray())
            }.channel
            respond(body, HttpStatusCode.OK)
        }
        val text = client(engine).use { client ->
            traceTransaction("Task", TraceOperation.TASK) {
                withContext(Dispatchers.IO) { client.get("/export").bodyAsText() }
            }
        }

        assertEquals("slow body", text)
        val spans = sent.single().spans
        val request = spans.single { it.op == TraceOperation.HTTP_CLIENT.value }
        val body = spans.single { it.op == TraceOperation.HTTP_CLIENT_BODY.value }
        assertEquals(request.spanId, body.parentSpanId)
        // Some margin: the body starts arriving (and so the delay) slightly before the span starts
        assertTrue(body.durationMillis() >= 25, "Body took ${body.durationMillis()} ms")
        assertTrue(request.timestamp!! >= body.timestamp!!)
    }

    private fun SentrySpan.durationMillis() = ((timestamp!! - startTimestamp) * 1000).toLong()

    @Test
    fun test_failedRequest_finishedAsError() = runTest {
        client(MockEngine { throw IOException("unreachable") }).use { client ->
            assertFailsWith<IOException> {
                traceTransaction("Task", TraceOperation.TASK) { client.get("/login") }
            }
        }

        val transaction = sent.single()
        assertEquals(SpanStatus.INTERNAL_ERROR, transaction.status)
        assertEquals(SpanStatus.INTERNAL_ERROR, transaction.spans.single { it.op == TraceOperation.HTTP_CLIENT.value }.status)
    }

    @Test
    fun test_requestOutsideTransaction_notRecorded() = runTest {
        client(MockEngine { respond("ok", HttpStatusCode.OK) }).use { client ->
            assertEquals(HttpStatusCode.OK, client.get("/login").status)
        }

        assertEquals(emptyList(), sent)
    }

    @Test
    fun test_transaction_recordsSqlStatements() = runTest {
        traceTransaction("Task", TraceOperation.TASK) {
            withContext(Dispatchers.IO) { Database { Files.selectAll().count() } }
        }

        val transaction = sent.single()
        assertEquals("Task", transaction.transaction)
        assertTrue(transaction.spans.any { it.op == TraceOperation.DB_SQL_QUERY.value })
    }
}
