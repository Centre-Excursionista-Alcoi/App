package org.centrexcursionistalcoi.app.auth

import org.koin.core.annotation.Singleton

// Desktop signs in with a password only.
@Singleton
actual class Passkeys {
    actual val isSupported: Boolean = false
    actual val deviceName: String = System.getProperty("os.name") ?: "Desktop"
    actual suspend fun create(requestJson: String): String? = null
    actual suspend fun createAutomatically(requestJson: String): String? = null
    actual suspend fun signIn(requestJson: String): SavedCredential? = null
    actual suspend fun savePassword(email: String, password: String) {}
}
