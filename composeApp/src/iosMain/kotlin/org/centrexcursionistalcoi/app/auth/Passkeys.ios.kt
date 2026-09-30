@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package org.centrexcursionistalcoi.app.auth

import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.io.encoding.Base64
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.cValue
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.centrexcursionistalcoi.app.json
import org.koin.core.annotation.Singleton
import platform.AuthenticationServices.ASAuthorization
import platform.AuthenticationServices.ASAuthorizationController
import platform.AuthenticationServices.ASAuthorizationControllerDelegateProtocol
import platform.AuthenticationServices.ASAuthorizationControllerPresentationContextProvidingProtocol
import platform.AuthenticationServices.ASAuthorizationErrorCanceled
import platform.AuthenticationServices.ASAuthorizationPasswordProvider
import platform.AuthenticationServices.ASAuthorizationPlatformPublicKeyCredentialAssertion
import platform.AuthenticationServices.ASAuthorizationPlatformPublicKeyCredentialProvider
import platform.AuthenticationServices.ASAuthorizationPlatformPublicKeyCredentialRegistration
import platform.AuthenticationServices.ASAuthorizationPlatformPublicKeyCredentialRegistrationRequest
import platform.AuthenticationServices.ASAuthorizationPlatformPublicKeyCredentialRegistrationRequestStyle
import platform.AuthenticationServices.ASAuthorizationPublicKeyCredentialUserVerificationPreferenceRequired
import platform.AuthenticationServices.ASAuthorizationRequest
import platform.AuthenticationServices.ASPasswordCredential
import platform.AuthenticationServices.ASPresentationAnchor
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSOperatingSystemVersion
import platform.Foundation.NSProcessInfo
import platform.Foundation.create
import platform.UIKit.UIApplication
import platform.UIKit.UIDevice
import platform.UIKit.UIWindow
import platform.darwin.NSObject
import platform.posix.memcpy

private val base64Url = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL)

private fun String.base64UrlToNSData(): NSData = base64Url.decode(this).toNSData()

private fun ByteArray.toNSData(): NSData = usePinned { pinned ->
    NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
}

private fun NSData.toBase64Url(): String {
    val bytes = ByteArray(length.toInt())
    if (bytes.isNotEmpty()) bytes.usePinned { pinned -> memcpy(pinned.addressOf(0), this.bytes, length) }
    return base64Url.encode(bytes).trimEnd('=')
}

/** Resumes with the authorization, `null` if the user cancelled. */
private class AuthorizationDelegate(
    private val onResult: (Result<ASAuthorization?>) -> Unit,
) : NSObject(), ASAuthorizationControllerDelegateProtocol, ASAuthorizationControllerPresentationContextProvidingProtocol {
    override fun authorizationController(controller: ASAuthorizationController, didCompleteWithAuthorization: ASAuthorization) {
        onResult(Result.success(didCompleteWithAuthorization))
    }

    override fun authorizationController(controller: ASAuthorizationController, didCompleteWithError: NSError) {
        if (didCompleteWithError.code == ASAuthorizationErrorCanceled) {
            onResult(Result.success(null))
        } else {
            onResult(Result.failure(PasskeyException(didCompleteWithError.localizedDescription)))
        }
    }

    override fun presentationAnchorForAuthorizationController(controller: ASAuthorizationController): ASPresentationAnchor {
        @Suppress("UNCHECKED_CAST")
        val windows = UIApplication.sharedApplication.windows as List<UIWindow>
        return windows.firstOrNull { it.isKeyWindow() } ?: windows.first()
    }
}

