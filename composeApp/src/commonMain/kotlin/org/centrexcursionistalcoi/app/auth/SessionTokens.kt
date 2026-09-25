package org.centrexcursionistalcoi.app.auth

import com.diamondedge.logging.logging
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.plugin
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.util.AttributeKey
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.centrexcursionistalcoi.app.BuildKonfig
import org.centrexcursionistalcoi.app.data.RefreshTokenRequest
import org.centrexcursionistalcoi.app.data.TokenResponse
import org.centrexcursionistalcoi.app.error.bodyAsError
import org.koin.core.annotation.Singleton
import org.koin.mp.KoinPlatformTools
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * Marks a request that must be sent without the session's access token: token requests themselves, and the ones
 * authenticated some other way (a password).
 */
val SkipSessionAuth = AttributeKey<Unit>("SkipSessionAuth")

fun HttpRequestBuilder.skipSessionAuth() {
    attributes.put(SkipSessionAuth, Unit)
}

/**
 * Holds the session's tokens: the access token in memory only, and the refresh token in [CredentialsStore].
 *
 * Access tokens are refreshed shortly before they expire, and whenever the server rejects one (see
 * [installSessionAuth]). Refreshes are serialized, since each refresh token can only be used once: two concurrent
 * refreshes with the same one would make the server revoke the whole session as a stolen token.
 */
@Singleton
class SessionTokens(private val credentialsStore: CredentialsStore) {
    private val log = logging()

    private val mutex = Mutex()

    private var accessToken: String? = null
    private var accessTokenExpiresAt: Instant? = null

    /** Refreshes a bit before the access token actually expires, to absorb clock skew and request latency. */
    private val expiryMargin = 30.seconds

    private fun isExpiring(): Boolean =
        accessTokenExpiresAt.let { it == null || Clock.System.now() + expiryMargin >= it }

    /** Stores the tokens of a session that has just been started. */
    suspend fun onLoggedIn(tokens: TokenResponse) = mutex.withLock { store(tokens) }

    /** Forgets the access token. The saved session itself is cleared by [CredentialsStore.clear]. */
    suspend fun onLoggedOut() = mutex.withLock {
        accessToken = null
        accessTokenExpiresAt = null
    }

    /**
     * Refreshes the tokens now.
     * @return whether there's still a valid session.
     * @throws Exception if the server couldn't be reached, or failed: the session is kept for later.
     */
    suspend fun refresh(client: HttpClient): Boolean = mutex.withLock { refreshLocked(client) != null }

    /** A valid access token, refreshing it if it's about to expire. `null` if there's no session. */
    internal suspend fun validAccessToken(client: HttpClient): String? = mutex.withLock {
        accessToken?.takeUnless { isExpiring() } ?: refreshLocked(client)
    }

    /** A new access token after the server rejected [rejected], unless another request already got one. */
    internal suspend fun accessTokenAfterRejection(client: HttpClient, rejected: String): String? = mutex.withLock {
        accessToken?.takeIf { it != rejected && !isExpiring() } ?: refreshLocked(client)
    }

    private suspend fun refreshLocked(client: HttpClient): String? {
        val session = credentialsStore.getSession() ?: return null
        val response = client.post("/auth/refresh") {
            skipSessionAuth()
            contentType(ContentType.Application.Json)
            setBody(RefreshTokenRequest(session.refreshToken))
        }
        if (response.status == HttpStatusCode.Unauthorized) {
            // Expired, revoked, or the user was disabled: the session is over.
            log.i { "The session is no longer valid, forgetting it." }
            credentialsStore.clear()
            accessToken = null
            accessTokenExpiresAt = null
            return null
        }
        // Anything else (rate limited, server error...) says nothing about the session: keep it for later.
        if (!response.status.isSuccess()) throw response.bodyAsError().toThrowable()

        val tokens = response.body<TokenResponse>()
        store(tokens)
        return tokens.accessToken
    }

    private fun store(tokens: TokenResponse) {
        // Saved first: the old refresh token is already invalid.
        credentialsStore.saveSession(tokens.accountEmail, tokens.refreshToken)
        accessToken = tokens.accessToken
        accessTokenExpiresAt = Clock.System.now() + tokens.accessTokenExpiresIn.seconds
    }
}

/**
 * Authenticates this client's requests to the server with the session's access token, refreshing it (and retrying
 * once) if the server rejects it. Requests to any other host never get the token.
 */
fun HttpClient.installSessionAuth(): HttpClient {
    val client = this
    val serverHost = Url(BuildKonfig.SERVER_URL).host
    plugin(HttpSend).intercept { request ->
        // Koin isn't started in some tests.
        val sessionTokens = KoinPlatformTools.defaultContext().getOrNull()?.getOrNull<SessionTokens>()
        if (sessionTokens == null || SkipSessionAuth in request.attributes || request.url.host != serverHost) {
            return@intercept execute(request)
        }
        val token = sessionTokens.validAccessToken(client) ?: return@intercept execute(request)
        request.headers[HttpHeaders.Authorization] = "Bearer $token"
        val call = execute(request)
        if (call.response.status != HttpStatusCode.Unauthorized) return@intercept call

        val newToken = sessionTokens.accessTokenAfterRejection(client, token) ?: return@intercept call
        request.headers[HttpHeaders.Authorization] = "Bearer $newToken"
        execute(request)
    }
    return this
}
