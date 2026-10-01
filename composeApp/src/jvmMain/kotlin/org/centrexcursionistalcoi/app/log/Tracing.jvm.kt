package org.centrexcursionistalcoi.app.log

import io.sentry.ISpan
import io.sentry.Sentry
import io.sentry.SpanStatus

actual class TraceSpan(private val span: ISpan) {
    actual fun startChild(operation: TraceOperation, description: String?): TraceSpan =
        TraceSpan(span.startChild(operation.value, description))

    actual fun setTag(key: String, value: String) = span.setTag(key, value)

    actual fun setData(key: String, value: String) = span.setData(key, value)
    actual fun setData(key: String, value: Long) = span.setData(key, value)
    actual fun setData(key: String, value: Boolean) = span.setData(key, value)

    actual fun finish(status: TraceStatus) = span.finish(
        when (status) {
            TraceStatus.OK -> SpanStatus.OK
            TraceStatus.CANCELLED -> SpanStatus.CANCELLED
            TraceStatus.INTERNAL_ERROR -> SpanStatus.INTERNAL_ERROR
        }
    )
}

actual fun startTraceTransaction(name: String, operation: TraceOperation): TraceSpan =
    TraceSpan(Sentry.startTransaction(name, operation.value))
