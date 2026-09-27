package org.centrexcursionistalcoi.app.security

import com.webauthn4j.converter.AttestationObjectConverter
import com.webauthn4j.converter.CollectedClientDataConverter
import com.webauthn4j.converter.util.ObjectConverter
import com.webauthn4j.data.attestation.AttestationObject
import com.webauthn4j.data.attestation.authenticator.AAGUID
import com.webauthn4j.data.attestation.authenticator.AttestedCredentialData
import com.webauthn4j.data.attestation.authenticator.AuthenticatorData
import com.webauthn4j.data.attestation.authenticator.EC2COSEKey
import com.webauthn4j.data.attestation.statement.COSEAlgorithmIdentifier
import com.webauthn4j.data.attestation.statement.NoneAttestationStatement
import com.webauthn4j.data.client.ClientDataType
import com.webauthn4j.data.client.CollectedClientData
import com.webauthn4j.data.client.Origin
import com.webauthn4j.data.client.challenge.DefaultChallenge
import com.webauthn4j.data.extension.authenticator.RegistrationExtensionAuthenticatorOutput
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.centrexcursionistalcoi.app.AppLinks
import org.centrexcursionistalcoi.app.ApplicationTestBase
import org.centrexcursionistalcoi.app.assertError
import org.centrexcursionistalcoi.app.assertStatusCode
import org.centrexcursionistalcoi.app.data.RegisterRestoreKeyRequest
import org.centrexcursionistalcoi.app.data.RestoreKeyVerificationRequest
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.UserCredentialRecordEntity
import org.centrexcursionistalcoi.app.error.Error
import org.centrexcursionistalcoi.app.routes.WellKnownConfigProvider
import org.centrexcursionistalcoi.app.test.LoginType
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.util.Base64
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Regression coverage for the two shape bugs fixed while completing this WIP implementation -- neither needs a
 * real WebAuthn ceremony (a software authenticator round-trip) to catch:
 * - `rp.id`/`rpId` must be the domain this app is associated with ([AppLinks.host]), never the Android package
 *   name -- Android's Credential Manager validates it against the app's `.well-known/assetlinks.json`
 *   association, and a package name isn't a domain.
 * - `/generate-auth-challenge` must return the "get" (authentication) options shape (`AuthenticationOptionsResponse`:
 *   `challenge`/`rpId`), not the "create" (registration) shape with a placeholder dummy user -- Android's
 *   `GetRestoreCredentialOption` expects a `PublicKeyCredentialRequestOptionsJSON`, not a creation-options JSON.
 *
 * Registration is covered with a hand-built `none` attestation (see [restoreKeyRegistrationResponseJson]), but
 * redeeming a key is not: that needs a response signed by the registered key, which is a substantial addition of
 * its own. The "malformed credential response" test below at least confirms the verify endpoint fails cleanly
 * (a structured [Error], not a raw string or a crash).
 */
class TestWebAuthnRoutes : ApplicationTestBase() {
    @AfterTest
    fun tearDown() {
        WellKnownConfigProvider.override(WellKnownConfigProvider.SHA256_CERT_FINGERPRINTS_VARIABLE, null)
    }

    private val base64Url = Base64.getUrlEncoder().withoutPadding()

    /**
     * A registration response as an authenticator creating a Restore Credential produces it: `none` attestation,
     * and only the user present flag set -- no user verification, since restore keys are created silently.
     */
    private fun restoreKeyRegistrationResponseJson(challenge: String, origin: String): String {
        val objectConverter = ObjectConverter()
        val keyPair = KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp256r1")) }.generateKeyPair()
        val credentialId = ByteArray(16).also { SecureRandom().nextBytes(it) }

        val authenticatorData = AuthenticatorData<RegistrationExtensionAuthenticatorOutput>(
            MessageDigest.getInstance("SHA-256").digest(AppLinks.host.toByteArray()),
            (AuthenticatorData.BIT_UP.toInt() or AuthenticatorData.BIT_AT.toInt()).toByte(),
            0,
            AttestedCredentialData(
                AAGUID.ZERO,
                credentialId,
                EC2COSEKey.create(keyPair.public as ECPublicKey, COSEAlgorithmIdentifier.ES256),
            ),
        )
        val attestationObject = AttestationObjectConverter(objectConverter)
            .convertToBytes(AttestationObject(authenticatorData, NoneAttestationStatement()))
        val clientData = CollectedClientDataConverter(objectConverter).convertToBytes(
            CollectedClientData(
                ClientDataType.WEBAUTHN_CREATE,
                DefaultChallenge(Base64.getUrlDecoder().decode(challenge)),
                Origin.create(origin),
                null,
            )
        )

        val id = base64Url.encodeToString(credentialId)
        return """{"id":"$id","rawId":"$id","type":"public-key","response":{"clientDataJSON":"${base64Url.encodeToString(clientData)}","attestationObject":"${base64Url.encodeToString(attestationObject)}"},"clientExtensionResults":{}}"""
    }

