package org.centrexcursionistalcoi.app.auth

import org.koin.core.annotation.Singleton

// Not implemented on desktop yet -- AuthBackend.tryAutoRelogin() always falls through to a normal logout there.
@Singleton
actual class CredentialsStore {
    actual fun save(email: String, password: String) {}
    actual fun get(): SavedCredentials? = null
    actual fun clear() {}
}
