package org.centrexcursionistalcoi.app.push

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.mockk.coEvery
import io.mockk.mockk
import java.util.Collections
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import org.centrexcursionistalcoi.app.auth.CredentialsStore
import org.centrexcursionistalcoi.app.auth.SavedSession
import org.centrexcursionistalcoi.app.auth.SessionTokens
import org.centrexcursionistalcoi.app.data.TokenResponse
import org.centrexcursionistalcoi.app.json
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class TestSSEClient {
    private val credentialsStore = mockk<CredentialsStore> {
        coEvery { getSession() } returns SavedSession("user@example.com", "refresh-1")
        coEvery { saveSession(any(), any()) } returns Unit
    }

    @BeforeTest
    fun setUp() {
        startKoin { modules(module { single { SessionTokens(credentialsStore) } }) }
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    /** #753: refreshing the access token needs the Resources plugin, which this client was missing. */
    @Test
    fun `it signs in with the session, refreshing the access token first`() = runTest {
        val requests = Collections.synchronizedList(mutableListOf<HttpRequestData>())
        val client = createSSEClient(MockEngine { request ->
            requests += request
            when (request.url.encodedPath) {
                "/auth/refresh" -> respond(
                    json.encodeToString(
                        TokenResponse.serializer(),
                        TokenResponse(
                            accessToken = "access-2",
                            accessTokenExpiresIn = 600,
                            refreshToken = "refresh-2",
                            refreshTokenExpiresIn = 3600,
                            accountEmail = "user@example.com",
                        ),
                    ),
                    HttpStatusCode.OK,
                    headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
                else -> respond("", HttpStatusCode.OK)
            }
        })

        client.get("/sse")

        assertEquals(listOf("/auth/refresh", "/sse"), requests.map { it.url.encodedPath })
        assertEquals("Bearer access-2", requests.last().headers[HttpHeaders.Authorization])
    }
}
