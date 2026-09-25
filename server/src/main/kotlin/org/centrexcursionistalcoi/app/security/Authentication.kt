package org.centrexcursionistalcoi.app.security

import com.webauthn4j.credential.CredentialRecordImpl
import com.webauthn4j.data.AuthenticationParameters
import com.webauthn4j.data.RegistrationParameters
import com.webauthn4j.data.client.challenge.DefaultChallenge
import com.webauthn4j.server.ServerProperty
import com.webauthn4j.verifier.exception.VerificationException
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
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set
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

private val secretEncryptKey by lazy { (SessionsKeys.secretEncryptKey ?: "00112233445566778899aabbccddeeff").hexToByteArray() }
private val secretSignKey by lazy { (SessionsKeys.secretSignKey ?: "6819b57a326945c1968f45236589").hexToByteArray() }

object SessionsKeys : ConfigProvider() {
    val secretEncryptKey get() = getenv("SECRET_ENCRYPT_KEY")
    val secretSignKey get() = getenv("SECRET_SIGN_KEY")
}

/**
 * @param sub Subject Identifier
 * @param fullName Full Name
 * @param email Email Address
 * @param groups List of groups the user belongs to
 */
@Serializable
data class UserSession(val sub: String, val fullName: String, val email: String, val groups: List<String>) {
    companion object {
        const val COOKIE_NAME = "USER_SESSION"

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

        /** Creates a [UserSession] straight from an already-resolved [reference], e.g. after a WebAuthn login. */
        fun fromReference(reference: UserReferenceEntity) = UserSession(
            sub = reference.sub.value,
            fullName = reference.fullName,
            email = reference.email,
            groups = reference.groups,
        )

        /**
         * Gets the [UserSession] from the call, or `null` if it doesn't exist.
         *
         * Also appends a header (`CEA-LoggedIn`) to the response indicating whether the user is logged in or not.
         */
        fun ApplicationCall.getUserSession(): UserSession? {
            val session = sessions.get<UserSession>()
            response.header("CEA-LoggedIn", (session != null).toString())
            return session
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

fun Application.configureAuthentication(isTesting: Boolean, isDevelopment: Boolean) {
    install(Sessions) {
        cookie<UserSession>(UserSession.COOKIE_NAME) {
            cookie.httpOnly = true                        // Prevent JS access
            cookie.secure = !isTesting && !isDevelopment  // Use HTTPS in production
            if (!isDevelopment) cookie.extensions["SameSite"] = "lax"
            cookie.path = "/"
            cookie.maxAgeInSeconds = 7 * 24 * 60 * 60 // 1 week

            // Encrypt and sign the cookie to prevent tampering
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
        // credential the platform surfaces for this request tells it that (see /verify-restore-key below).
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

    // App sends the credential response from the NEW device, with no session yet -- this is what creates one.
    post("/verify-restore-key") {
        val request = call.receive<RestoreKeyVerificationRequest>()

        try {
            val authenticationData = webAuthnManager.parseAuthenticationResponseJSON(request.authenticationResponseJson)

            // The challenge the client used is echoed back inside its own response -- no separate tracking ID
            // needed, the same way /generate-auth-challenge stored it keyed by its own value.
            val challengeBytes = authenticationData.collectedClientData?.challenge?.value
                ?: return@post respondError(Error.InvalidArgument("authenticationResponseJson"))
            val challengeBase64Url = Base64.getUrlEncoder().withoutPadding().encodeToString(challengeBytes)
            RedisStoreMap.default.get("auth_challenge:$challengeBase64Url")
                ?: return@post respondError(Error.InvalidArgument("challenge", "Challenge expired or was never requested."))

            val credentialIdBase64Url = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(authenticationData.credentialId)
            val stored = Database { UserCredentialRecordEntity.findById(credentialIdBase64Url) }
                ?: return@post respondError(Error.EntityNotFound(UserCredentialRecordEntity::class, credentialIdBase64Url))
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

            val session = Database {
                // Many platform authenticators (including the one behind Restore Credentials) always report a
                // signCount of 0 -- still worth persisting whatever comes back, so a future authenticator that
                // does increment it is tracked correctly from here on.
                stored.signCount = verifiedData.authenticatorData!!.signCount
                UserSession.fromReference(stored.user)
            }

            RedisStoreMap.default.remove("auth_challenge:$challengeBase64Url")
            call.sessions.set(session)
            call.respond(HttpStatusCode.OK)
        } catch (e: VerificationException) {
            // Mirrors a rejected password login: a real credential that just didn't check out, not a malformed
            // request -- same status code and error shape as /login's own failure path.
            respondError(Error.IncorrectPasswordOrEmail())
        } catch (e: Exception) {
            respondError(Error.Exception(e))
        }
    }
}
