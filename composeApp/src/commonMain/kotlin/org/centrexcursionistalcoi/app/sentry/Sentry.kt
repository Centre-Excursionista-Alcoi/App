package org.centrexcursionistalcoi.app.sentry

import io.sentry.kotlin.multiplatform.Sentry
import org.centrexcursionistalcoi.app.BuildKonfig

fun initializeSentry() {
    Sentry.init { options ->
        options.dsn = BuildKonfig.SENTRY_DSN
        options.release = BuildKonfig.VERSION + '+' + BuildKonfig.CODE

        options.sampleRate = 1.0
        options.tracesSampleRate = 1.0
    }
}
