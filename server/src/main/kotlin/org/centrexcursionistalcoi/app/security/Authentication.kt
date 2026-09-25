package org.centrexcursionistalcoi.app.security

import com.webauthn4j.credential.CredentialRecordImpl
import com.webauthn4j.data.AuthenticationParameters
import com.webauthn4j.data.RegistrationParameters
import com.webauthn4j.data.client.challenge.DefaultChallenge
import com.webauthn4j.server.ServerProperty
import com.webauthn4j.verifier.exception.VerificationException
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.request.receive
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.post
import io.ktor.server.sessions.SessionTransportTransformerEncrypt
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.cookie
import io.ktor.util.AttributeKey
import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.ADMIN_GROUP_NAME
import org.centrexcursionistalcoi.app.ConfigProvider
import org.centrexcursionistalcoi.app.data.RegisterRestoreKeyRequest
import org.centrexcursionistalcoi.app.data.RestoreKeyVerificationRequest
import org.centrexcursionistalcoi.app.data.webauthn.AuthenticationOptionsResponse
import org.centrexcursionistalcoi.app.data.webauthn.CreationOptionsResponse
import org.centrexcursionistalcoi.app.data.webauthn.RelyingParty
import org.centrexcursionistalcoi.app.data.webauthn.WebAuthnUser
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.UserCredentialRecordEntity
import org.centrexcursionistalcoi.app.database.entity.UserReferenceEntity
import org.centrexcursionistalcoi.app.error.Error
import org.centrexcursionistalcoi.app.error.respondError
import org.centrexcursionistalcoi.app.security.UserSession.Companion.getUserSession
import org.centrexcursionistalcoi.app.security.UserSession.Companion.getUserSessionOrFail
import org.centrexcursionistalcoi.app.storage.RedisStoreMap
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import java.util.Base64
import java.util.UUID

private val secretEncryptKey by lazy { (SessionsKeys.secretEncryptKey ?: SessionsKeys.DEFAULT_ENCRYPT_KEY).hexToByteArray() }
private val secretSignKey by lazy { (SessionsKeys.secretSignKey ?: SessionsKeys.DEFAULT_SIGN_KEY).hexToByteArray() }

object SessionsKeys : ConfigProvider() {
    /** Public (in this repository and `compose.yml`), so only acceptable for development and tests. */
    const val DEFAULT_ENCRYPT_KEY = "00112233445566778899aabbccddeeff"
    const val DEFAULT_SIGN_KEY = "6819b57a326945c1968f45236589"

    val secretEncryptKey get() = getenv("SECRET_ENCRYPT_KEY")
    val secretSignKey get() = getenv("SECRET_SIGN_KEY")

    /**
     * Whether session cookies (see [WebDavSession]) would be encrypted and signed with keys anyone can know,
     * letting them forge a session for any user.
     */
    fun areInsecure(): Boolean = secretEncryptKey.let { it == null || it == DEFAULT_ENCRYPT_KEY } ||
        secretSignKey.let { it == null || it == DEFAULT_SIGN_KEY }
}

private val resolvedSessionKey = AttributeKey<ResolvedSession>("CEA-ResolvedSession")

/**
 * The outcome of authenticating a call, computed once per call (see [UserSession.getUserSession]).
 * @param accessTokenSessionId The session of the access token that authenticated the call, if any.
 * @param bearerPresented Whether the call carried a bearer token at all, even an invalid one.
 */
private class ResolvedSession(
    val session: UserSession?,
    val accessTokenSessionId: UUID?,
    val bearerPresented: Boolean,
)

private fun ApplicationCall.bearerToken(): String? = request.headers[HttpHeaders.Authorization]
    ?.takeIf { it.startsWith("Bearer ", ignoreCase = true) }
    ?.substring("Bearer ".length)
    ?.trim()
    ?.takeIf { it.isNotEmpty() }

private fun ApplicationCall.resolveSession(): ResolvedSession {
    val bearer = bearerToken() ?: return ResolvedSession(null, null, bearerPresented = false)
    val resolved = AuthTokens.resolveAccessToken(bearer)
    return ResolvedSession(resolved?.userSession, resolved?.sessionId, bearerPresented = true)
}

/** The session of the access token that authenticated this call, `null` if it isn't authenticated. */
fun ApplicationCall.getAccessTokenSessionId(): UUID? {
    getUserSession()
    return attributes[resolvedSessionKey].accessTokenSessionId
}

