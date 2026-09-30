package org.centrexcursionistalcoi.app.plugins

import com.webauthn4j.verifier.exception.VerificationException
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receiveNullable
import io.ktor.server.resources.post
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import org.centrexcursionistalcoi.app.data.Member
import org.centrexcursionistalcoi.app.data.PasskeyRegistrationOptionsRequest
import org.centrexcursionistalcoi.app.data.PasskeyRegistrationRequest
import org.centrexcursionistalcoi.app.data.RegistrationCodeRequest
import org.centrexcursionistalcoi.app.data.webauthn.AuthenticatorSelection
import org.centrexcursionistalcoi.app.data.webauthn.CreationOptionsResponse
import org.centrexcursionistalcoi.app.data.webauthn.RelyingParty
import org.centrexcursionistalcoi.app.data.webauthn.WebAuthnUser
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.MemberEntity
import org.centrexcursionistalcoi.app.database.entity.UserCredentialRecordEntity
import org.centrexcursionistalcoi.app.database.entity.UserReferenceEntity
import org.centrexcursionistalcoi.app.database.entity.generateUserSub
import org.centrexcursionistalcoi.app.database.table.AuthEventType
import org.centrexcursionistalcoi.app.database.table.AuthSessionMethod
import org.centrexcursionistalcoi.app.database.table.CredentialKind
import org.centrexcursionistalcoi.app.database.table.Members
import org.centrexcursionistalcoi.app.error.Error
import org.centrexcursionistalcoi.app.error.respondError
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.notifications.Email
import org.centrexcursionistalcoi.app.notifications.EmailTemplate
import org.centrexcursionistalcoi.app.notifications.email.mailersend.MailerSendEmail
import org.centrexcursionistalcoi.app.routes.Api
import org.centrexcursionistalcoi.app.security.AuthTokens
import org.centrexcursionistalcoi.app.security.ClientInfo
import org.centrexcursionistalcoi.app.security.EmailValidation
import org.centrexcursionistalcoi.app.security.RegistrationCodes
import org.centrexcursionistalcoi.app.security.generateWebAuthnChallenge
import org.centrexcursionistalcoi.app.security.registrationChallenge
import org.centrexcursionistalcoi.app.security.toBase64Url
import org.centrexcursionistalcoi.app.security.verifyRegistration
import org.centrexcursionistalcoi.app.security.webAuthnRpId
import org.centrexcursionistalcoi.app.storage.RedisStoreMap
import org.centrexcursionistalcoi.app.translation.locale
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.upperCase

/** How long a new account's passkey can take to be created, once its options are handed out. */
private val passkeyRegistrationTimeout = 10.minutes

sealed interface RegistrationEligibility {
    class Eligible(val member: MemberEntity) : RegistrationEligibility
    class Rejected(val error: Error) : RegistrationEligibility
}

/**
 * Whether an account can be registered with [email]: it must be a valid email, of an active member, with no
 * account yet.
 * @param email Uppercase.
 */
fun registrationEligibility(email: String): RegistrationEligibility {
    if (!EmailValidation.validate(email)) return RegistrationEligibility.Rejected(Error.InvalidArgument("email"))

    val existingReference = Database { UserReferenceEntity.findByEmail(email) }
    if (existingReference != null) return RegistrationEligibility.Rejected(Error.UserAlreadyRegistered())

    val member = Database { MemberEntity.find { Members.email.upperCase() eq email }.limit(1).firstOrNull() }
        ?: return RegistrationEligibility.Rejected(Error.EmailNotFound())
    if (member.status != Member.Status.ACTIVE) return RegistrationEligibility.Rejected(Error.MemberIsNotActive())

    return RegistrationEligibility.Eligible(member)
}

/** A new account whose passkey is being created, keyed by the challenge of its options. */
@Serializable
private class PendingPasskeyRegistration(val email: String, val sub: String)

private fun pendingPasskeyRegistrationKey(challenge: String) = "passkey_registration:$challenge"

/**
 * Registration steps other than the password registration itself (`POST /register`): the emailed code that proves
 * the email is the user's, needed by every registration, and registering an account that signs in with a passkey.
 */
