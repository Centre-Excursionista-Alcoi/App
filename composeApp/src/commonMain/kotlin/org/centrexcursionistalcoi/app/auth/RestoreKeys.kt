package org.centrexcursionistalcoi.app.auth

import org.centrexcursionistalcoi.app.data.TokenResponse
import org.koin.core.annotation.Singleton

/**
 * Restore keys: a WebAuthn credential registered for the logged-in account, which the platform backs up and
 * restores to a new device, so the account can be logged in there without the password (Android's Restore
 * Credentials). A no-op on platforms without them.
 */
@Singleton
expect class RestoreKeys {
    /** Registers a restore key for the account that has just logged in, in the background. */
    fun create()

    /** Removes this device's restore key, in the background, so it can't log back in. */
    fun clear()

    /**
     * Starts a new session with this device's restore key.
     * @return the session's tokens, or `null` if there's no restore key or it couldn't be redeemed.
     */
    suspend fun redeem(): TokenResponse?
}
