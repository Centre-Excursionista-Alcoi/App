package org.centrexcursionistalcoi.app.log

import cocoapods.Sentry.SentrySDK
import cocoapods.Sentry.SentrySpanProtocol
import cocoapods.Sentry.SentrySpanStatus
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
actual class TraceSpan(private val span: SentrySpanProtocol) {
    actual fun startChild(operation: String, description: String?): TraceSpan =
        TraceSpan(span.startChildWithOperation(operation, description))

    actual fun setTag(key: String, value: String) = span.setTagValue(value, forKey = key)

    actual fun setData(key: String, value: String) = span.setDataValue(value, forKey = key)
    actual fun setData(key: String, value: Long) = span.setDataValue(value, forKey = key)
    actual fun setData(key: String, value: Boolean) = span.setDataValue(value, forKey = key)

    actual fun finish(status: TraceStatus) = span.finishWithStatus(
        when (status) {
            TraceStatus.OK -> SentrySpanStatus.kSentrySpanStatusOk
            TraceStatus.CANCELLED -> SentrySpanStatus.kSentrySpanStatusCancelled
            TraceStatus.INTERNAL_ERROR -> SentrySpanStatus.kSentrySpanStatusInternalError
        }
    )
}

@OptIn(ExperimentalForeignApi::class)
actual fun startTraceTransaction(name: String, operation: String): TraceSpan =
    TraceSpan(SentrySDK.startTransactionWithName(name, operation = operation))
