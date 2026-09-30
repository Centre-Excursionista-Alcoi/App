package org.centrexcursionistalcoi.app.security

import io.ktor.client.HttpClient
import io.ktor.client.plugins.resources.delete
import io.ktor.client.plugins.resources.get
import io.ktor.client.plugins.resources.post
import io.ktor.client.plugins.resources.put
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.parameters
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.centrexcursionistalcoi.app.AppLinks
import org.centrexcursionistalcoi.app.ApplicationTestBase
import org.centrexcursionistalcoi.app.assertBody
import org.centrexcursionistalcoi.app.assertError
import org.centrexcursionistalcoi.app.assertStatusCode
import org.centrexcursionistalcoi.app.data.AddPasskeyRequest
import org.centrexcursionistalcoi.app.data.PasskeyRegistrationOptionsRequest
import org.centrexcursionistalcoi.app.data.PasskeyRegistrationRequest
import org.centrexcursionistalcoi.app.data.Reauthentication
import org.centrexcursionistalcoi.app.data.ReauthenticatedRequest
import org.centrexcursionistalcoi.app.data.RegisterRestoreKeyRequest
import org.centrexcursionistalcoi.app.data.RegistrationCodeRequest
import org.centrexcursionistalcoi.app.data.RestoreKeyVerificationRequest
import org.centrexcursionistalcoi.app.data.SecurityInfo
import org.centrexcursionistalcoi.app.data.SetPasswordRequest
import org.centrexcursionistalcoi.app.data.TokenResponse
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.UserCredentialRecordEntity
import org.centrexcursionistalcoi.app.database.entity.UserReferenceEntity
import org.centrexcursionistalcoi.app.database.table.CredentialKind
import org.centrexcursionistalcoi.app.error.Error
import org.centrexcursionistalcoi.app.href
import org.centrexcursionistalcoi.app.routes.Api
import org.centrexcursionistalcoi.app.routes.WellKnownConfigProvider
import org.centrexcursionistalcoi.app.test.FakeUser
import org.centrexcursionistalcoi.app.test.LoginType
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TestPasskeyRoutes : ApplicationTestBase() {
    /** The origin iOS presents for the app. */
    private val iosOrigin = "https://${AppLinks.host}"

    private val password = "TestPassword123"

    @AfterTest
    fun tearDown() {
        WellKnownConfigProvider.override(WellKnownConfigProvider.SHA256_CERT_FINGERPRINTS_VARIABLE, null)
    }

    private suspend fun HttpResponse.challenge(): String =
        Json.parseToJsonElement(bodyAsText()).jsonObject["challenge"]!!.jsonPrimitive.content

    private suspend fun HttpClient.authChallenge(): String =
        post(Api.GenerateAuthChallenge()).also { it.assertStatusCode(HttpStatusCode.OK) }.challenge()

    /** Adds a passkey to the logged-in account. */
    private suspend fun HttpClient.addPasskey(authenticator: SoftwareAuthenticator, userVerified: Boolean = true): HttpResponse {
        val challenge = post(Api.Profile.Passkeys.Options()).also { it.assertStatusCode(HttpStatusCode.OK) }.challenge()
        return post(Api.Profile.Passkeys()) {
            contentType(ContentType.Application.Json)
            setBody(AddPasskeyRequest(authenticator.registrationResponseJson(challenge, iosOrigin, userVerified), "iPhone"))
        }
    }

    private suspend fun HttpClient.signIn(authenticator: SoftwareAuthenticator, userVerified: Boolean = true): HttpResponse {
        val challenge = authChallenge()
        return post(Api.Auth.WebAuthnVerify()) {
            contentType(ContentType.Application.Json)
            setBody(RestoreKeyVerificationRequest(authenticator.authenticationResponseJson(challenge, iosOrigin, userVerified)))
        }
    }

    private suspend fun HttpClient.passkeyReauthentication(authenticator: SoftwareAuthenticator) =
        Reauthentication(authenticationResponseJson = authenticator.authenticationResponseJson(authChallenge(), iosOrigin, true))

    private suspend fun HttpClient.security(): SecurityInfo {
        lateinit var info: SecurityInfo
        get(Api.Profile.Security()).assertBody(SecurityInfo.serializer()) { info = it }
        return info
    }

    // Registration

    @Test
    fun test_registrationCode_isSentToAMember() = runApplicationTest(
        databaseInitBlock = { FakeUser.provideMemberEntity() },
    ) {
        client.post(Api.Register.Verification()) {
            contentType(ContentType.Application.Json)
            setBody(RegistrationCodeRequest(FakeUser.EMAIL))
        }.assertStatusCode(HttpStatusCode.Accepted)
    }

    @Test
    fun test_registrationCode_isNotSentToANonMember() = runApplicationTest {
        client.post(Api.Register.Verification()) {
            contentType(ContentType.Application.Json)
            setBody(RegistrationCodeRequest(FakeUser.EMAIL))
        }.assertError(Error.EmailNotFound())
    }

    @Test
    fun test_registrationCode_stopsWorkingAfterTooManyWrongGuesses() = runApplicationTest {
        val email = FakeUser.EMAIL.uppercase()
        val code = RegistrationCodes.create(email)
        val wrong = if (code == "000000") "000001" else "000000"
        repeat(5) { assertFalse(RegistrationCodes.check(email, wrong)) }
        assertFalse(RegistrationCodes.check(email, code))
    }

    @Test
    fun test_registerWithPasskey_createsAPasswordlessAccount_andLogsItIn() = runApplicationTest(
        databaseInitBlock = { FakeUser.provideMemberEntity() },
    ) {
        val code = RegistrationCodes.create(FakeUser.EMAIL.uppercase())
        val authenticator = SoftwareAuthenticator()

        val challenge = client.post(Api.Register.Passkey.Options()) {
            contentType(ContentType.Application.Json)
            setBody(PasskeyRegistrationOptionsRequest(FakeUser.EMAIL, code))
        }.also { response ->
            response.assertStatusCode(HttpStatusCode.OK)
            val options = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            assertEquals("required", options["authenticatorSelection"]!!.jsonObject["userVerification"]!!.jsonPrimitive.content)
        }.challenge()

        client.post(Api.Register.Passkey()) {
            contentType(ContentType.Application.Json)
            setBody(PasskeyRegistrationRequest(FakeUser.EMAIL, code, authenticator.registrationResponseJson(challenge, iosOrigin, true), "iPhone"))
        }.assertBody(TokenResponse.serializer()) { assertEquals(FakeUser.EMAIL.uppercase(), it.accountEmail?.uppercase()) }

        val user = assertNotNull(Database { UserReferenceEntity.findByEmail(FakeUser.EMAIL.uppercase()) })
        assertFalse(Database { user.hasPassword })
        val credential = assertNotNull(Database { UserCredentialRecordEntity.findById(authenticator.credentialId) })
        assertEquals(CredentialKind.PASSKEY, Database { credential.kind })

        // And the passkey signs in
        client.signIn(authenticator).assertStatusCode(HttpStatusCode.OK)
        // The code has been used up
        assertFalse(RegistrationCodes.check(FakeUser.EMAIL.uppercase(), code))
    }

    @Test
    fun test_registerWithPasskey_withAWrongCode() = runApplicationTest(
        databaseInitBlock = { FakeUser.provideMemberEntity() },
    ) {
        val code = RegistrationCodes.create(FakeUser.EMAIL.uppercase())
        val wrong = if (code == "000000") "000001" else "000000"
        client.post(Api.Register.Passkey.Options()) {
            contentType(ContentType.Application.Json)
            setBody(PasskeyRegistrationOptionsRequest(FakeUser.EMAIL, wrong))
        }.assertError(Error.InvalidVerificationCode())
    }

    @Test
    fun test_registerWithPasskey_withoutUserVerification_isRejected() = runApplicationTest(
        databaseInitBlock = { FakeUser.provideMemberEntity() },
    ) {
        val code = RegistrationCodes.create(FakeUser.EMAIL.uppercase())
        val authenticator = SoftwareAuthenticator()
        val challenge = client.post(Api.Register.Passkey.Options()) {
            contentType(ContentType.Application.Json)
            setBody(PasskeyRegistrationOptionsRequest(FakeUser.EMAIL, code))
        }.challenge()

        client.post(Api.Register.Passkey()) {
            contentType(ContentType.Application.Json)
            setBody(PasskeyRegistrationRequest(FakeUser.EMAIL, code, authenticator.registrationResponseJson(challenge, iosOrigin, false)))
        }.assertStatusCode(HttpStatusCode.BadRequest)
        assertNull(Database { UserReferenceEntity.findByEmail(FakeUser.EMAIL.uppercase()) })
    }

    // Signing in

    @Test
    fun test_passkeySignIn_requiresUserVerification() = runApplicationTest(shouldLogIn = LoginType.USER) {
        val authenticator = SoftwareAuthenticator()
        client.addPasskey(authenticator).assertStatusCode(HttpStatusCode.Created)
        logout()

        client.signIn(authenticator, userVerified = false).assertError(Error.IncorrectPasswordOrEmail())
        client.signIn(authenticator, userVerified = true).assertStatusCode(HttpStatusCode.OK)
    }

    @Test
    fun test_addPasskey_withoutUserVerification_isRejected() = runApplicationTest(shouldLogIn = LoginType.USER) {
        client.addPasskey(SoftwareAuthenticator(), userVerified = false).assertStatusCode(HttpStatusCode.BadRequest)
    }

    // Security settings

    @Test
    fun test_security_listsPasskeys_butNotRestoreKeys() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        userEntityPatches = { it.password = Passwords.hash(password.toCharArray()) },
    ) {
        WellKnownConfigProvider.override(WellKnownConfigProvider.SHA256_CERT_FINGERPRINTS_VARIABLE, "AA:BB")
        val restoreChallenge = client.post(Api.GenerateRestoreChallenge()).challenge()
        client.post(Api.RegisterRestoreKey()) {
            contentType(ContentType.Application.Json)
            setBody(RegisterRestoreKeyRequest(SoftwareAuthenticator().registrationResponseJson(restoreChallenge, "android:apk-key-hash:qrs", false)))
        }.assertStatusCode(HttpStatusCode.OK)

        val passkey = SoftwareAuthenticator()
        client.addPasskey(passkey).assertStatusCode(HttpStatusCode.Created)

        val info = client.security()
        assertTrue(info.hasPassword)
        assertEquals(listOf(passkey.credentialId), info.passkeys.map { it.id })
        assertEquals("iPhone", info.passkeys.single().name)
    }

    @Test
    fun test_removePasskey_requiresReauthentication() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        userEntityPatches = { it.password = Passwords.hash(password.toCharArray()) },
    ) {
        val passkey = SoftwareAuthenticator()
        client.addPasskey(passkey).assertStatusCode(HttpStatusCode.Created)

        client.delete(Api.Profile.Passkeys.Id(passkey.credentialId)).assertError(Error.ReauthenticationFailed())
        client.delete(Api.Profile.Passkeys.Id(passkey.credentialId)) {
            contentType(ContentType.Application.Json)
            setBody(ReauthenticatedRequest(Reauthentication(password = "WrongPassword1")))
        }.assertError(Error.ReauthenticationFailed())

        client.delete(Api.Profile.Passkeys.Id(passkey.credentialId)) {
            contentType(ContentType.Application.Json)
            setBody(ReauthenticatedRequest(Reauthentication(password = password)))
        }.assertStatusCode(HttpStatusCode.NoContent)
        assertTrue(client.security().passkeys.isEmpty())
    }

    @Test
    fun test_theLastWayToSignIn_cantBeRemoved() = runApplicationTest(shouldLogIn = LoginType.USER) {
        // FakeUser has no password
        val passkey = SoftwareAuthenticator()
        client.addPasskey(passkey).assertStatusCode(HttpStatusCode.Created)

        client.delete(Api.Profile.Passkeys.Id(passkey.credentialId)) {
            contentType(ContentType.Application.Json)
            setBody(ReauthenticatedRequest(client.passkeyReauthentication(passkey)))
        }.assertError(Error.LastLoginMethod())
        assertEquals(1, client.security().passkeys.size)
    }

    @Test
    fun test_password_cantBeRemoved_withoutAPasskey() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        userEntityPatches = { it.password = Passwords.hash(password.toCharArray()) },
    ) {
        client.delete(Api.Profile.Password()) {
            contentType(ContentType.Application.Json)
            setBody(ReauthenticatedRequest(Reauthentication(password = password)))
        }.assertError(Error.LastLoginMethod())
        assertTrue(client.security().hasPassword)
    }

    @Test
    fun test_password_canBeRemoved_andSetAgain_withAPasskey() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        userEntityPatches = { it.password = Passwords.hash(password.toCharArray()) },
    ) {
        val passkey = SoftwareAuthenticator()
        client.addPasskey(passkey).assertStatusCode(HttpStatusCode.Created)

        client.delete(Api.Profile.Password()) {
            contentType(ContentType.Application.Json)
            setBody(ReauthenticatedRequest(Reauthentication(password = password)))
        }.assertStatusCode(HttpStatusCode.NoContent)
        assertFalse(client.security().hasPassword)

        // Without a password, only the passkey can confirm it's them
        client.put(Api.Profile.Password()) {
            contentType(ContentType.Application.Json)
            setBody(SetPasswordRequest(Reauthentication(password = password), "NewPassword123"))
        }.assertError(Error.ReauthenticationFailed())
        client.put(Api.Profile.Password()) {
            contentType(ContentType.Application.Json)
            setBody(SetPasswordRequest(client.passkeyReauthentication(passkey), "NewPassword123"))
        }.assertStatusCode(HttpStatusCode.NoContent)
        assertTrue(client.security().hasPassword)

        logout()
        client.submitForm(
            href(Api.Auth.Login()),
            parameters {
                append("email", FakeUser.EMAIL)
                append("password", "NewPassword123")
            },
        ).assertStatusCode(HttpStatusCode.OK)
    }

    @Test
    fun test_anotherUsersPasskey_doesntReauthenticate() = runApplicationTest(shouldLogIn = LoginType.USER) {
        val mine = SoftwareAuthenticator()
        client.addPasskey(mine).assertStatusCode(HttpStatusCode.Created)
        // A passkey the server doesn't know as FakeUser's
        val stranger = SoftwareAuthenticator()

        client.put(Api.Profile.Password()) {
            contentType(ContentType.Application.Json)
            setBody(SetPasswordRequest(client.passkeyReauthentication(stranger), "NewPassword123"))
        }.assertError(Error.ReauthenticationFailed())
        assertFalse(client.security().hasPassword)
    }
}
