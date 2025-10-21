package org.centrexcursionistalcoi.app

import io.sentry.kotlin.multiplatform.Sentry

fun initializeSentry() {
    Sentry.init { options ->
        options.dsn = BuildKonfig.SENTRY_DSN
        // Adds request headers and IP for users, for more info visit:
        // https://docs.sentry.io/platforms/kotlin/guides/kotlin-multiplatform/data-management/data-collected/
        options.sendDefaultPii = true
    }
}
