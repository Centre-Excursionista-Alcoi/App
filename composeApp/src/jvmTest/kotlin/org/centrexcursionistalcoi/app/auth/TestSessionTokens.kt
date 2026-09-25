package org.centrexcursionistalcoi.app.auth

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.client.request.get
import io.ktor.content.TextContent
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.centrexcursionistalcoi.app.BuildKonfig
import org.centrexcursionistalcoi.app.data.RefreshTokenRequest
import org.centrexcursionistalcoi.app.data.TokenResponse
import org.centrexcursionistalcoi.app.json
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.util.Collections
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class TestSessionTokens {
    /** An in-memory [CredentialsStore]. */
    private var savedSession: SavedSession? = null
    private val credentialsStore = mockk<CredentialsStore> {
        every { getSession() } answers { savedSession }
        every { saveSession(any(), any()) } answers { savedSession = SavedSession(firstArg(), secondArg()) }
        every { clear() } answers { savedSession = null }
    }
    private val sessionTokens = SessionTokens(credentialsStore)

    private val requests: MutableList<HttpRequestData> = Collections.synchronizedList(mutableListOf())
    private var refreshCount = 0

    @BeforeTest
    fun setUp() {
        startKoin { modules(module { single { sessionTokens } }) }
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    private fun tokens(n: Int, expiresIn: Long = 600) = TokenResponse(
        accessToken = "access-$n",
        accessTokenExpiresIn = expiresIn,
        refreshToken = "refresh-$n",
        refreshTokenExpiresIn = 3600,
        accountEmail = "user@example.com",
    )

    private fun MockRequestHandleScope.respondJson(body: String, status: HttpStatusCode = HttpStatusCode.OK) =
        respond(body, status, headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))

    /**
     * A client whose server accepts the access token named [validAccessToken], and refreshes with [onRefresh].
     */
    private fun client(
        validAccessToken: () -> String,
        onRefresh: MockRequestHandleScope.(RefreshTokenRequest) -> HttpResponseData = { request ->
            refreshCount++
            val next = request.refreshToken.removePrefix("refresh-").toInt() + 1
            respondJson(json.encodeToString(TokenResponse.serializer(), tokens(next)))
        },
    ) = HttpClient(MockEngine { request ->
        requests += request
        when (request.url.encodedPath) {
            "/auth/refresh" -> {
                val body = json.decodeFromString(RefreshTokenRequest.serializer(), (request.body as TextContent).text)
                onRefresh(body)
            }
            else -> if (request.headers[HttpHeaders.Authorization] == "Bearer ${validAccessToken()}") {
                respondJson("{}")
            } else {
                respondJson("{}", HttpStatusCode.Unauthorized)
            }
        }
    }) {
        defaultRequest { url(BuildKonfig.SERVER_URL) }
        install(ContentNegotiation) { json(json) }
    }.installSessionAuth()

    @Test
    fun `requests carry the access token`() = runTest {
        sessionTokens.onLoggedIn(tokens(1))
        val client = client(validAccessToken = { "access-1" })

        assertEquals(HttpStatusCode.OK, client.get("/profile").status)
        assertEquals(0, refreshCount)
    }

    @Test
    fun `the token is never sent to other hosts`() = runTest {
        sessionTokens.onLoggedIn(tokens(1))
        val client = client(validAccessToken = { "access-1" })

        client.get("https://example.com/releases")
        assertNull(requests.single().headers[HttpHeaders.Authorization])
    }

    @Test
    fun `a rejected token is refreshed, and the request retried`() = runTest {
        sessionTokens.onLoggedIn(tokens(1))
        val client = client(validAccessToken = { "access-2" })

        assertEquals(HttpStatusCode.OK, client.get("/profile").status)
        assertEquals(1, refreshCount)
        assertEquals("refresh-2", savedSession?.refreshToken)
    }

    @Test
    fun `an expiring token is refreshed before the request`() = runTest {
        sessionTokens.onLoggedIn(tokens(1, expiresIn = 10))
        val client = client(validAccessToken = { "access-2" })

        assertEquals(HttpStatusCode.OK, client.get("/profile").status)
        // Only the refresh and the request itself: the expiring token wasn't even tried.
        assertEquals(listOf("/auth/refresh", "/profile"), requests.map { it.url.encodedPath })
    }

    @Test
    fun `without a saved session the access token is refreshed from the stored refresh token`() = runTest {
        // What happens right after the app starts: only the refresh token survived.
        savedSession = SavedSession("user@example.com", "refresh-1")
        val client = client(validAccessToken = { "access-2" })

        assertEquals(HttpStatusCode.OK, client.get("/profile").status)
    }

    @Test
    fun `concurrent requests share a single refresh`() = runTest {
        sessionTokens.onLoggedIn(tokens(1))
        val client = client(validAccessToken = { "access-2" })

        List(10) { async { client.get("/profile").status } }.awaitAll()
            .forEach { assertEquals(HttpStatusCode.OK, it) }
        assertEquals(1, refreshCount)
    }

    @Test
    fun `a rejected refresh ends the session`() = runTest {
        sessionTokens.onLoggedIn(tokens(1))
        val client = client(
            validAccessToken = { "access-2" },
            onRefresh = { respondJson("{}", HttpStatusCode.Unauthorized) },
        )

        assertEquals(HttpStatusCode.Unauthorized, client.get("/profile").status)
        assertNull(savedSession)
    }

    @Test
    fun `a failed refresh keeps the session`() = runTest {
        sessionTokens.onLoggedIn(tokens(1))
        val client = client(
            validAccessToken = { "access-2" },
            onRefresh = { respondJson("""{"code":0,"description":"Server error"}""", HttpStatusCode.InternalServerError) },
        )

        assertFailsWith<Throwable> { client.get("/profile") }
        assertEquals("refresh-1", savedSession?.refreshToken)
    }

    @Test
    fun `refresh requests themselves carry no access token`() = runTest {
        sessionTokens.onLoggedIn(tokens(1))
        val client = client(validAccessToken = { "access-2" })

        client.get("/profile")
        assertNull(requests.single { it.url.encodedPath == "/auth/refresh" }.headers[HttpHeaders.Authorization])
    }
}
