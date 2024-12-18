package org.centrexcursionistalcoi.app

import io.sentry.Sentry
import org.slf4j.LoggerFactory

object Sentry {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun initializeSentry() {
        val sentryDsn = System.getenv("SENTRY_DSN")
        if (sentryDsn == null) {
            logger.warn("SENTRY_DSN environment variable not set. Sentry won't be enabled.")
            return
        }

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
