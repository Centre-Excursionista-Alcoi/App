package org.centrexcursionistalcoi.app

import com.diamondedge.logging.logging
import io.sentry.kotlin.multiplatform.Sentry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.centrexcursionistalcoi.app.error.Error
import org.centrexcursionistalcoi.app.exception.ServerException

object GlobalAsyncErrorHandler {
    private val log = logging()
    private val _error = MutableStateFlow<Throwable?>(null)
    val error get() = _error.asStateFlow()

    // "Not logged in" is an expected condition (an expired/invalidated session), not an error to alarm the
    // user with -- every RemoteRepository call funnels its failures through here, so this is the one place
    // that can catch it regardless of which repository/screen triggered it. See #620/the App-wide crash it
    // caused when left unhandled: a full "Error descontrolat" dialog instead of a graceful re-login.
    private val _sessionExpired = MutableStateFlow(false)
    val sessionExpired get() = _sessionExpired.asStateFlow()

    fun setError(throwable: Throwable) {
        if (throwable is CancellationException) {
            // Ignore cancellations
            log.e { "Coroutine cancelled." }
            return
        }

        if (throwable is ServerException && throwable.errorCode == Error.ERROR_NOT_LOGGED_IN) {
            log.w { "Session expired." }
            _sessionExpired.value = true
            return
        }

        log.e(throwable) { "Unhandled exception" }
        Sentry.captureException(throwable)
        _error.value = throwable
    }

    fun clearError() {
        _error.value = null
    }

    fun clearSessionExpired() {
        _sessionExpired.value = false
    }
}
