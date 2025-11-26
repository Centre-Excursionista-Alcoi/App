package org.centrexcursionistalcoi.app.log

import com.diamondedge.logging.KmLogging
import io.sentry.kotlin.multiplatform.Sentry
import org.centrexcursionistalcoi.app.BuildKonfig
import org.centrexcursionistalcoi.app.storage.SETTINGS_PRIVACY_ANALYTICS
import org.centrexcursionistalcoi.app.storage.SETTINGS_PRIVACY_ERRORS
import org.centrexcursionistalcoi.app.storage.SETTINGS_PRIVACY_SESSION_REPLAY
import org.centrexcursionistalcoi.app.storage.settings

private const val SESSION_REPLAY_ON_ERROR_SAMPLE_RATE = 1.0
private const val SESSION_REPLAY_SESSION_SAMPLE_RATE = 0.1

fun initializeSentry() {
    val reportErrors = settings.getBoolean(SETTINGS_PRIVACY_ERRORS, true)
    val reportAnalytics = settings.getBoolean(SETTINGS_PRIVACY_ANALYTICS, true)
    val reportSessionReplay = settings.getBoolean(SETTINGS_PRIVACY_SESSION_REPLAY, true)

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

        // Disable ANR tracking
        options.isAnrEnabled = false

        // Privacy settings: adjust data collection based on user preferences
        options.enableAppHangTracking = reportErrors
        options.enableAutoSessionTracking = reportErrors
        options.enableCaptureFailedRequests = reportErrors
        options.sampleRate = if (reportAnalytics) 1.0 else 0.0
        options.tracesSampleRate = if (reportAnalytics) 1.0 else 0.0

        // Session Replay configuration
        options.sessionReplay.apply {
            onErrorSampleRate = if (reportSessionReplay) SESSION_REPLAY_SESSION_SAMPLE_RATE else 0.0
            sessionSampleRate = if (reportSessionReplay) SESSION_REPLAY_ON_ERROR_SAMPLE_RATE else 0.0

            maskAllText = false
            maskAllImages = false
        }
    }

    if (BuildKonfig.SENTRY_DSN != null) {
        KmLogging.addLogger(SentryLogger())
    }
}
