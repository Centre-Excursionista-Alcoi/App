package org.centrexcursionistalcoi.app

import com.diamondedge.logging.logging
import io.sentry.kotlin.multiplatform.Sentry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.centrexcursionistalcoi.app.error.Error
import org.centrexcursionistalcoi.app.exception.ServerException

object GlobalAsyncErrorHandler {
    private val log = logging()

    val error: StateFlow<Throwable?>
        field = MutableStateFlow(null)

    // "Not logged in" is an expected condition (an expired/invalidated session), not an error to alarm the
    // user with -- every RemoteRepository call funnels its failures through here, so this is the one place
    // that can catch it regardless of which repository/screen triggered it. See #620/the App-wide crash it
    // caused when left unhandled: a full "Error descontrolat" dialog instead of a graceful re-login.
    val sessionExpired: StateFlow<Boolean>
        field = MutableStateFlow(false)

    fun setError(throwable: Throwable) {
        if (throwable is CancellationException) {
            // Ignore cancellations
            log.e { "Coroutine cancelled." }
            return
        }

        if (throwable is ServerException && throwable.errorCode == Error.ERROR_NOT_LOGGED_IN) {
            log.w { "Session expired." }
            sessionExpired.value = true
            return
        }

        log.e(throwable) { "Unhandled exception" }
        Sentry.captureException(throwable)
        error.value = throwable
    }

    fun clearError() {
        error.value = null
    }

    fun clearSessionExpired() {
        sessionExpired.value = false
    }
}
