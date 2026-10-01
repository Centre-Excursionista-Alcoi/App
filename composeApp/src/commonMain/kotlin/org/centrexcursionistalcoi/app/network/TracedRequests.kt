package org.centrexcursionistalcoi.app.network

import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpMethod
import io.ktor.http.contentLength
import io.ktor.http.encodedPath
import org.centrexcursionistalcoi.app.log.TraceOperation
import org.centrexcursionistalcoi.app.log.traceSpan

/**
 * Makes the request [block] builds, recorded as a [TraceOperation.HTTP_CLIENT] span of the current trace, if any (see
 * [traceSpan]), described by its method and path.
 *
 * The span includes downloading the response's body: like [HttpClient.request], this only returns once the whole body
 * has been received (reading it afterwards doesn't touch the network).
 */
suspend fun HttpClient.requestTraced(block: suspend HttpRequestBuilder.() -> Unit): HttpResponse {
    val builder = HttpRequestBuilder().also { it.block() }
    // Resource paths are relative (resolved against the client's base URL when sent)
    val path = "/" + builder.url.encodedPath.removePrefix("/")
    return traceSpan(TraceOperation.HTTP_CLIENT, "${builder.method.value} $path") { span ->
        request(builder).also { response ->
            span?.setData("http.response.status_code", response.status.value.toLong())
            response.contentLength()?.let { span?.setData("http.response_content_length", it) }
        }
    }
}

/** A `GET` [requestTraced]. */
suspend fun HttpClient.getTraced(block: suspend HttpRequestBuilder.() -> Unit): HttpResponse =
    requestTraced {
        method = HttpMethod.Get
        block()
    }

/** A `GET` [requestTraced] to an `Api` [resource]. */
suspend inline fun <reified T : Any> HttpClient.getTraced(
    resource: T,
    crossinline block: suspend HttpRequestBuilder.() -> Unit,
): HttpResponse {
    val client = this
    return getTraced {
        resource(client, resource)
        block()
    }
}