/**
 * The authenticated user of a call, see [getUserSession].
 * @param sub Subject Identifier
 * @param fullName Full Name
 * @param email Email Address
 * @param groups List of groups the user belongs to
 */
data class UserSession(val sub: String, val fullName: String, val email: String, val groups: List<String>) {
    companion object {
        /**
         * Creates a UserSession from an email address, checking that the user exists.
         * @param email Email Address of the user
         * @throws IllegalArgumentException if the user is not found
         */
        context(_: JdbcTransaction)
        fun fromEmail(email: String) = UserReferenceEntity.findByEmail(email)?.let { reference ->
            UserSession(
                sub = reference.sub.value,
                fullName = reference.fullName,
                email = email,
                groups = reference.groups,
            )
        } ?: error("User with email $email not found")

        /**
         * Creates a [UserSession] straight from an already-resolved [reference], e.g. after a WebAuthn login. Must be
         * called within a transaction, since `groups` is an array column.
         */
        fun fromReference(reference: UserReferenceEntity) = UserSession(
            sub = reference.sub.value,
            fullName = reference.fullName,
            email = reference.email,
            groups = reference.groups,
        )

        /**
         * Gets the [UserSession] authenticating the call from its bearer access token (see [AuthTokens]), or `null`
         * if there's none. Resolved once per call.
         *
         * Also appends a header (`CEA-LoggedIn`) to the response indicating whether the user is logged in or not.
         */
        fun ApplicationCall.getUserSession(): UserSession? {
            attributes.getOrNull(resolvedSessionKey)?.let { return it.session }
            val resolved = resolveSession()
            attributes.put(resolvedSessionKey, resolved)
            response.header("CEA-LoggedIn", (resolved.session != null).toString())
            return resolved.session
        }

        /**
         * Gets the [UserSession] from the call, or `null` if it doesn't exist.
         */
        fun RoutingContext.getUserSession(): UserSession? = call.getUserSession()

        /**
         * Gets the [UserSession] from the call, or responds with an error ([Error.NotLoggedIn]) if it doesn't exist.
         * @see getUserSession
         */
        suspend fun RoutingContext.getUserSessionOrFail(): UserSession? {
            val session = getUserSession()
            if (session == null) {
                if (call.attributes[resolvedSessionKey].bearerPresented) {
                    // RFC 6750 §3.1: tells the client to refresh its access token.
                    call.response.header(HttpHeaders.WWWAuthenticate, "Bearer error=\"invalid_token\"")
                }
                respondError(Error.NotLoggedIn())
                return null
            } else {
                return session
            }
        }

        suspend fun RoutingContext.assertAdmin(): UserSession? {
            val session = getUserSessionOrFail() ?: return null
            if (!session.isAdmin()) {
                respondError(Error.NotAnAdmin())
                return null
            }
            return session
        }
    }

    fun isAdmin(): Boolean = groups.contains(ADMIN_GROUP_NAME)

    /**
     * Gets the [UserReferenceEntity] associated with this session user's sub.
     */
    fun getReference() = Database {
        UserReferenceEntity.findById(sub)
    }

    /**
     * Gets the Base64Url-encoded version of the `sub` field, which is used as the user ID in WebAuthn.
     */
    fun subBase64Url(): String = Base64.getUrlEncoder().withoutPadding().encodeToString(sub.toByteArray())
}

/**
 * The WebDAV admin area's own session (see `WebDAVRoutes.kt`), started with HTTP Basic credentials: WebDAV clients
 * can't use bearer tokens. Its cookie is only sent to `/webdav`, so it grants nothing in the rest of the API, and it
 * only names the user, who is re-read (and must still be an admin) on every request.
 */
@Serializable
data class WebDavSession(val sub: String) {
    companion object {
        const val COOKIE_NAME = "WEBDAV_SESSION"
        const val PATH = "/webdav"
    }
}

fun Application.configureAuthentication(isTesting: Boolean, isDevelopment: Boolean) {
    check(isTesting || isDevelopment || !SessionsKeys.areInsecure()) {
        "SECRET_ENCRYPT_KEY and SECRET_SIGN_KEY must be set to secret values in production: with the default " +
            "ones, anyone can forge a WebDAV session cookie for any admin."
    }
    install(Sessions) {
        cookie<WebDavSession>(WebDavSession.COOKIE_NAME) {
            cookie.httpOnly = true
            cookie.secure = !isTesting && !isDevelopment
            if (!isDevelopment) cookie.extensions["SameSite"] = "strict"
            cookie.path = WebDavSession.PATH
            cookie.maxAgeInSeconds = 60 * 60 // 1 hour
            transform(SessionTransportTransformerEncrypt(secretEncryptKey, secretSignKey))
        }
    }
}

