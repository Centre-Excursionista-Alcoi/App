package org.centrexcursionistalcoi.app.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.annotation.Singleton

// Not implemented on desktop yet -- AuthBackend.tryAutoRelogin() always falls through to a normal logout there.
@Singleton
actual class CredentialsStore {
    actual val current: StateFlow<SavedCredentials?>
        field = MutableStateFlow(null)

    actual suspend fun save(email: String, password: String) {}
    actual suspend fun get(): SavedCredentials? = null
    actual suspend fun clear() {}
    actual suspend fun restoreSession(): Boolean = false
}
