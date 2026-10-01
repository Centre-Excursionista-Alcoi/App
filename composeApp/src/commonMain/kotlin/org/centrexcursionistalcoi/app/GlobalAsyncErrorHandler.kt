package org.centrexcursionistalcoi.app

import com.diamondedge.logging.logging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.centrexcursionistalcoi.app.error.Error
import org.centrexcursionistalcoi.app.exception.InternetAccessNotAvailable
import org.centrexcursionistalcoi.app.exception.ServerException
import org.centrexcursionistalcoi.app.network.isNoConnectionError

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

    /**
     * Hands what a coroutine throws to [setError], instead of crashing the app: for coroutines launched without
     * catching their errors, e.g. directly in `viewModelScope`.
     */
    val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable -> setError(throwable) }

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

        if (throwable.isConnectivityError()) {
            // Shown to the user, but nothing to fix in the app
            log.w(throwable) { "Network error." }
        } else {
            // Reported to Sentry by SentryLogger
            log.e(throwable) { "Unhandled exception" }
        }
        error.value = throwable
    }

    /** Whether this, or what caused it, is the device being offline or the connection failing. */
    private fun Throwable.isConnectivityError(): Boolean =
        generateSequence(this) { it.cause.takeIf { cause -> cause !== it } }.take(10).any {
            it is InternetAccessNotAvailable || isNoConnectionError(it)
        }

    fun clearError() {
        error.value = null
    }

    fun clearSessionExpired() {
        sessionExpired.value = false
    }
}
