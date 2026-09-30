package org.centrexcursionistalcoi.app.auth

import org.koin.core.annotation.Singleton

/** A way to sign in the platform's sign-in sheet returned, see [Passkeys.signIn]. */
sealed interface SavedCredential {
    /** A passkey: its WebAuthn response, for the server to verify. */
    class Passkey(val authenticationResponseJson: String) : SavedCredential

    /** A password saved in the platform's password manager. */
    class Password(val email: String, val password: String) : SavedCredential {
        override fun toString(): String = "Password(email=$email, password=<redacted>)"
    }
}

/** The platform couldn't create or use a passkey, for a reason other than the user cancelling. */
class PasskeyException(message: String?, cause: Throwable? = null) : Exception(message, cause)

/**
 * The platform's passkeys and saved passwords: Android's Credential Manager, and iOS's AuthenticationServices.
 * Desktop has neither ([isSupported] is `false`): it signs in with a password only.
 *
 * The requests and responses are WebAuthn's JSON, as the server's routes hand them out and receive them.
 */
@Singleton
expect class Passkeys {
    /** Whether passkeys can be created and used on this device. */
    val isSupported: Boolean

    /** A name for a passkey created on this device, to tell it apart from the user's others. */
    val deviceName: String

    /**
     * Asks the user to create a passkey, for the WebAuthn creation options [requestJson].
     * @return the registration response, or `null` if the user cancelled.
     * @throws PasskeyException if the passkey couldn't be created.
     */
    suspend fun create(requestJson: String): String?

    /**
     * Creates a passkey without asking, if the platform allows it: right after the user signed in with a password
     * from the password manager, the platform can upgrade the account to a passkey on its own (Android's
     * conditional creation, iOS 18's automatic passkey upgrade).
     * @return the registration response, or `null` if the platform didn't create one.
     */
    suspend fun createAutomatically(requestJson: String): String?

    /**
     * Shows the platform's sign-in sheet, with the user's passkeys for the WebAuthn request options [requestJson],
     * and their saved passwords.
     * @param includePasswords Whether to offer saved passwords too, or only passkeys (e.g. to confirm it's the user,
     * see [org.centrexcursionistalcoi.app.data.Reauthentication]).
     * @return what the user picked, or `null` if they cancelled or have nothing saved.
     * @throws PasskeyException if the sheet couldn't be shown.
     */
    suspend fun signIn(requestJson: String, includePasswords: Boolean = true): SavedCredential?

    /** Offers to save the password the user just signed in with, if the platform doesn't do it on its own. */
    suspend fun savePassword(email: String, password: String)
}
