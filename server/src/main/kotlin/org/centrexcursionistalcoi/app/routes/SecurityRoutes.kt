package org.centrexcursionistalcoi.app.routes

import com.webauthn4j.verifier.exception.VerificationException
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.request.receiveNullable
import io.ktor.server.resources.delete
import io.ktor.server.resources.get
import io.ktor.server.resources.post
import io.ktor.server.resources.put
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import org.centrexcursionistalcoi.app.data.AddPasskeyRequest
import org.centrexcursionistalcoi.app.data.PasskeyInfo
import org.centrexcursionistalcoi.app.data.Reauthentication
import org.centrexcursionistalcoi.app.data.ReauthenticatedRequest
import org.centrexcursionistalcoi.app.data.SecurityInfo
import org.centrexcursionistalcoi.app.data.SetPasswordRequest
import org.centrexcursionistalcoi.app.data.webauthn.AuthenticatorSelection
import org.centrexcursionistalcoi.app.data.webauthn.CreationOptionsResponse
import org.centrexcursionistalcoi.app.data.webauthn.PublicKeyCredentialDescriptor
import org.centrexcursionistalcoi.app.data.webauthn.RelyingParty
import org.centrexcursionistalcoi.app.data.webauthn.WebAuthnUser
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.UserCredentialRecordEntity
import org.centrexcursionistalcoi.app.database.entity.UserReferenceEntity
import org.centrexcursionistalcoi.app.database.table.AuthEventType
import org.centrexcursionistalcoi.app.database.table.CredentialKind
import org.centrexcursionistalcoi.app.database.table.UserCredentialRecords
import org.centrexcursionistalcoi.app.error.Error
import org.centrexcursionistalcoi.app.error.respondError
import org.centrexcursionistalcoi.app.notifications.Email
import org.centrexcursionistalcoi.app.notifications.EmailTemplate
import org.centrexcursionistalcoi.app.notifications.email.mailersend.MailerSendEmail
import org.centrexcursionistalcoi.app.plugins.RateLimits
import org.centrexcursionistalcoi.app.plugins.recordAuthEvent
import org.centrexcursionistalcoi.app.plugins.respondAuthError
import org.centrexcursionistalcoi.app.security.AssertionVerification
import org.centrexcursionistalcoi.app.security.Passwords
import org.centrexcursionistalcoi.app.security.UserSession
import org.centrexcursionistalcoi.app.security.UserSession.Companion.getUserSessionOrFail
import org.centrexcursionistalcoi.app.security.generateWebAuthnChallenge
import org.centrexcursionistalcoi.app.security.toBase64Url
import org.centrexcursionistalcoi.app.security.verifyAssertion
import org.centrexcursionistalcoi.app.security.verifyRegistration
import org.centrexcursionistalcoi.app.security.webAuthnRpId
import org.centrexcursionistalcoi.app.storage.RedisStoreMap
import org.centrexcursionistalcoi.app.translation.locale
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq

/** How long a passkey can take to be created, once its options are handed out. */
private val passkeyCreationTimeout = 5.minutes

private fun passkeyChallengeKey(sub: String) = "passkey_challenge:$sub"

context(_: org.jetbrains.exposed.v1.jdbc.JdbcTransaction)
private fun passkeysOf(sub: String) = UserCredentialRecordEntity.find {
    (UserCredentialRecords.user eq sub) and (UserCredentialRecords.kind eq CredentialKind.PASSKEY)
}

/**
 * Whether [reauthentication] proves, again, that the request comes from the user of [session]: their password, or a
 * passkey sign-in with one of their own passkeys.
 */
private suspend fun verifyReauthentication(session: UserSession, reauthentication: Reauthentication): Boolean {
    reauthentication.password?.let { password ->
        val user = Database { UserReferenceEntity.findById(session.sub) } ?: return false
        return user.hasPassword && Passwords.verify(password.toCharArray(), user.password)
    }
    reauthentication.authenticationResponseJson?.let { response ->
        return verifyAssertion(response, user = session.sub) is AssertionVerification.Success
    }
    return false
}

/** Responds [Error.ReauthenticationFailed] unless [reauthentication] checks out, see [verifyReauthentication]. */
private suspend fun RoutingContext.assertReauthenticated(session: UserSession, reauthentication: Reauthentication?): Unit? {
    if (reauthentication == null || !verifyReauthentication(session, reauthentication)) {
        respondError(Error.ReauthenticationFailed())
        return null
    }
    return Unit
}

/**
 * How the logged-in user signs in: their passkeys and password. Every change that could lock the user out, or let a
 * stolen session keep the account, asks for the user's password or a passkey again (see [Reauthentication]).
 */