@OptIn(ExperimentalXmlUtilApi::class)
fun Route.registrationRoutes() {
    post<Api.Register.Verification> {
        val request = runCatching { call.receiveNullable<RegistrationCodeRequest>() }.getOrNull()
            ?: return@post respondError(Error.MissingArgument("email"))
        val email = request.email.trim().uppercase()

        val member = when (val eligibility = registrationEligibility(email)) {
            is RegistrationEligibility.Rejected ->
                return@post respondAuthError(AuthEventType.REGISTRATION_CODE, email, eligibility.error)
            is RegistrationEligibility.Eligible -> eligibility.member
        }

        val code = RegistrationCodes.create(email)
        val locale = call.request.locale()
        val fullName = member.fullName
        Email.launch {
            Email.sendTemplate(
                to = listOf(MailerSendEmail(email, fullName)),
                template = EmailTemplate.RegistrationCode,
                locale = locale,
                args = mapOf(
                    "userName" to fullName,
                    "code" to code,
                ),
            )
        }

        recordAuthEvent(AuthEventType.REGISTRATION_CODE, email, null)
        call.respond(HttpStatusCode.Accepted)
    }

    // The options to create the passkey of a new account. The code is checked, but not used up: it's needed again
    // to register the passkey.
    post<Api.Register.Passkey.Options> {
        val request = runCatching { call.receiveNullable<PasskeyRegistrationOptionsRequest>() }.getOrNull()
            ?: return@post respondError(Error.MissingArgument("email"))
        val email = request.email.trim().uppercase()

        val member = when (val eligibility = registrationEligibility(email)) {
            is RegistrationEligibility.Rejected ->
                return@post respondAuthError(AuthEventType.REGISTER, email, eligibility.error)
            is RegistrationEligibility.Eligible -> eligibility.member
        }
        if (!RegistrationCodes.check(email, request.code)) {
            return@post respondAuthError(AuthEventType.REGISTER, email, Error.InvalidVerificationCode())
        }

        // The account's id is chosen now, since it's the passkey's user handle.
        val sub = generateUserSub()
        val challenge = generateWebAuthnChallenge().value.toBase64Url()
        RedisStoreMap.default.put(
            pendingPasskeyRegistrationKey(challenge),
            json.encodeToString(PendingPasskeyRegistration.serializer(), PendingPasskeyRegistration(email, sub)),
            passkeyRegistrationTimeout.inWholeSeconds,
        )

        call.respond(
            CreationOptionsResponse(
                challenge = challenge,
                rp = RelyingParty(name = "CEA App", id = webAuthnRpId),
                user = WebAuthnUser(
                    id = sub.toByteArray().toBase64Url(),
                    // What the user sees to pick the passkey: their email, as they know their account by it.
                    name = member.email ?: email,
                    displayName = member.fullName,
                ),
                authenticatorSelection = AuthenticatorSelection(userVerification = "required"),
                timeout = passkeyRegistrationTimeout.inWholeMilliseconds,
            )
        )
    }

    // Registers the new account with the passkey created for the options above, and logs it in.
    post<Api.Register.Passkey> {
        val request = runCatching { call.receiveNullable<PasskeyRegistrationRequest>() }.getOrNull()
            ?: return@post respondError(Error.MissingArgument("registrationResponseJson"))
        val email = request.email.trim().uppercase()

        val pending = try {
            registrationChallenge(request.registrationResponseJson)
                ?.let { RedisStoreMap.default.remove(pendingPasskeyRegistrationKey(it))?.let { value -> it to value } }
        } catch (_: Exception) {
            null
        } ?: return@post respondAuthError(
            AuthEventType.REGISTER, email,
            Error.InvalidArgument("challenge", "Challenge expired or was never requested."),
        )
        val (challenge, pendingJson) = pending
        val registration = json.decodeFromString(PendingPasskeyRegistration.serializer(), pendingJson)
        if (registration.email != email) {
            return@post respondAuthError(AuthEventType.REGISTER, email, Error.InvalidArgument("email"))
        }

        val member = when (val eligibility = registrationEligibility(email)) {
            is RegistrationEligibility.Rejected ->
                return@post respondAuthError(AuthEventType.REGISTER, email, eligibility.error)
            is RegistrationEligibility.Eligible -> eligibility.member
        }

        val credential = try {
            verifyRegistration(request.registrationResponseJson, challenge, userVerificationRequired = true)
        } catch (e: VerificationException) {
            return@post respondAuthError(AuthEventType.REGISTER, email, Error.InvalidArgument("registrationResponseJson", e.message))
        }

        // Only used up once everything else checked out, so a failed attempt can be retried with the same code.
        if (!RegistrationCodes.check(email, request.code, consume = true)) {
            return@post respondAuthError(AuthEventType.REGISTER, email, Error.InvalidVerificationCode())
        }

        val tokens = Database {
            val user = member.insertUser(hashedPassword = null, sub = registration.sub)
            UserCredentialRecordEntity.new(credential.credentialId) {
                this.user = user
                this.attestedCredentialData = credential.attestedCredentialData
                this.signCount = credential.signCount
                this.kind = CredentialKind.PASSKEY
                this.name = request.name?.trim()?.take(100)?.takeIf { it.isNotEmpty() }
                this.createdAt = Clock.System.now()
                this.lastUsedAt = Clock.System.now()
            }
            AuthTokens.startSession(user, AuthSessionMethod.PASSKEY, ClientInfo.from(call))
        }

        recordAuthEvent(AuthEventType.REGISTER, email, null)
        recordAuthEvent(AuthEventType.PASSKEY_ADDED, email, null)
        respondTokens(tokens)
    }
}
