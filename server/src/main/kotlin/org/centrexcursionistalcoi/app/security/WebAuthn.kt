package org.centrexcursionistalcoi.app.security

import com.webauthn4j.WebAuthnManager
import com.webauthn4j.credential.CredentialRecordImpl
import com.webauthn4j.data.AuthenticationParameters
import com.webauthn4j.data.RegistrationParameters
import com.webauthn4j.converter.AttestedCredentialDataConverter
import com.webauthn4j.converter.util.ObjectConverter
import com.webauthn4j.data.client.Origin
import com.webauthn4j.data.client.challenge.DefaultChallenge
import com.webauthn4j.server.ServerProperty
import com.webauthn4j.verifier.exception.VerificationException
import org.centrexcursionistalcoi.app.AppLinks
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.UserCredentialRecordEntity
import org.centrexcursionistalcoi.app.database.entity.UserReferenceEntity
import org.centrexcursionistalcoi.app.database.table.CredentialKind
import org.centrexcursionistalcoi.app.database.table.UserCredentialRecords
import org.centrexcursionistalcoi.app.error.Error
import org.centrexcursionistalcoi.app.routes.WellKnownConfigProvider
import org.centrexcursionistalcoi.app.storage.RedisStoreMap
import java.security.SecureRandom
import java.util.Base64
import kotlin.time.Clock

// Use the non-strict manager to bypass hardware attestation certificate checks -- this app doesn't need to know
// *which* authenticator model created a credential, only that the signature is valid.
val webAuthnManager = WebAuthnManager.createNonStrictWebAuthnManager()

private val objectConverter = ObjectConverter()

/** (De)serializes an [com.webauthn4j.data.attestation.authenticator.AttestedCredentialData] to/from the bytes stored in [UserCredentialRecords][org.centrexcursionistalcoi.app.database.table.UserCredentialRecords]. */
val attestedCredentialDataConverter = AttestedCredentialDataConverter(objectConverter)

/**
 * The WebAuthn relying party ID: must be a real domain the app is associated with via Digital Asset Links (see
 * the `delegate_permission/common.get_login_creds` relation in `WellKnownRoutes.kt`'s `assetlinks.json`), never
 * the Android package name -- Android's Credential Manager validates `rp.id` against that association, and a
 * package name isn't a domain.
 */
val webAuthnRpId: String get() = AppLinks.host

/**
 * The origins WebAuthn responses may come from:
 * - The Android app's signing certificate(s), as the `android:apk-key-hash:` origins Android's Credential Manager
 *   presents when the request comes from the native app (there's no custom URL scheme, so this is the only origin
 *   an Android request can ever have). Reuses the same SHA-256 fingerprints already configured for
 *   `assetlinks.json` (colon-separated hex) rather than duplicating them in a second env var, converting each to
 *   the Base64Url form this origin format expects.
 * - `https://` [webAuthnRpId], the origin iOS presents for the native app, which is associated with the domain
 *   through the `webcredentials` of `apple-app-site-association`.
 */
val webAuthnOrigins: Set<Origin> get() = WellKnownConfigProvider.sha256CertFingerprints.map { fingerprint ->
    val bytes = fingerprint.replace(":", "").hexToByteArray()
    val base64Url = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    Origin.create("android:apk-key-hash:$base64Url")
}.toSet() + Origin.create("https://$webAuthnRpId")

private val secureRandom = SecureRandom()

fun generateWebAuthnChallenge(): DefaultChallenge {
    // Generate 32 random bytes
    val randomBytes = ByteArray(32)
    secureRandom.nextBytes(randomBytes)

    // Wrap it in WebAuthn4J's DefaultChallenge
    return DefaultChallenge(randomBytes)
}

/** Base64Url without padding: the encoding WebAuthn uses throughout. */
fun ByteArray.toBase64Url(): String = Base64.getUrlEncoder().withoutPadding().encodeToString(this)

/** A credential whose registration has been verified, ready to be stored in [UserCredentialRecords]. */
class RegisteredCredential(
    /** Base64Url without padding. */
    val credentialId: String,
    val attestedCredentialData: ByteArray,
    val signCount: Long,
)

/** The challenge a registration response was created for (Base64Url, no padding), before verifying it. */
fun registrationChallenge(registrationResponseJson: String): String? =
    webAuthnManager.parseRegistrationResponseJSON(registrationResponseJson)
        .collectedClientData?.challenge?.value?.toBase64Url()

/**
 * Verifies a WebAuthn registration response against the [challengeBase64Url] it was created for.
 * @param userVerificationRequired Whether the user must have unlocked the device: always for passkeys, never for
 * restore keys, which are created silently.
 * @throws VerificationException if the response doesn't check out.
 */