@OptIn(ExperimentalXmlUtilApi::class)
fun Route.securityRoutes() {
    get<Api.Profile.Security> {
        val session = getUserSessionOrFail() ?: return@get

        val info = Database {
            val user = UserReferenceEntity.findById(session.sub) ?: return@Database null
            SecurityInfo(
                hasPassword = user.hasPassword,
                passkeys = passkeysOf(session.sub)
                    .sortedBy { it.createdAt }
                    .map { PasskeyInfo(it.credentialId.value, it.name, it.createdAt, it.lastUsedAt) },
            )
        } ?: return@get respondError(Error.UserReferenceNotFound())

        call.respond(info)
    }

    post<Api.Profile.Passkeys.Options> {
        val session = getUserSessionOrFail() ?: return@post

        val challenge = generateWebAuthnChallenge().value.toBase64Url()
        // Keyed by sub: one passkey being created at a time per user, which is all this needs.
        RedisStoreMap.default.put(passkeyChallengeKey(session.sub), challenge, passkeyCreationTimeout.inWholeSeconds)

        val existing = Database { passkeysOf(session.sub).map { it.credentialId.value } }
        call.respond(
            CreationOptionsResponse(
                challenge = challenge,
                rp = RelyingParty(name = "CEA App", id = webAuthnRpId),
                user = WebAuthnUser(
                    id = session.subBase64Url(),
                    name = session.email,
                    displayName = session.fullName,
                ),
                authenticatorSelection = AuthenticatorSelection(userVerification = "required"),
                timeout = passkeyCreationTimeout.inWholeMilliseconds,
                excludeCredentials = existing.map { PublicKeyCredentialDescriptor(it) },
            )
        )
    }

    post<Api.Profile.Passkeys> {
        val session = getUserSessionOrFail() ?: return@post
        val request = runCatching { call.receiveNullable<AddPasskeyRequest>() }.getOrNull()
            ?: return@post respondError(Error.MissingArgument("registrationResponseJson"))

        val challenge = RedisStoreMap.default.get(passkeyChallengeKey(session.sub))
            ?: return@post respondError(Error.InvalidArgument("challenge", "Challenge expired or was never requested."))
        val credential = try {
            verifyRegistration(request.registrationResponseJson, challenge, userVerificationRequired = true)
        } catch (e: VerificationException) {
            return@post respondError(Error.InvalidArgument("registrationResponseJson", e.message))
        }

        val stored = Database {
            val user = UserReferenceEntity.findById(session.sub) ?: return@Database null
            UserCredentialRecordEntity.new(credential.credentialId) {
                this.user = user
                this.attestedCredentialData = credential.attestedCredentialData
                this.signCount = credential.signCount
                this.kind = CredentialKind.PASSKEY
                this.name = request.name?.trim()?.take(100)?.takeIf { it.isNotEmpty() }
                this.createdAt = Clock.System.now()
            }
        } ?: return@post respondError(Error.UserReferenceNotFound())
        RedisStoreMap.default.remove(passkeyChallengeKey(session.sub))

        recordAuthEvent(AuthEventType.PASSKEY_ADDED, session.email, null)
        call.respond(
            HttpStatusCode.Created,
            PasskeyInfo(stored.credentialId.value, stored.name, stored.createdAt, stored.lastUsedAt),
        )
    }

    rateLimit(RateLimits.AUTHENTICATION) {
        delete<Api.Profile.Passkeys.Id> { passkey ->
            val session = getUserSessionOrFail() ?: return@delete
            val request = runCatching { call.receiveNullable<ReauthenticatedRequest>() }.getOrNull()
            assertReauthenticated(session, request?.reauthentication) ?: return@delete

            val error = Database {
                val user = UserReferenceEntity.findById(session.sub) ?: return@Database Error.UserReferenceNotFound()
                val passkeys = passkeysOf(session.sub).toList()
                val record = passkeys.find { it.credentialId.value == passkey.id }
                    ?: return@Database Error.EntityNotFound(UserCredentialRecordEntity::class, passkey.id)
                // The account must keep a way to sign in.
                if (!user.hasPassword && passkeys.size == 1) return@Database Error.LastLoginMethod()
                record.delete()
                null
            }
            if (error != null) return@delete respondAuthError(AuthEventType.PASSKEY_REMOVED, session.email, error)

            recordAuthEvent(AuthEventType.PASSKEY_REMOVED, session.email, null)
            call.respond(HttpStatusCode.NoContent)
        }

        // Sets a password, or changes it.
        put<Api.Profile.Password> {
            val session = getUserSessionOrFail() ?: return@put
            val request = runCatching { call.receiveNullable<SetPasswordRequest>() }.getOrNull()
                ?: return@put respondError(Error.MissingArgument("newPassword"))
            assertReauthenticated(session, request.reauthentication) ?: return@put

            val newPassword = request.newPassword.trim().toCharArray()
            if (!Passwords.isSafe(newPassword)) {
                return@put respondAuthError(AuthEventType.PASSWORD_CHANGED, session.email, Error.PasswordNotSafeEnough())
            }

            val user = Database {
                UserReferenceEntity.findById(session.sub)?.also { it.password = Passwords.hash(newPassword) }
            } ?: return@put respondError(Error.UserReferenceNotFound())

            notifyPasswordChanged(user)
            recordAuthEvent(AuthEventType.PASSWORD_CHANGED, session.email, null)
            call.respond(HttpStatusCode.NoContent)
        }

        // Removes the password, so the account signs in with passkeys only.
        delete<Api.Profile.Password> {
            val session = getUserSessionOrFail() ?: return@delete
            val request = runCatching { call.receiveNullable<ReauthenticatedRequest>() }.getOrNull()
            assertReauthenticated(session, request?.reauthentication) ?: return@delete

            val error = Database {
                val user = UserReferenceEntity.findById(session.sub) ?: return@Database Error.UserReferenceNotFound()
                // The account must keep a way to sign in.
                if (passkeysOf(session.sub).empty()) return@Database Error.LastLoginMethod()
                user.password = ByteArray(0)
                null
            }
            if (error != null) return@delete respondAuthError(AuthEventType.PASSWORD_CHANGED, session.email, error)

            recordAuthEvent(AuthEventType.PASSWORD_CHANGED, session.email, null)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}

@OptIn(ExperimentalXmlUtilApi::class)
private fun RoutingContext.notifyPasswordChanged(user: UserReferenceEntity) {
    val locale = call.request.locale()
    Email.launch {
        Email.sendTemplate(
            to = listOf(MailerSendEmail(user.email, user.fullName)),
            template = EmailTemplate.PasswordChangedNotification,
            locale = locale,
            args = mapOf("userName" to user.fullName),
        )
    }
}
