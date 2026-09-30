package org.centrexcursionistalcoi.app.data

import kotlin.time.Instant
import kotlinx.serialization.Serializable

/** Asks the server to email a registration code to [email], proving the person registering owns it. */
@Serializable
data class RegistrationCodeRequest(val email: String)

/** Asks for the options to create the passkey of a new account, see [PasskeyRegistrationRequest]. */
@Serializable
data class PasskeyRegistrationOptionsRequest(val email: String, val code: String)

/**
 * Registers a new account that signs in with a passkey instead of a password.
 * @param code The code emailed with [RegistrationCodeRequest].
 * @param registrationResponseJson The platform's response to the options of [PasskeyRegistrationOptionsRequest].
 * @param name A name for the passkey, to tell it apart from the user's others (e.g. the device's).
 */
@Serializable
data class PasskeyRegistrationRequest(
    val email: String,
    val code: String,
    val registrationResponseJson: String,
    val name: String? = null,
)

/** Adds a passkey to the logged-in account. */
@Serializable
data class AddPasskeyRequest(val registrationResponseJson: String, val name: String? = null)

/**
 * Proves, again, that it's really the user making a sensitive change to their account (removing a passkey, or
 * setting or removing the password), so that a stolen session can't take the account over. Exactly one of
 * [password] or [authenticationResponseJson] (a passkey sign-in, for the options of `/generate-auth-challenge`).
 */
@Serializable
data class Reauthentication(
    val password: String? = null,
    val authenticationResponseJson: String? = null,
)

@Serializable
data class ReauthenticatedRequest(val reauthentication: Reauthentication)

@Serializable
data class SetPasswordRequest(val reauthentication: Reauthentication, val newPassword: String)

/** How the logged-in user can sign in. */
@Serializable
data class SecurityInfo(val hasPassword: Boolean, val passkeys: List<PasskeyInfo>)

@Serializable
data class PasskeyInfo(
    /** The credential id, Base64Url without padding. */
    val id: String,
    val name: String?,
    val createdAt: Instant,
    val lastUsedAt: Instant?,
)
