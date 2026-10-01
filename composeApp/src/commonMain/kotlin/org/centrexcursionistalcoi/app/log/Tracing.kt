package org.centrexcursionistalcoi.app.log

import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withContext

/**
 * A Sentry span (a transaction, or one of its children).
 *
 * The Kotlin Multiplatform SDK has no tracing API, so each platform implements this with its native SDK. When Sentry
 * is not initialized (debug builds, see [initializeSentry]), the native SDKs hand out no-op spans, so nothing is sent.
 */
expect class TraceSpan {
    fun startChild(operation: String, description: String? = null): TraceSpan

    /** Tags are indexed and can be searched and grouped by in Sentry. Keep their values low-cardinality. */
    fun setTag(key: String, value: String)

    fun setData(key: String, value: String)
    fun setData(key: String, value: Long)
    fun setData(key: String, value: Boolean)

    fun finish(status: TraceStatus)
}

enum class TraceStatus { OK, CANCELLED, INTERNAL_ERROR }

/**
 * Starts a new transaction, the root of a trace. Only sampled (and so sent) if the user allows analytics, see
 * [initializeSentry].
 */
expect fun startTraceTransaction(name: String, operation: String): TraceSpan

/**
 * Carries the span that [traceSpan] creates its children under, so it doesn't have to be passed around explicitly
 * (and works across threads, unlike the native SDKs' own scope).
 */
private class CurrentTraceSpan(val span: TraceSpan) : AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<CurrentTraceSpan>
}

private suspend fun <T> TraceSpan.runInside(block: suspend (TraceSpan) -> T): T {
    try {
        return withContext(CurrentTraceSpan(this)) { block(this@runInside) }.also { finish(TraceStatus.OK) }
    } catch (e: CancellationException) {
        finish(TraceStatus.CANCELLED)
        throw e
    } catch (e: Throwable) {
        finish(TraceStatus.INTERNAL_ERROR)
        throw e
    }
}

/**
 * Runs [block] inside a new transaction, finished when it returns or throws. [traceSpan] calls inside it are
 * recorded as its children.
 */
suspend fun <T> traceTransaction(name: String, operation: String, block: suspend (TraceSpan) -> T): T =
    startTraceTransaction(name, operation).runInside(block)

/**
 * Runs [block] inside a new child of the current span, if any: outside a [traceTransaction], [block] is just run,
 * with a `null` span.
 */
suspend fun <T> traceSpan(operation: String, description: String? = null, block: suspend (TraceSpan?) -> T): T {
    val parent = currentCoroutineContext()[CurrentTraceSpan]?.span ?: return block(null)
    return parent.startChild(operation, description).runInside(block)
}