/**
 * Routes for both directions of a WebAuthn ceremony. Currently only reachable from a logged-in session (to
 * register a Restore Credential) and from the login screen with no session yet (to redeem one on a new device),
 * but the ceremony itself -- challenge, verify, store/load a [UserCredentialRecordEntity] -- has no restore-
 * specific logic in it: a future user-facing passkey (registered while already logged in, exactly like a restore
 * key is, and redeemed the same way a restore key is) would reuse this unchanged.
 */
fun Route.webAuthnRoutes() {
    // App requests options to create a credential on the OLD device (Restore Credential today; the same
    // ceremony a user explicitly registering a passkey would use).
    post("/generate-restore-challenge") {
        // Only allow logged-in users to request one.
        val user = getUserSessionOrFail() ?: return@post

        val challenge = generateWebAuthnChallenge()
        // WebAuthn requires Base64Url (no padding) throughout.
        val challengeBase64Url = Base64.getUrlEncoder().withoutPadding().encodeToString(challenge.value)

        // Keyed by sub: one in-flight registration at a time per user, which is all this needs.
        RedisStoreMap.default.put("webauthn_challenge:${user.sub}", challengeBase64Url, 300)

        call.respond(
            CreationOptionsResponse(
                challenge = challengeBase64Url,
                rp = RelyingParty(name = "CEA App", id = webAuthnRpId),
                user = WebAuthnUser(
                    id = user.subBase64Url(),
                    name = user.email,
                    displayName = user.fullName,
                ),
            )
        )
    }

    // App requests options to redeem a credential on a NEW device, before it has any session at all.
    post("/generate-auth-challenge") {
        val challenge = generateWebAuthnChallenge()
        val challengeBase64Url = Base64.getUrlEncoder().withoutPadding().encodeToString(challenge.value)

        // Keyed by the challenge itself, not by user -- the server doesn't know who's asking yet; whichever
        // credential the platform surfaces for this request tells it that (see verifyRestoreKey below).
        RedisStoreMap.default.put("auth_challenge:$challengeBase64Url", "valid", 300)

        call.respond(
            AuthenticationOptionsResponse(
                challenge = challengeBase64Url,
                rpId = webAuthnRpId,
            )
        )
    }

    post("/register-restore-key") {
        val user = getUserSessionOrFail() ?: return@post
        val request = call.receive<RegisterRestoreKeyRequest>()

        val challengeBase64Url = RedisStoreMap.default.get("webauthn_challenge:${user.sub}")
            ?: return@post respondError(Error.InvalidArgument("challenge", "Challenge expired or was never requested."))
        val challengeBytes = Base64.getUrlDecoder().decode(challengeBase64Url)

        val serverProperty = ServerProperty.builder()
            .rpId(webAuthnRpId)
            .origins(webAuthnAndroidOrigins)
            .challenge(DefaultChallenge(challengeBytes))
            .build()

        try {
            val parsed = webAuthnManager.parseRegistrationResponseJSON(request.registrationResponseJson)
            // pubKeyCredParams = null: accept any algorithm, matching the non-strict manager's own leniency.
            val registrationParameters = RegistrationParameters(serverProperty, null, true)
            val registrationData = webAuthnManager.verify(parsed, registrationParameters)

            val authenticatorData = registrationData.attestationObject!!.authenticatorData
            val attestedCredentialData = authenticatorData.attestedCredentialData!!
            val credentialIdBase64Url = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(attestedCredentialData.credentialId)
            val attestedCredentialDataBytes = attestedCredentialDataConverter.convert(attestedCredentialData)

            val stored = Database {
                val reference = UserReferenceEntity.findById(user.sub)
                    ?: return@Database null
                UserCredentialRecordEntity.new(credentialIdBase64Url) {
                    this.user = reference
                    this.attestedCredentialData = attestedCredentialDataBytes
                    this.signCount = authenticatorData.signCount
                }.also {
                    // Only the key this device replaced: the user's other devices keep their own restore keys.
                    request.replacesCredentialId
                        ?.takeIf { it != credentialIdBase64Url }
                        ?.let { UserCredentialRecordEntity.deleteIfOwnedBy(it, user.sub) }
                }
            }
            if (stored == null) return@post respondError(Error.EntityNotFound(UserReferenceEntity::class, user.sub))

            // Only clear the challenge once the credential is actually persisted, so a failure above can still
            // be retried with the same challenge instead of forcing a fresh /generate-restore-challenge round trip.
            RedisStoreMap.default.remove("webauthn_challenge:${user.sub}")

            call.respond(HttpStatusCode.OK)
        } catch (e: VerificationException) {
            respondError(Error.InvalidArgument("registrationResponseJson", e.message))
        } catch (e: Exception) {
            respondError(Error.Exception(e))
        }
    }
}

