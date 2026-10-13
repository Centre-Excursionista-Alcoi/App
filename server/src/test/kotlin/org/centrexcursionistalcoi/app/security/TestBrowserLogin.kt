package org.centrexcursionistalcoi.app.security

import io.ktor.client.HttpClient
import io.ktor.client.plugins.resources.post
import io.ktor.client.plugins.sse.sse
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.centrexcursionistalcoi.app.AppLinks
import org.centrexcursionistalcoi.app.ApplicationTestBase
import org.centrexcursionistalcoi.app.assertBody
import org.centrexcursionistalcoi.app.assertError
import org.centrexcursionistalcoi.app.assertStatusCode
import org.centrexcursionistalcoi.app.data.AddPasskeyRequest
import org.centrexcursionistalcoi.app.data.BROWSER_LOGIN_APPROVED_EVENT
import org.centrexcursionistalcoi.app.data.BROWSER_LOGIN_SECRET_HEADER
import org.centrexcursionistalcoi.app.data.BrowserLoginRequest
import org.centrexcursionistalcoi.app.data.BrowserLoginStartRequest
import org.centrexcursionistalcoi.app.data.RestoreKeyVerificationRequest
import org.centrexcursionistalcoi.app.data.TokenResponse
import org.centrexcursionistalcoi.app.error.Error
import org.centrexcursionistalcoi.app.routes.Api
import org.centrexcursionistalcoi.app.test.FakeUser
import org.centrexcursionistalcoi.app.test.LoginType
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class TestBrowserLogin : ApplicationTestBase() {
    private val origin = "https://${AppLinks.host}"

    private suspend fun HttpClient.start(): BrowserLoginRequest {
        lateinit var request: BrowserLoginRequest
        post(Api.Auth.Browser()) {
            contentType(ContentType.Application.Json)
            setBody(BrowserLoginStartRequest("Linux"))
        }.assertBody(BrowserLoginRequest.serializer()) { request = it }
        return request
    }

    private suspend fun HttpClient.signInInBrowser(id: String, authenticator: SoftwareAuthenticator): HttpResponse {
        val challenge = post(Api.GenerateAuthChallenge()).let {
            Json.parseToJsonElement(it.bodyAsText()).jsonObject["challenge"]!!.jsonPrimitive.content
        }
        return post(Api.Auth.Browser.Complete(id)) {
            contentType(ContentType.Application.Json)
            setBody(RestoreKeyVerificationRequest(authenticator.authenticationResponseJson(challenge, origin, true)))
        }
    }

    private suspend fun HttpClient.redeem(request: BrowserLoginRequest, secret: String? = request.secret) =
        post(Api.Auth.Browser.Redeem(request.id)) { secret?.let { header(BROWSER_LOGIN_SECRET_HEADER, it) } }

    private suspend fun HttpClient.userWithPasskey(): SoftwareAuthenticator {
        val authenticator = SoftwareAuthenticator()
        val challenge = post(Api.Profile.Passkeys.Options()).let {
            Json.parseToJsonElement(it.bodyAsText()).jsonObject["challenge"]!!.jsonPrimitive.content
        }
        post(Api.Profile.Passkeys()) {
            contentType(ContentType.Application.Json)
            setBody(AddPasskeyRequest(authenticator.registrationResponseJson(challenge, origin, true), "Phone"))
        }.assertStatusCode(HttpStatusCode.Created)
        return authenticator
    }

    @Test
    fun test_page_showsTheCode_andExpiredForUnknownRequests() = runApplicationTest {
        val request = client.start()

        val page = client.get("/browser-login?id=${request.id}")
        page.assertStatusCode(HttpStatusCode.OK)
        assertContains(page.bodyAsText(), request.code)

        // What a browser without JavaScript or passkeys needs: a hidden button, its explanations, and a monospace code
        val html = page.bodyAsText()
        assertContains(html, "<noscript>")
        assertContains(html, """id="unsupported"""")
        assertContains(html, """<code class="code">${request.code}</code>""")
        assertContains(html, """<button type="button" class="cta" id="continue" hidden="">""")

        client.get("/browser-login?id=unknown").assertStatusCode(HttpStatusCode.NotFound)
        client.get("/browser-login").assertStatusCode(HttpStatusCode.NotFound)
    }

    @Test
    fun test_signInInTheBrowser_givesTheAppItsTokens_once() = runApplicationTest(shouldLogIn = LoginType.USER) {
        val authenticator = client.userWithPasskey()
        logout()

        val request = client.start()
        assertEquals(request.url, "${AppLinks.baseUrl}/browser-login?id=${request.id}")

        // Nothing to redeem until the user signs in
        client.redeem(request).assertError(Error.EntityNotFound("BrowserLogin", request.id))

        client.signInInBrowser(request.id, authenticator).assertStatusCode(HttpStatusCode.NoContent)

        // Only whoever started it gets the tokens
        client.redeem(request, secret = null).assertError(Error.EntityNotFound("BrowserLogin", request.id))
        client.redeem(request, secret = "wrong").assertError(Error.EntityNotFound("BrowserLogin", request.id))

        client.redeem(request).assertBody(TokenResponse.serializer()) {
            assertEquals(FakeUser.EMAIL.uppercase(), it.accountEmail.uppercase())
            assertNotEquals("", it.refreshToken)
        }
        client.redeem(request).assertError(Error.EntityNotFound("BrowserLogin", request.id))
    }

    @Test
    fun test_signInInTheBrowser_canOnlyBeDoneOnce() = runApplicationTest(shouldLogIn = LoginType.USER) {
        val authenticator = client.userWithPasskey()
        logout()

        val request = client.start()
        client.signInInBrowser(request.id, authenticator).assertStatusCode(HttpStatusCode.NoContent)
        client.signInInBrowser(request.id, authenticator).assertError(Error.EntityNotFound("BrowserLogin", request.id))
    }

    @Test
    fun test_signInInTheBrowser_withAnUnknownRequest() = runApplicationTest(shouldLogIn = LoginType.USER) {
        val authenticator = client.userWithPasskey()
        logout()

        client.signInInBrowser("unknown", authenticator).assertError(Error.EntityNotFound("BrowserLogin", "unknown"))
    }

    @Test
    fun test_signInInTheBrowser_withoutUserVerification_isRejected() = runApplicationTest(shouldLogIn = LoginType.USER) {
        val authenticator = client.userWithPasskey()
        logout()

        val request = client.start()
        val challenge = client.post(Api.GenerateAuthChallenge()).let {
            Json.parseToJsonElement(it.bodyAsText()).jsonObject["challenge"]!!.jsonPrimitive.content
        }
        client.post(Api.Auth.Browser.Complete(request.id)) {
            contentType(ContentType.Application.Json)
            setBody(RestoreKeyVerificationRequest(authenticator.authenticationResponseJson(challenge, origin, false)))
        }.assertError(Error.IncorrectPasswordOrEmail())
        client.redeem(request).assertError(Error.EntityNotFound("BrowserLogin", request.id))
    }

    @Test
    fun test_events_tellTheAppAboutTheApproval() = runApplicationTest(shouldLogIn = LoginType.USER) {
        val authenticator = client.userWithPasskey()
        logout()

        val request = client.start()
        client.signInInBrowser(request.id, authenticator).assertStatusCode(HttpStatusCode.NoContent)

        var event: String? = null
        client.sse("/auth/browser/${request.id}/events", request = { header(BROWSER_LOGIN_SECRET_HEADER, request.secret) }) {
            event = incoming.first { it.event != "connection" }.event
        }
        assertEquals(BROWSER_LOGIN_APPROVED_EVENT, event)
    }
}