    @Test
    fun test_registerRestoreKey_withoutUserVerification_isAccepted() = runApplicationTest(
        shouldLogIn = LoginType.USER,
    ) {
        WellKnownConfigProvider.override(WellKnownConfigProvider.SHA256_CERT_FINGERPRINTS_VARIABLE, "AA:BB")

        val challengeResponse = client.post("/generate-restore-challenge")
        challengeResponse.assertStatusCode(HttpStatusCode.OK)
        val challenge = Json.parseToJsonElement(challengeResponse.bodyAsText()).jsonObject["challenge"]!!.jsonPrimitive.content

        val registrationResponseJson = restoreKeyRegistrationResponseJson(challenge, origin = "android:apk-key-hash:qrs")
        client.post("/register-restore-key") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRestoreKeyRequest(registrationResponseJson))
        }.assertStatusCode(HttpStatusCode.OK)

        val credentialId = Json.parseToJsonElement(registrationResponseJson).jsonObject["id"]!!.jsonPrimitive.content
        assertNotNull(Database { UserCredentialRecordEntity.findById(credentialId) })
    }

    @Test
    fun test_generateRestoreChallenge_requiresLogin() = runApplicationTest {
        client.post("/generate-restore-challenge").assertError(Error.NotLoggedIn())
    }

    @Test
    fun test_generateRestoreChallenge_returnsTheRealDomainAsRpId_notThePackageName() = runApplicationTest(
        shouldLogIn = LoginType.USER,
    ) {
        val response = client.post("/generate-restore-challenge")
        response.assertStatusCode(HttpStatusCode.OK)

        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        val rpId = body["rp"]!!.jsonObject["id"]!!.jsonPrimitive.content

        assertEquals(AppLinks.host, rpId)
        assertFalse(rpId == AppLinks.ANDROID_PACKAGE_NAME, "rp.id must be a domain, not the Android package name: $rpId")
        assertTrue(rpId.contains("."), "rp.id must look like a domain: $rpId")
    }

    @Test
    fun test_generateAuthChallenge_doesNotRequireLogin() = runApplicationTest {
        client.post("/generate-auth-challenge").assertStatusCode(HttpStatusCode.OK)
    }

    @Test
    fun test_generateAuthChallenge_returnsTheAuthenticationOptionsShape_notARegistrationShapeWithADummyUser() = runApplicationTest {
        val response = client.post("/generate-auth-challenge")
        response.assertStatusCode(HttpStatusCode.OK)

        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        // The "get" options shape: a bare rpId, no `rp`/`user`/`pubKeyCredParams` (those are registration-only).
        assertTrue(body.containsKey("challenge"))
        assertTrue(body.containsKey("rpId"))
        assertEquals(AppLinks.host, body["rpId"]!!.jsonPrimitive.content)
        assertFalse(body.containsKey("user"), "the auth-challenge response must not carry a (dummy) user: $body")
        assertFalse(body.containsKey("rp"), "the auth-challenge response must not carry the registration rp object: $body")
        assertFalse(body.containsKey("pubKeyCredParams"))
    }

    @Test
    fun test_registerRestoreKey_requiresLogin() = runApplicationTest {
        client.post("/register-restore-key") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRestoreKeyRequest("{}"))
        }.assertError(Error.NotLoggedIn())
    }

    @Test
    fun test_registerRestoreKey_withoutARequestedChallenge_respondsWithAStructuredError() = runApplicationTest(
        shouldLogIn = LoginType.USER,
    ) {
        // No prior call to /generate-restore-challenge -- nothing in Redis for this user to match against.
        val response = client.post("/register-restore-key") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRestoreKeyRequest("{}"))
        }

        response.assertError(Error.InvalidArgument("challenge"))
    }

    @Test
    fun test_verifyRestoreKey_withAMalformedCredentialResponse_respondsWithAStructuredErrorNotARawString() = runApplicationTest {
        val response = client.post("/auth/webauthn/verify") {
            contentType(ContentType.Application.Json)
            setBody(RestoreKeyVerificationRequest("not a real WebAuthn response"))
        }

        // Whatever the exact status, the body must be the app's structured Error JSON (parseable, with a code),
        // not a bare string -- the client's error handling only understands the former (see ServerException).
        val body = response.bodyAsText()
        val json = Json.parseToJsonElement(body).jsonObject
        assertTrue(json.containsKey("code"), "expected a structured Error body, got: $body")
    }
}
