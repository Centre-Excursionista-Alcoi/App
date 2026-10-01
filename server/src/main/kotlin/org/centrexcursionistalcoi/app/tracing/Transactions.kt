package org.centrexcursionistalcoi.app.tracing

import io.sentry.ITransaction
import io.sentry.Sentry
import io.sentry.SpanStatus
import io.sentry.TransactionOptions
import io.sentry.kotlin.SentryContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext

/**
 * Runs [block] inside a new Sentry transaction, finished when it returns or throws, for work that doesn't run inside a
 * request (whose transaction `configureSentryTracing()` starts). Spans started meanwhile, like SQL statements and
 * requests to other services, are recorded as its children.
 *
 * The transaction is bound to a scope of its own ([SentryContext]), so it doesn't leak into unrelated work sharing
 * its threads.
 */
suspend fun <T> traceTransaction(name: String, operation: TraceOperation, block: suspend (ITransaction) -> T): T =
    withContext(SentryContext()) {
        val transaction = Sentry.startTransaction(
            name,
            operation.value,
            TransactionOptions().apply { isBindToScope = true },
        )
        try {
            block(transaction).also { transaction.finish(SpanStatus.OK) }
        } catch (e: CancellationException) {
            transaction.finish(SpanStatus.CANCELLED)
            throw e
        } catch (e: Throwable) {
            transaction.throwable = e
            transaction.finish(SpanStatus.INTERNAL_ERROR)
            throw e
        }
    }