@Singleton
actual class Passkeys {
    // Passkeys need iOS 16, the app's minimum.
    actual val isSupported: Boolean = true

    actual val deviceName: String get() = UIDevice.currentDevice.model

    /** The controller and its delegate, which the controller only holds weakly, while a request is running. */
    private var running: Pair<ASAuthorizationController, AuthorizationDelegate>? = null

    private suspend fun perform(requests: List<ASAuthorizationRequest>): ASAuthorization? =
        withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                val controller = ASAuthorizationController(authorizationRequests = requests)
                val delegate = AuthorizationDelegate { result ->
                    running = null
                    if (continuation.isActive) {
                        result.fold({ continuation.resume(it) }, { continuation.resumeWithException(it) })
                    }
                }
                controller.delegate = delegate
                controller.presentationContextProvider = delegate
                running = controller to delegate
                continuation.invokeOnCancellation { controller.cancel() }
                controller.performRequests()
            }
        }

    private fun registrationRequest(
        requestJson: String,
        style: ASAuthorizationPlatformPublicKeyCredentialRegistrationRequestStyle =
            ASAuthorizationPlatformPublicKeyCredentialRegistrationRequestStyle.ASAuthorizationPlatformPublicKeyCredentialRegistrationRequestStyleStandard,
    ): ASAuthorizationPlatformPublicKeyCredentialRegistrationRequest {
        val options = json.parseToJsonElement(requestJson).jsonObject
        val rp = options.getValue("rp").jsonObject
        val user = options.getValue("user").jsonObject
        val provider = ASAuthorizationPlatformPublicKeyCredentialProvider(relyingPartyIdentifier = rp.string("id"))
        return provider.createCredentialRegistrationRequestWithChallenge(
            challenge = options.string("challenge").base64UrlToNSData(),
            name = user.string("name"),
            userID = user.string("id").base64UrlToNSData(),
            requestStyle = style,
        ).apply {
            userVerificationPreference = ASAuthorizationPublicKeyCredentialUserVerificationPreferenceRequired
            // The options' excludeCredentials can't be passed on: only browsers can exclude credentials. iOS already
            // replaces the passkey an account has on the device, instead of adding a second one.
        }
    }

    private fun registrationResponseJson(registration: ASAuthorizationPlatformPublicKeyCredentialRegistration): String {
        val id = registration.credentialID.toBase64Url()
        val attestationObject = registration.rawAttestationObject
            ?: throw PasskeyException("The passkey was created without an attestation object")
        return buildJsonObject {
            put("id", id)
            put("rawId", id)
            put("type", "public-key")
            put("authenticatorAttachment", "platform")
            put("response", buildJsonObject {
                put("clientDataJSON", registration.rawClientDataJSON.toBase64Url())
                put("attestationObject", attestationObject.toBase64Url())
            })
            put("clientExtensionResults", JsonObject(emptyMap()))
        }.toString()
    }

    actual suspend fun create(requestJson: String): String? {
        val authorization = perform(listOf(registrationRequest(requestJson))) ?: return null
        val registration = authorization.credential as? ASAuthorizationPlatformPublicKeyCredentialRegistration
            ?: throw PasskeyException("Unexpected credential: ${authorization.credential}")
        return registrationResponseJson(registration)
    }

    actual suspend fun createAutomatically(requestJson: String): String? {
        // The automatic passkey upgrade is iOS 18's.
        if (!isAtLeast(18, 0)) return null
        return try {
            val request = registrationRequest(
                requestJson,
                ASAuthorizationPlatformPublicKeyCredentialRegistrationRequestStyle.ASAuthorizationPlatformPublicKeyCredentialRegistrationRequestStyleConditional,
            )
            val registration = perform(listOf(request))?.credential as? ASAuthorizationPlatformPublicKeyCredentialRegistration
            registration?.let(::registrationResponseJson)
        } catch (_: PasskeyException) {
            // Expected whenever iOS doesn't upgrade the account on its own.
            null
        }
    }

    actual suspend fun signIn(requestJson: String, includePasswords: Boolean): SavedCredential? {
        val options = json.parseToJsonElement(requestJson).jsonObject
        val passkeyRequest = ASAuthorizationPlatformPublicKeyCredentialProvider(relyingPartyIdentifier = options.string("rpId"))
            .createCredentialAssertionRequestWithChallenge(options.string("challenge").base64UrlToNSData())
            .apply { userVerificationPreference = ASAuthorizationPublicKeyCredentialUserVerificationPreferenceRequired }
        val passwordRequest = ASAuthorizationPasswordProvider().createRequest().takeIf { includePasswords }

        val authorization = perform(listOfNotNull(passkeyRequest, passwordRequest)) ?: return null
        return when (val credential = authorization.credential) {
            is ASAuthorizationPlatformPublicKeyCredentialAssertion -> {
                val id = credential.credentialID.toBase64Url()
                SavedCredential.Passkey(
                    buildJsonObject {
                        put("id", id)
                        put("rawId", id)
                        put("type", "public-key")
                        put("authenticatorAttachment", "platform")
                        put("response", buildJsonObject {
                            put("clientDataJSON", credential.rawClientDataJSON.toBase64Url())
                            put("authenticatorData", credential.rawAuthenticatorData?.toBase64Url() ?: throw PasskeyException("No authenticator data"))
                            put("signature", credential.signature?.toBase64Url() ?: throw PasskeyException("No signature"))
                            credential.userID?.let { put("userHandle", it.toBase64Url()) }
                        })
                        put("clientExtensionResults", JsonObject(emptyMap()))
                    }.toString()
                )
            }
            is ASPasswordCredential -> SavedCredential.Password(credential.user, credential.password)
            else -> throw PasskeyException("Unexpected credential: $credential")
        }
    }

    // iOS offers to save a password on its own, when its fields are marked as such (see PasswordFormField).
    actual suspend fun savePassword(email: String, password: String) {}

    private fun isAtLeast(major: Int, minor: Int): Boolean = NSProcessInfo.processInfo.isOperatingSystemAtLeastVersion(
        cValue<NSOperatingSystemVersion> { majorVersion = major.toLong(); minorVersion = minor.toLong(); patchVersion = 0 }
    )
}

private fun JsonObject.string(key: String): String = getValue(key).jsonPrimitive.content
