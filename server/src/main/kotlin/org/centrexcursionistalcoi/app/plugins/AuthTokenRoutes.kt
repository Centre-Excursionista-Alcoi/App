package org.centrexcursionistalcoi.app.plugins

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.request.receiveNullable
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.centrexcursionistalcoi.app.data.RefreshTokenRequest
import org.centrexcursionistalcoi.app.data.RestoreKeyVerificationRequest
import org.centrexcursionistalcoi.app.data.TokenResponse
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.UserReferenceEntity
import org.centrexcursionistalcoi.app.database.table.AuthEventType
import org.centrexcursionistalcoi.app.database.table.AuthSessionMethod
import org.centrexcursionistalcoi.app.database.table.AuthSessionRevocationReason
import org.centrexcursionistalcoi.app.error.Error
import org.centrexcursionistalcoi.app.error.respondError
import org.centrexcursionistalcoi.app.routes.assertContentType
import org.centrexcursionistalcoi.app.security.AuthTokens
import org.centrexcursionistalcoi.app.security.ClientInfo
import org.centrexcursionistalcoi.app.security.RefreshResult
import org.centrexcursionistalcoi.app.security.RestoreKeyVerification
import org.centrexcursionistalcoi.app.security.getAccessTokenSessionId
import org.centrexcursionistalcoi.app.security.verifyRestoreKey

/**
 * Token-based authentication (see [AuthTokens]). Every route that hands out tokens responds a [TokenResponse].
 */
fun Route.authTokenRoutes() {
    route("/auth") {
        rateLimit(RateLimits.AUTHENTICATION) {
            post("/login") {
                assertContentType(ContentType.Application.FormUrlEncoded) ?: return@post

                val parameters = call.receiveParameters()
                val email = parameters["email"]?.trim()?.uppercase()
                    ?: return@post respondAuthError(AuthEventType.LOGIN, null, Error.IncorrectPasswordOrEmail())
                val password = parameters["password"]?.trim()?.toCharArray()
                    ?: return@post respondAuthError(AuthEventType.LOGIN, email, Error.IncorrectPasswordOrEmail())

                login(email, password)?.let { return@post respondAuthError(AuthEventType.LOGIN, email, it) }

                recordAuthEvent(AuthEventType.LOGIN, email, null)
                val tokens = Database {
                    val user = UserReferenceEntity.findByEmail(email) ?: error("User with email $email not found")
                    AuthTokens.startSession(user, AuthSessionMethod.PASSWORD, ClientInfo.from(call))
                }
                respondTokens(tokens)
            }

            post("/webauthn/verify") {
                val request = call.receiveNullable<RestoreKeyVerificationRequest>()
                    ?: return@post respondError(Error.MissingArgument("authenticationResponseJson"))
                when (val result = verifyRestoreKey(request)) {
                    is RestoreKeyVerification.Failure ->
                        respondAuthError(AuthEventType.WEBAUTHN_LOGIN, null, result.error)
                    is RestoreKeyVerification.Success -> {
                        recordAuthEvent(AuthEventType.WEBAUTHN_LOGIN, result.user.email, null)
                        val tokens = Database {
                            AuthTokens.startSession(
                                result.user,
                                AuthSessionMethod.WEBAUTHN,
                                ClientInfo.from(call),
                            )
                        }
                        respondTokens(tokens)
                    }
                }
            }
        }

        rateLimit(RateLimits.TOKEN_REFRESH) {
            post("/refresh") {
                val request = call.receiveNullable<RefreshTokenRequest>()
                    ?: return@post respondError(Error.MissingArgument("refresh_token"))
                when (val result = Database { AuthTokens.refresh(request.refreshToken) }) {
                    is RefreshResult.Success -> respondTokens(result.tokens)
                    RefreshResult.Invalid ->
                        respondAuthError(AuthEventType.TOKEN_REFRESH, null, Error.NotLoggedIn())
                    is RefreshResult.Reused ->
                        respondAuthError(AuthEventType.REFRESH_TOKEN_REUSE, result.email, Error.NotLoggedIn())
                }
            }

            // Ends the session of the refresh token in the body, and of the access token authenticating the call, if
            // any. Always succeeds, so it can't be used to tell whether a token is valid.
            post("/logout") {
                // Optional: a client may log out with just its access token.
                val request = runCatching { call.receiveNullable<RefreshTokenRequest>() }.getOrNull()
                val accessTokenSessionId = call.getAccessTokenSessionId()
                Database {
                    request?.let { AuthTokens.revokeSessionOf(it.refreshToken, AuthSessionRevocationReason.LOGOUT) }
                    accessTokenSessionId?.let { AuthTokens.revokeSession(it, AuthSessionRevocationReason.LOGOUT) }
                }
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

private suspend fun RoutingContext.respondTokens(tokens: TokenResponse) {
    // RFC 6749 §5.1: token responses must never be cached.
    call.response.header(HttpHeaders.CacheControl, "no-store")
    call.response.header(HttpHeaders.Pragma, "no-cache")
    call.respond(tokens)
}
