package org.centrexcursionistalcoi.app.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.origin
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.plugins.ratelimit.RateLimitName
import kotlin.time.Duration.Companion.minutes

object RateLimits {
    /**
     * Everything that checks a password, a WebAuthn credential or a session cookie, or sends emails: login,
     * registration, password recovery, and restore-key challenges and verification. Limits password guessing,
     * email flooding and challenge spam.
     */
    val AUTHENTICATION = RateLimitName("authentication")

    /** Refreshing tokens and logging out, which clients do far more often than logging in. */
    val TOKEN_REFRESH = RateLimitName("token-refresh")
}

/**
 * Per-client-IP limits for [RateLimits]. The IP is the one nginx reports (see [configureForwardedHeaders]), which
 * the client can't spoof. Over the limit, requests get `429 Too Many Requests` with a `Retry-After` header.
 * @param isTesting Tests exercise these routes far more often than any real client, so limits are lifted for them.
 */
fun Application.configureRateLimits(isTesting: Boolean) {
    install(RateLimit) {
        register(RateLimits.AUTHENTICATION) {
            rateLimiter(limit = if (isTesting) Int.MAX_VALUE else 30, refillPeriod = 5.minutes)
            requestKey { call -> call.request.origin.remoteHost }
        }
        register(RateLimits.TOKEN_REFRESH) {
            rateLimiter(limit = if (isTesting) Int.MAX_VALUE else 60, refillPeriod = 5.minutes)
            requestKey { call -> call.request.origin.remoteHost }
        }
    }
}
