package org.centrexcursionistalcoi.app.plugins

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.basicAuth
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.parameters
import io.ktor.http.renderCookieHeader
import io.ktor.http.setCookie
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import org.centrexcursionistalcoi.app.ADMIN_GROUP_NAME
import org.centrexcursionistalcoi.app.ApplicationTestBase
import org.centrexcursionistalcoi.app.assertError
import org.centrexcursionistalcoi.app.assertStatusCode
import org.centrexcursionistalcoi.app.data.RefreshTokenRequest
import org.centrexcursionistalcoi.app.data.TokenResponse
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.UserReferenceEntity
import org.centrexcursionistalcoi.app.database.table.AuthRefreshTokens
import org.centrexcursionistalcoi.app.database.table.AuthSessionRevocationReason
import org.centrexcursionistalcoi.app.database.table.AuthSessions
import org.centrexcursionistalcoi.app.database.table.RecoverPasswordRequests
import org.centrexcursionistalcoi.app.error.Error
import org.centrexcursionistalcoi.app.mockTime
import org.centrexcursionistalcoi.app.module
import org.centrexcursionistalcoi.app.security.AuthTokens
import org.centrexcursionistalcoi.app.security.Passwords
import org.centrexcursionistalcoi.app.security.SessionsKeys
import org.centrexcursionistalcoi.app.security.WebDavSession
import org.centrexcursionistalcoi.app.test.FakeAdminUser
import org.centrexcursionistalcoi.app.test.FakeUser
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class TestAuthTokens : ApplicationTestBase() {
    private val password = "TestPassword123"

    private val startTime: Instant = Instant.parse("2026-01-01T10:00:00Z")

    private fun runTokenTest(block: suspend ApplicationTestBuilder.() -> Unit) = runApplicationTest(
        mockNow = startTime,
        databaseInitBlock = {
            FakeUser.provideEntity().password = Passwords.hash(password.toCharArray())
        },
    ) { block() }

    private suspend fun HttpClient.login(): TokenResponse = submitForm(
        "/auth/login",
        parameters {
            append("email", FakeUser.EMAIL)
            append("password", password)
        },
    ).apply { assertStatusCode(HttpStatusCode.OK) }.body()

    private suspend fun HttpClient.refresh(refreshToken: String): HttpResponse = post("/auth/refresh") {
        contentType(ContentType.Application.Json)
        setBody(RefreshTokenRequest(refreshToken))
    }

    private suspend fun HttpClient.profile(accessToken: String): HttpResponse = get("/profile") {
        bearerAuth(accessToken)
    }

    @Test
    fun test_login_issuesTokensThatAuthenticate() = runTokenTest {
        val response = client.submitForm(
            "/auth/login",
            parameters {
                append("email", FakeUser.EMAIL)
                append("password", password)
            },
        )
        response.assertStatusCode(HttpStatusCode.OK)
        assertEquals("no-store", response.headers[HttpHeaders.CacheControl])
        val tokens = response.body<TokenResponse>()
        assertEquals("Bearer", tokens.tokenType)
        assertEquals(AuthTokens.accessTokenLifetime.inWholeSeconds, tokens.accessTokenExpiresIn)
        assertEquals(FakeUser.EMAIL, tokens.accountEmail)

        client.profile(tokens.accessToken).apply {
            assertStatusCode(HttpStatusCode.OK)
            assertEquals("true", headers["CEA-LoggedIn"])
        }
    }

    @Test
    fun test_login_wrongPassword() = runTokenTest {
        client.submitForm(
            "/auth/login",
            parameters {
                append("email", FakeUser.EMAIL)
                append("password", "WrongPassword123")
            },
        ).assertError(Error.IncorrectPasswordOrEmail())
    }

    @Test
    fun test_invalidBearer_isRejected() = runTokenTest {
        client.get("/profile") { bearerAuth("not-a-token") }.apply {
            assertError(Error.NotLoggedIn())
            assertEquals("Bearer error=\"invalid_token\"", headers[HttpHeaders.WWWAuthenticate])
        }
    }

    @Test
    fun test_accessToken_expires() = runTokenTest {
        val tokens = client.login()
        mockTime(startTime + (AuthTokens.accessTokenLifetime + 1.minutes).toJavaDuration())
        client.profile(tokens.accessToken).assertError(Error.NotLoggedIn())
    }

    @Test
    fun test_refresh_rotatesTokens() = runTokenTest {
        val first = client.login()
        mockTime(startTime + 5.minutes.toJavaDuration())

        val second = client.refresh(first.refreshToken).apply { assertStatusCode(HttpStatusCode.OK) }.body<TokenResponse>()
        assertNotEquals(first.refreshToken, second.refreshToken)
        assertNotEquals(first.accessToken, second.accessToken)
        client.profile(second.accessToken).assertStatusCode(HttpStatusCode.OK)

        // The new refresh token can be used in turn.
        client.refresh(second.refreshToken).assertStatusCode(HttpStatusCode.OK)
    }

    @Test
    fun test_refresh_reuseOutsideGracePeriod_revokesTheSession() = runTokenTest {
        val first = client.login()
        val second = client.refresh(first.refreshToken).body<TokenResponse>()

        mockTime(startTime + (AuthTokens.refreshTokenReuseGracePeriod + 1.seconds).toJavaDuration())
        client.refresh(first.refreshToken).assertError(Error.NotLoggedIn())

        // Every token of the session is now useless, including the legitimate ones.
        client.refresh(second.refreshToken).assertError(Error.NotLoggedIn())
        client.profile(second.accessToken).assertError(Error.NotLoggedIn())
        assertEquals(AuthSessionRevocationReason.REFRESH_TOKEN_REUSE, Database {
            AuthSessions.selectAll().single()[AuthSessions.revocationReason]
        })
    }

    @Test
    fun test_refresh_retryWithinGracePeriod_replacesTheUnusedSuccessor() = runTokenTest {
        val first = client.login()
        // The client never got this response.
        val lost = client.refresh(first.refreshToken).body<TokenResponse>()

        mockTime(startTime + (AuthTokens.refreshTokenReuseGracePeriod - 1.seconds).toJavaDuration())
        val retried = client.refresh(first.refreshToken).apply { assertStatusCode(HttpStatusCode.OK) }.body<TokenResponse>()

        client.refresh(retried.refreshToken).assertStatusCode(HttpStatusCode.OK)
        // The lost successor was replaced, so it can't be used anymore.
        client.refresh(lost.refreshToken).assertError(Error.NotLoggedIn())
    }

    @Test
    fun test_refresh_reuseAfterSuccessorWasUsed_revokesTheSession() = runTokenTest {
        val first = client.login()
        val second = client.refresh(first.refreshToken).body<TokenResponse>()
        val third = client.refresh(second.refreshToken).body<TokenResponse>()

        // Still within the grace period, but its successor has been used: this can only be a stolen copy.
        client.refresh(first.refreshToken).assertError(Error.NotLoggedIn())
        client.refresh(third.refreshToken).assertError(Error.NotLoggedIn())
    }

    @Test
    fun test_refresh_unknownToken() = runTokenTest {
        client.refresh("cea_rt_unknown").assertError(Error.NotLoggedIn())
    }

    @Test
    fun test_refresh_expiresAfterIdleLifetime() = runTokenTest {
        val tokens = client.login()
        mockTime(startTime + (AuthTokens.refreshTokenIdleLifetime + 1.minutes).toJavaDuration())
        client.refresh(tokens.refreshToken).assertError(Error.NotLoggedIn())
    }

    @Test
    fun test_logout_revokesTheSession() = runTokenTest {
        val tokens = client.login()
        client.post("/auth/logout") {
            bearerAuth(tokens.accessToken)
            contentType(ContentType.Application.Json)
            setBody(RefreshTokenRequest(tokens.refreshToken))
        }.assertStatusCode(HttpStatusCode.NoContent)

        client.profile(tokens.accessToken).assertError(Error.NotLoggedIn())
        client.refresh(tokens.refreshToken).assertError(Error.NotLoggedIn())
    }

    @Test
    fun test_logout_withAnUnknownToken_stillSucceeds() = runTokenTest {
        client.post("/auth/logout") {
            contentType(ContentType.Application.Json)
            setBody(RefreshTokenRequest("cea_rt_unknown"))
        }.assertStatusCode(HttpStatusCode.NoContent)
    }

    @Test
    fun test_logout_withOnlyTheAccessToken() = runTokenTest {
        val tokens = client.login()
        client.post("/auth/logout") { bearerAuth(tokens.accessToken) }.assertStatusCode(HttpStatusCode.NoContent)
        client.profile(tokens.accessToken).assertError(Error.NotLoggedIn())
        client.refresh(tokens.refreshToken).assertError(Error.NotLoggedIn())
    }

    @Test
    fun test_legacyRoutes_areGone() = runTokenTest {
        client.submitForm(
            "/login",
            parameters {
                append("email", FakeUser.EMAIL)
                append("password", password)
            },
        ).assertStatusCode(HttpStatusCode.NotFound)
        client.post("/auth/session/exchange").assertStatusCode(HttpStatusCode.NotFound)
        client.post("/verify-restore-key").assertStatusCode(HttpStatusCode.NotFound)
    }

    @Test
    fun test_disabledUser_isLockedOut() = runTokenTest {
        val tokens = client.login()
        Database { UserReferenceEntity[FakeUser.SUB].isDisabled = true }

        client.profile(tokens.accessToken).assertError(Error.NotLoggedIn())
        client.refresh(tokens.refreshToken).assertError(Error.NotLoggedIn())
    }

    @Test
    fun test_groupChanges_applyImmediately() = runTokenTest {
        val tokens = client.login()
        // An admin-only route, for a user that doesn't exist: only an admin gets past the permission check.
        suspend fun promote() = client.post("/users/unknown-user/promote") { bearerAuth(tokens.accessToken) }
        promote().assertError(Error.NotAnAdmin())

        Database { UserReferenceEntity[FakeUser.SUB].groups = FakeUser.GROUPS + ADMIN_GROUP_NAME }
        promote().assertError(Error.UserNotFound())
    }

    @Test
    fun test_passwordReset_revokesEverySession() = runTokenTest {
        val tokens = client.login()
        Database {
            RecoverPasswordRequests.insert {
                it[id] = "reset-request"
                it[user] = FakeUser.SUB
            }
        }
        client.submitForm(
            "/reset_password",
            parameters {
                append("request_id", "reset-request")
                append("password", "NewPassword123")
            },
        ).assertStatusCode(HttpStatusCode.OK)

        client.profile(tokens.accessToken).assertError(Error.NotLoggedIn())
        client.refresh(tokens.refreshToken).assertError(Error.NotLoggedIn())
    }

    @Test
    fun test_refreshTokens_areStoredHashed() = runTokenTest {
        val tokens = client.login()
        val stored = Database { AuthRefreshTokens.selectAll().map { it[AuthRefreshTokens.id].value } }
        assertEquals(1, stored.size)
        assertTrue(stored.none { it.contains(tokens.refreshToken) })
    }

    @Test
    fun test_production_refusesPublicSessionKeys() {
        SessionsKeys.override("SECRET_ENCRYPT_KEY", SessionsKeys.DEFAULT_ENCRYPT_KEY)
        SessionsKeys.override("SECRET_SIGN_KEY", SessionsKeys.DEFAULT_SIGN_KEY)
        try {
            assertFailsWith<IllegalStateException> {
                testApplication {
                    application { module(isTesting = false, isDevelopment = false) }
                    startApplication()
                }
            }
        } finally {
            SessionsKeys.override("SECRET_ENCRYPT_KEY", null)
            SessionsKeys.override("SECRET_SIGN_KEY", null)
        }
    }

    @Test
    fun test_webDavCookie_isScopedToWebDav() = runApplicationTest(
        databaseInitBlock = {
            FakeAdminUser.provideEntity().password = Passwords.hash(password.toCharArray())
        },
    ) {
        val response = client.get("/webdav/") { basicAuth(FakeAdminUser.EMAIL, password) }
        response.assertStatusCode(HttpStatusCode.OK)
        val cookie = response.setCookie().single()
        assertEquals(WebDavSession.COOKIE_NAME, cookie.name)
        assertEquals(WebDavSession.PATH, cookie.path)

        // Grants nothing outside WebDAV, even if sent there.
        client.get("/profile") {
            header(HttpHeaders.Cookie, renderCookieHeader(cookie))
        }.assertError(Error.NotLoggedIn())
        // But works for WebDAV without the password.
        client.get("/webdav/") {
            header(HttpHeaders.Cookie, renderCookieHeader(cookie))
        }.assertStatusCode(HttpStatusCode.OK)
    }
}