fun verifyRegistration(
    registrationResponseJson: String,
    challengeBase64Url: String,
    userVerificationRequired: Boolean,
): RegisteredCredential {
    val serverProperty = ServerProperty.builder()
        .rpId(webAuthnRpId)
        .origins(webAuthnOrigins)
        .challenge(DefaultChallenge(Base64.getUrlDecoder().decode(challengeBase64Url)))
        .build()

    val parsed = webAuthnManager.parseRegistrationResponseJSON(registrationResponseJson)
    // pubKeyCredParams = null: accept any algorithm, matching the non-strict manager's own leniency.
    val registrationParameters = RegistrationParameters(serverProperty, null, userVerificationRequired)
    val registrationData = webAuthnManager.verify(parsed, registrationParameters)

    val authenticatorData = registrationData.attestationObject!!.authenticatorData
    val attestedCredentialData = authenticatorData.attestedCredentialData!!
    return RegisteredCredential(
        credentialId = attestedCredentialData.credentialId.toBase64Url(),
        attestedCredentialData = attestedCredentialDataConverter.convert(attestedCredentialData),
        signCount = authenticatorData.signCount,
    )
}

sealed interface AssertionVerification {
    data class Success(val user: UserReferenceEntity, val kind: CredentialKind) : AssertionVerification
    data class Failure(val error: Error) : AssertionVerification
}

/**
 * Verifies a WebAuthn authentication response (a restore key or a passkey) against the challenge issued by
 * `/generate-auth-challenge` and the credential stored at registration, resolving the user it authenticates.
 *
 * Passkeys must have verified the user (unlocked the device); restore keys are redeemed silently, so they don't.
 * @param user If given, the credential must be one of this user's passkeys (to confirm it's really them, see
 * [org.centrexcursionistalcoi.app.data.Reauthentication]).
 */
suspend fun verifyAssertion(authenticationResponseJson: String, user: String? = null): AssertionVerification {
    try {
        val authenticationData = webAuthnManager.parseAuthenticationResponseJSON(authenticationResponseJson)

        // The challenge the client used is echoed back inside its own response -- no separate tracking ID
        // needed, the same way /generate-auth-challenge stored it keyed by its own value.
        val challengeBytes = authenticationData.collectedClientData?.challenge?.value
            ?: return AssertionVerification.Failure(Error.InvalidArgument("authenticationResponseJson"))
        // Consumed before verifying, so that a challenge can never be used twice, even by concurrent requests or
        // after a failed attempt.
        RedisStoreMap.default.remove("auth_challenge:${challengeBytes.toBase64Url()}")
            ?: return AssertionVerification.Failure(
                Error.InvalidArgument("challenge", "Challenge expired or was never requested.")
            )

        val credentialId = authenticationData.credentialId.toBase64Url()
        val stored = Database { UserCredentialRecordEntity.findById(credentialId) }
            ?: return AssertionVerification.Failure(Error.EntityNotFound(UserCredentialRecordEntity::class, credentialId))
        val (attestedCredentialDataBytes, storedSignCount, kind) = Database {
            Triple(stored.attestedCredentialData, stored.signCount, stored.kind)
        }
        if (user != null) {
            val owner = Database { stored.readValues[UserCredentialRecords.user].value }
            if (owner != user || kind != CredentialKind.PASSKEY) {
                return AssertionVerification.Failure(Error.ReauthenticationFailed())
            }
        }

        val serverProperty = ServerProperty.builder()
            .rpId(webAuthnRpId)
            .origins(webAuthnOrigins)
            .challenge(DefaultChallenge(challengeBytes))
            .build()

        val attestedCredentialData = attestedCredentialDataConverter.convert(attestedCredentialDataBytes)
        // CredentialRecordImpl (WebAuthn Level 3), not the deprecated AuthenticatorImpl -- uvInitialized/
        // backupEligible/backupState/clientData/clientExtensions/transports aren't persisted (not needed to
        // verify a signature), so null throughout; only the pieces saved at registration matter here.
        val credentialRecord = CredentialRecordImpl(
            null, null, null, null,
            storedSignCount, attestedCredentialData, null, null, null, null,
        )
        val authenticationParameters = AuthenticationParameters(
            serverProperty, credentialRecord, null, kind == CredentialKind.PASSKEY,
        )

        val verifiedData = webAuthnManager.verify(authenticationData, authenticationParameters)

        val credentialUser = Database {
            // Many platform authenticators (including the one behind Restore Credentials) always report a
            // signCount of 0 -- still worth persisting whatever comes back, so a future authenticator that
            // does increment it is tracked correctly from here on.
            stored.signCount = verifiedData.authenticatorData!!.signCount
            stored.lastUsedAt = Clock.System.now()
            stored.user
        }
        // Same rule as a password login.
        if (credentialUser.isDisabled) return AssertionVerification.Failure(Error.UserIsDisabled())
        return AssertionVerification.Success(credentialUser, kind)
    } catch (_: VerificationException) {
        // Mirrors a rejected password login: a real credential that just didn't check out, not a malformed
        // request -- same status code and error shape as /login's own failure path.
        return AssertionVerification.Failure(
            if (user != null) Error.ReauthenticationFailed() else Error.IncorrectPasswordOrEmail()
        )
    } catch (e: Exception) {
        return AssertionVerification.Failure(Error.Exception(e))
    }
}
