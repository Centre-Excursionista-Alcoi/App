package org.centrexcursionistalcoi.app.security

import com.webauthn4j.WebAuthnManager
import com.webauthn4j.converter.AttestedCredentialDataConverter
import com.webauthn4j.converter.util.ObjectConverter
import com.webauthn4j.data.client.Origin
import com.webauthn4j.data.client.challenge.DefaultChallenge
import org.centrexcursionistalcoi.app.AppLinks
import org.centrexcursionistalcoi.app.routes.WellKnownConfigProvider
import java.security.SecureRandom
import java.util.Base64

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
 * The Android app's signing certificate(s), as the `android:apk-key-hash:` origins Android's Credential Manager
 * presents when the request comes from the native app (there's no custom URL scheme, so this is the only origin
 * an Android request can ever have). Reuses the same SHA-256 fingerprints already configured for
 * `assetlinks.json` (colon-separated hex) rather than duplicating them in a second env var, converting each to
 * the Base64Url form this origin format expects.
 *
 * A browser-based passkey login (not yet implemented) would add `Origin.create(AppLinks.baseUrl)` (a plain
 * HTTPS origin) here too.
 */
val webAuthnAndroidOrigins: Set<Origin> get() = WellKnownConfigProvider.sha256CertFingerprints.map { fingerprint ->
    val bytes = fingerprint.replace(":", "").hexToByteArray()
    val base64Url = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    Origin.create("android:apk-key-hash:$base64Url")
}.toSet()

private val secureRandom = SecureRandom()

fun generateWebAuthnChallenge(): DefaultChallenge {
    // Generate 32 random bytes
    val randomBytes = ByteArray(32)
    secureRandom.nextBytes(randomBytes)

    // Wrap it in WebAuthn4J's DefaultChallenge
    return DefaultChallenge(randomBytes)
}
