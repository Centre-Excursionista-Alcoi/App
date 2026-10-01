package org.centrexcursionistalcoi.app.tracing

import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.statement.HttpReceivePipeline
import io.ktor.http.contentLength
import io.ktor.http.encodedPath
import io.ktor.util.AttributeKey
import io.ktor.util.pipeline.PipelinePhase
import io.sentry.ISpan
import io.sentry.Sentry
import io.sentry.SpanStatus

private val RequestSpan = AttributeKey<ISpan>("SentryRequestSpan")

/** Runs right before Ktor's `SaveBody` plugin, which downloads the whole body of every response that isn't streamed. */
private val DownloadBody = PipelinePhase("SentryDownloadBody")

/**
 * Records every request of the client it's installed in as a [TraceOperation.HTTP_CLIENT] span of the current Sentry
 * span (a request's transaction, or a `PeriodicWorker` run's), described by its method and path. Requests made outside
 * any transaction aren't recorded.
 *
 * The span lasts until the whole response body has been downloaded (Ktor does so before the request returns), and a
 * [TraceOperation.HTTP_CLIENT_BODY] child span measures the download on its own.
 *
 * Only the method, host and path are recorded: never the query, headers or body, which may carry credentials.
 */
val SentryHttpClientTracing = createClientPlugin("SentryHttpClientTracing") {
    client.receivePipeline.insertPhaseBefore(HttpReceivePipeline.Before, DownloadBody)
    client.receivePipeline.intercept(DownloadBody) { response ->
        val span = response.call.attributes.getOrNull(RequestSpan) ?: return@intercept
        span.traceChild(TraceOperation.HTTP_CLIENT_BODY, "Response body") { body ->
            response.contentLength()?.let { body.setData("http.response.body.size", it) }
            proceed()
        }
    }

    on(Send) { request ->
        val parent = Sentry.getSpan() ?: return@on proceed(request)
        val span = parent.startChild(
            TraceOperation.HTTP_CLIENT.value,
            "${request.method.value} /${request.url.encodedPath.removePrefix("/")}",
        )
        span.setData("server.address", request.url.host)
        request.attributes.put(RequestSpan, span)
        try {
            val call = proceed(request)
            val status = call.response.status.value
            span.setData("http.response.status_code", status)
            span.finish(SpanStatus.fromHttpStatusCode(status, SpanStatus.UNKNOWN))
            call
        } catch (e: Throwable) {
            span.throwable = e
            span.finish(SpanStatus.INTERNAL_ERROR)
            throw e
        }
    }
}

private inline fun <T> ISpan.traceChild(operation: TraceOperation, description: String, block: (ISpan) -> T): T {
    val child = startChild(operation.value, description)
    try {
        return block(child).also { child.finish(SpanStatus.OK) }
    } catch (e: Throwable) {
        child.throwable = e
        child.finish(SpanStatus.INTERNAL_ERROR)
        throw e
    }
}
