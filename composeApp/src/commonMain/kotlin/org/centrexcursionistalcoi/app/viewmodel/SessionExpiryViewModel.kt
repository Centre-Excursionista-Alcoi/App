package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diamondedge.logging.logging
import kotlinx.coroutines.launch
import org.centrexcursionistalcoi.app.GlobalAsyncErrorHandler
import org.centrexcursionistalcoi.app.auth.AuthBackend
import org.koin.core.annotation.KoinViewModel

/**
 * Reacts to [GlobalAsyncErrorHandler.sessionExpired] app-wide: tries a silent re-login (see
 * [AuthBackend.tryAutoRelogin]) before falling back to a real [AuthBackend.logout]. Deliberately a ViewModel
 * rather than a Composable's `LaunchedEffect` -- this is critical, side-effecting logic (it can wipe local
 * data via `logout()`), and `viewModelScope` isn't tied to Compose recomposition the way a `LaunchedEffect`
 * is. That distinction isn't cosmetic: this used to live directly in `App.kt`, and its effect would
 * self-cancel (`LeftCompositionCancellationException`) mid-relogin whenever an unrelated recomposition
 * happened to land first -- see the discussion on #624.
 *
 * [onLoggedOut] is invoked directly (rather than exposing an event `Flow` for the caller to `collect`) so
 * `App.kt` doesn't need a `LaunchedEffect` of its own just to forward it into navigation -- one less place
 * doing UI-adjacent work outside a ViewModel.
 */
@KoinViewModel
class SessionExpiryViewModel(
    private val authBackend: AuthBackend,
    private val onLoggedOut: () -> Unit,
) : ViewModel() {
    private val log = logging()

    init {
        viewModelScope.launch {
            GlobalAsyncErrorHandler.sessionExpired.collect { expired ->
                if (!expired) return@collect

                log.d { "Session expired, attempting automatic re-login..." }
                if (!authBackend.tryAutoRelogin()) {
                    log.d { "Automatic re-login not possible or failed, logging out..." }
                    authBackend.logout()
                    onLoggedOut()
                }
                GlobalAsyncErrorHandler.clearSessionExpired()
            }
        }
    }
}
