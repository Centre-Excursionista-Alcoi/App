package org.centrexcursionistalcoi.app.database

import io.sentry.ITransaction
import io.sentry.NoOpTransportFactory
import io.sentry.Sentry
import io.sentry.SentryTracer
import io.sentry.Span
import io.sentry.SpanStatus
import io.sentry.TransactionOptions
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue
import org.centrexcursionistalcoi.app.database.table.Files
import org.jetbrains.exposed.v1.jdbc.selectAll

class TestSentryStatementInterceptor {
    @BeforeTest
    fun setUp() {
        Database.initForTests()
        Sentry.init { options ->
            options.dsn = "https://key@sentry.invalid/1"
            options.tracesSampleRate = 1.0
            options.setTransportFactory(NoOpTransportFactory.getInstance())
        }
    }

    @AfterTest
    fun tearDown() {
        Sentry.close()
        Database.clear()
    }

    private fun traced(block: () -> Unit): List<Span> {
        val transaction: ITransaction = Sentry.startTransaction(
            "test",
            "test",
            TransactionOptions().apply { isBindToScope = true },
        )
        try {
            block()
        } finally {
            transaction.finish()
        }
        return (transaction as SentryTracer).spans.filter { it.operation == "db.sql.query" }
    }

    @Test
    fun test_statement_recordedAsSpan() {
        val spans = traced { Database { Files.selectAll().count() } }

        val span = spans.single()
        assertTrue(span.description!!.contains("FROM ${Files.tableName}", ignoreCase = true), span.description)
        assertTrue(span.isFinished)
        assertEquals(SpanStatus.OK, span.status)
    }

    @Test
    fun test_failedStatement_finishedAsError() {
        val spans = traced {
            assertFails { Database { exec("SELECT * FROM table_that_does_not_exist") } }
        }

        assertTrue(spans.isNotEmpty())
        spans.forEach { span ->
            assertTrue(span.isFinished)
            assertEquals(SpanStatus.INTERNAL_ERROR, span.status)
        }
    }

    @Test
    fun test_statementOutsideTransaction_notRecorded() {
        // Nothing to attach the span to: must just run the statement
        Database { Files.selectAll().count() }
    }
}
