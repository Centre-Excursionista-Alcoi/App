package org.centrexcursionistalcoi.app

import io.sentry.kotlin.multiplatform.Sentry

fun initializeSentry() {
    Sentry.init { options ->
        options.dsn = BuildKonfig.SENTRY_DSN
        // Adds request headers and IP for users, for more info visit:
        // https://docs.sentry.io/platforms/kotlin/guides/kotlin-multiplatform/data-management/data-collected/
        options.sendDefaultPii = true

        // Capture screenshots on error events
        options.attachScreenshot = true

        // Set the environment
        options.environment = if (BuildKonfig.DEBUG) "development" else "production"

        // Set the release name
        options.release = BuildKonfig.VERSION_NAME + "/" + BuildKonfig.VERSION_CODE

        // Session Replay configuration
        options.sessionReplay.apply {
            onErrorSampleRate = 1.0
            sessionSampleRate = 0.1

            maskAllText = false
            maskAllImages = false
        }
    }
}
