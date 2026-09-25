package org.centrexcursionistalcoi.app.security

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
import org.centrexcursionistalcoi.app.error.Error
import org.centrexcursionistalcoi.app.test.LoginType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
 * A full register -> verify round trip (a real software WebAuthn authenticator producing a valid signed
 * response) is *not* covered here -- constructing one needs `webauthn4j-test`'s `ClientPlatform`/authenticator
 * fixtures wired up to this exact request/response shape, which is a substantial addition of its own. The
 * "malformed credential response" tests below at least confirm the verify/register endpoints fail cleanly
 * (a structured [Error], not a raw string or a crash) rather than actually verifying the crypto succeeds.
 */
class TestWebAuthnRoutes : ApplicationTestBase() {

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
