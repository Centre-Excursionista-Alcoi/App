package org.centrexcursionistalcoi.app.auth

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.centrexcursionistalcoi.app.BuildKonfig
import org.centrexcursionistalcoi.app.data.TokenResponse
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.network._httpClient
import java.io.IOException
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TestAuthBackendRestoreKeys {
    private val originalHttpClient = _httpClient

    private val tokens = TokenResponse(
        accessToken = "access",
        accessTokenExpiresIn = 600,
        refreshToken = "refresh",
        refreshTokenExpiresIn = 3600,
        accountEmail = "user@example.com",
    )

    private val credentialsStore = mockk<CredentialsStore>()
    private val sessionTokens = mockk<SessionTokens>(relaxUnitFun = true)
    private val restoreKeys = mockk<RestoreKeys>(relaxUnitFun = true)
    private val authBackend = AuthBackend(mockk(), credentialsStore, sessionTokens, restoreKeys)

    @AfterTest
    fun tearDown() {
        _httpClient = originalHttpClient
    }

    @Test
    fun `a valid saved session is refreshed, without the restore key`() = runTest {
        every { credentialsStore.getSession() } returns SavedSession("user@example.com", "refresh")
        coEvery { sessionTokens.refresh(any()) } returns true

        assertTrue(authBackend.tryAutoRelogin())
        coVerify(exactly = 0) { restoreKeys.redeem() }
    }

    @Test
    fun `without a saved session the restore key is redeemed`() = runTest {
        every { credentialsStore.getSession() } returns null
        coEvery { restoreKeys.redeem() } returns tokens

        assertTrue(authBackend.tryAutoRelogin())
        coVerify { sessionTokens.onLoggedIn(tokens) }
    }

    @Test
    fun `a session that's over falls back to the restore key`() = runTest {
        every { credentialsStore.getSession() } returns SavedSession("user@example.com", "refresh")
        coEvery { sessionTokens.refresh(any()) } returns false
        coEvery { restoreKeys.redeem() } returns tokens

        assertTrue(authBackend.tryAutoRelogin())
        coVerify { sessionTokens.onLoggedIn(tokens) }
    }

    @Test
    fun `a failed refresh keeps the session and doesn't touch the restore key`() = runTest {
        every { credentialsStore.getSession() } returns SavedSession("user@example.com", "refresh")
        coEvery { sessionTokens.refresh(any()) } throws IOException("offline")

        assertFalse(authBackend.tryAutoRelogin())
        coVerify(exactly = 0) { restoreKeys.redeem() }
    }

    @Test
    fun `without a session or restore key there's nothing to recover`() = runTest {
        every { credentialsStore.getSession() } returns null
        coEvery { restoreKeys.redeem() } returns null

        assertFalse(authBackend.tryAutoRelogin())
    }

    @Test
    fun `logging in with a password creates a restore key`() = runTest {
        _httpClient = HttpClient(MockEngine {
            respond(
                json.encodeToString(TokenResponse.serializer(), tokens),
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }) {
            defaultRequest { url(BuildKonfig.SERVER_URL) }
            install(ContentNegotiation) { json(json) }
        }

        authBackend.authenticate("user@example.com", "password")

        coVerify { sessionTokens.onLoggedIn(tokens) }
        verify { restoreKeys.create() }
    }
}
