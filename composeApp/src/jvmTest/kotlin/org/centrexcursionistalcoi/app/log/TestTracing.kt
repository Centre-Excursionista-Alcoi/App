package org.centrexcursionistalcoi.app.log

import io.sentry.NoOpTransportFactory
import io.sentry.Sentry
import io.sentry.SpanStatus
import io.sentry.protocol.SentryTransaction
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext

class TestTracing {
    private val sent = mutableListOf<SentryTransaction>()

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
    }

    @Test
    fun test_traceSpan_nestsUnderTransaction_acrossDispatchers() = runTest {
        traceTransaction("Sync all data", TraceOperation.SYNC) { transaction ->
            transaction.setTag("sync.reason", "initial")
            traceSpan(TraceOperation.SYNC_ENTITY, "departments") { entity ->
                entity?.setData("sync.inserted", 3L)
                withContext(Dispatchers.Default) {
                    traceSpan(TraceOperation.DB_WRITE, "Store departments") { }
                }
            }
        }

        val transaction = sent.single()
        assertEquals("Sync all data", transaction.transaction)
        assertEquals("initial", transaction.tags?.get("sync.reason"))
        val entity = transaction.spans.single { it.op == "sync.entity" }
        assertEquals("departments", entity.description)
        assertEquals(3L, entity.data?.get("sync.inserted"))
        assertEquals(SpanStatus.OK, entity.status)
        val write = transaction.spans.single { it.op == "db.write" }
        assertEquals(entity.spanId, write.parentSpanId)
    }

    @Test
    fun test_traceSpan_failure_finishesWithError() = runTest {
        assertFailsWith<IllegalStateException> {
            traceTransaction("Sync all data", TraceOperation.SYNC) {
                traceSpan(TraceOperation.SYNC_ENTITY, "users") { error("boom") }
            }
        }

        val transaction = sent.single()
        assertEquals(SpanStatus.INTERNAL_ERROR, transaction.status)
        assertEquals(SpanStatus.INTERNAL_ERROR, transaction.spans.single().status)
    }

    @Test
    fun test_traceSpan_cancelled_finishesAsCancelled() = runTest {
        assertFailsWith<CancellationException> {
            traceTransaction("Sync all data", TraceOperation.SYNC) {
                traceSpan(TraceOperation.SYNC_ENTITY, "users") { throw CancellationException("stopped") }
            }
        }

        assertEquals(SpanStatus.CANCELLED, sent.single().spans.single().status)
    }

    @Test
    fun test_traceSpan_outsideTransaction_runsWithoutSpan() = runTest {
        val result = traceSpan(TraceOperation.SYNC_ENTITY, "users") { span ->
            assertNull(span)
            42
        }

        assertEquals(42, result)
        assertEquals(emptyList(), sent)
    }
}
