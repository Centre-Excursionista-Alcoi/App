package org.centrexcursionistalcoi.app.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.routing.RoutingPipelineCall
import io.ktor.server.routing.routing
import io.sentry.Sentry
import io.sentry.SpanStatus
import io.sentry.TransactionOptions
import io.sentry.kotlin.SentryContext
import kotlinx.coroutines.withContext

/**
 * Wraps every request in a Sentry transaction, named/tagged by its resolved route (e.g.
 * "GET /users/{sub}") rather than the raw request path, so requests to the same endpoint with
 * different path parameters group together instead of creating one transaction name per value.
 *
 * Registered via [routing] (reusing the tree installed by `configureRouting()`) rather than
 * directly on [Application]: the top-level `Application` pipeline only ever sees the plain,
 * pre-routing call object, while the resolved route is only available on the [RoutingPipelineCall]
 * that routing substitutes in for its own pipeline -- so this has to run inside that pipeline to
 * observe it.
 *
 * Requests are additionally isolated with [SentryContext] so the bound transaction doesn't leak
 * across concurrent requests that happen to share a thread across suspension points.
 */
fun Application.configureSentryTracing() {
    routing {
        intercept(ApplicationCallPipeline.Setup) {
            withContext(SentryContext()) {
                val method = call.request.httpMethod.value
                val transaction = Sentry.startTransaction(
                    "$method ${call.request.path()}",
                    "http.server",
                    TransactionOptions().apply { isBindToScope = true },
                )
                try {
                    proceed()
                } finally {
                    (call as? RoutingPipelineCall)?.route?.toString()?.let { route ->
                        transaction.setName("$method $route")
                        transaction.setTag("http.route", route)
                    }
                    transaction.setTag("http.method", method)
                    val statusCode = call.response.status()?.value
                    if (statusCode != null) transaction.setData("http.status_code", statusCode)
                    transaction.finish(SpanStatus.fromHttpStatusCode(statusCode, SpanStatus.INTERNAL_ERROR))
                }
            }
        }
    }
}