sealed interface RestoreKeyVerification {
    data class Success(val user: UserReferenceEntity) : RestoreKeyVerification
    data class Failure(val error: Error) : RestoreKeyVerification
}

/**
 * Verifies a WebAuthn authentication response against the challenge issued by `/generate-auth-challenge` and the
 * credential stored at registration, resolving the user it authenticates.
 */
suspend fun verifyRestoreKey(request: RestoreKeyVerificationRequest): RestoreKeyVerification {
    try {
        val authenticationData = webAuthnManager.parseAuthenticationResponseJSON(request.authenticationResponseJson)

        // The challenge the client used is echoed back inside its own response -- no separate tracking ID
        // needed, the same way /generate-auth-challenge stored it keyed by its own value.
        val challengeBytes = authenticationData.collectedClientData?.challenge?.value
            ?: return RestoreKeyVerification.Failure(Error.InvalidArgument("authenticationResponseJson"))
        val challengeBase64Url = Base64.getUrlEncoder().withoutPadding().encodeToString(challengeBytes)
        // Consumed before verifying, so that a challenge can never be used twice, even by concurrent requests or
        // after a failed attempt.
        RedisStoreMap.default.remove("auth_challenge:$challengeBase64Url")
            ?: return RestoreKeyVerification.Failure(
                Error.InvalidArgument("challenge", "Challenge expired or was never requested.")
            )

        val credentialIdBase64Url = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(authenticationData.credentialId)
        val stored = Database { UserCredentialRecordEntity.findById(credentialIdBase64Url) }
            ?: return RestoreKeyVerification.Failure(
                Error.EntityNotFound(UserCredentialRecordEntity::class, credentialIdBase64Url)
            )
        val (attestedCredentialDataBytes, storedSignCount) = Database { stored.attestedCredentialData to stored.signCount }

        val serverProperty = ServerProperty.builder()
            .rpId(webAuthnRpId)
            .origins(webAuthnAndroidOrigins)
            .challenge(DefaultChallenge(challengeBytes))
            .build()

        val attestedCredentialData = attestedCredentialDataConverter.convert(attestedCredentialDataBytes)
        // CredentialRecordImpl (WebAuthn Level 3), not the deprecated AuthenticatorImpl -- uvInitialized/
        // backupEligible/backupState/clientData/clientExtensions/transports aren't persisted (not needed to
        // verify a signature), so null throughout; only the pieces saved at registration matter here.
        val credentialRecord = CredentialRecordImpl(
            null, null, null, null,
            storedSignCount, attestedCredentialData, null, null, null, null,
        )
        // Restore Credentials are redeemed silently, with no user interaction to satisfy a verification
        // requirement -- userVerificationRequired = false.
        val authenticationParameters = AuthenticationParameters(serverProperty, credentialRecord, null, false)

        val verifiedData = webAuthnManager.verify(authenticationData, authenticationParameters)

        val user = Database {
            // Many platform authenticators (including the one behind Restore Credentials) always report a
            // signCount of 0 -- still worth persisting whatever comes back, so a future authenticator that
            // does increment it is tracked correctly from here on.
            stored.signCount = verifiedData.authenticatorData!!.signCount
            stored.user
        }
        // Same rule as a password login.
        if (user.isDisabled) return RestoreKeyVerification.Failure(Error.UserIsDisabled())
        return RestoreKeyVerification.Success(user)
    } catch (_: VerificationException) {
        // Mirrors a rejected password login: a real credential that just didn't check out, not a malformed
        // request -- same status code and error shape as /login's own failure path.
        return RestoreKeyVerification.Failure(Error.IncorrectPasswordOrEmail())
    } catch (e: Exception) {
        return RestoreKeyVerification.Failure(Error.Exception(e))
    }
}
