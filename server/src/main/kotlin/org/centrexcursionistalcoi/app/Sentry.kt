package org.centrexcursionistalcoi.app

import io.sentry.Sentry
import org.slf4j.LoggerFactory

object Sentry {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun initializeSentry() {
        val sentryDsn = this::class.java
            .getResourceAsStream("/sentry_dsn.txt")
            ?.bufferedReader()
            ?.use { it.readText() }
            ?: error("Could not find sentry_dsn.txt resource. Try running the copySentryDsn task.")

        Sentry.init { options ->
            options.dsn = sentryDsn
            // Set tracesSampleRate to 1.0 to capture 100% of transactions for tracing.
            // We recommend adjusting this value in production.
            options.tracesSampleRate = 1.0
            // When first trying Sentry it's good to see what the SDK is doing:
            options.isDebug = true
        }

        logger.info("Sentry is ready")
    }
}
