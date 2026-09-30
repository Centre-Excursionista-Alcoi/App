package org.centrexcursionistalcoi.app.security

import com.webauthn4j.converter.AttestationObjectConverter
import com.webauthn4j.converter.CollectedClientDataConverter
import com.webauthn4j.converter.util.ObjectConverter
import com.webauthn4j.data.attestation.AttestationObject
import com.webauthn4j.data.attestation.authenticator.AAGUID
import com.webauthn4j.data.attestation.authenticator.AttestedCredentialData
import com.webauthn4j.data.attestation.authenticator.AuthenticatorData
import com.webauthn4j.data.attestation.authenticator.EC2COSEKey
import com.webauthn4j.data.attestation.statement.COSEAlgorithmIdentifier
import com.webauthn4j.data.attestation.statement.NoneAttestationStatement
import com.webauthn4j.data.client.ClientDataType
import com.webauthn4j.data.client.CollectedClientData
import com.webauthn4j.data.client.Origin
import com.webauthn4j.data.client.challenge.DefaultChallenge
import com.webauthn4j.data.extension.authenticator.RegistrationExtensionAuthenticatorOutput
import java.nio.ByteBuffer
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.Signature
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.util.Base64
import org.centrexcursionistalcoi.app.AppLinks

/**
 * A WebAuthn authenticator in software, holding a single credential: produces the registration and authentication
 * responses a platform authenticator would, signed with a real key, so the server's verification runs for real.
 */
class SoftwareAuthenticator {
    private val objectConverter = ObjectConverter()
    private val base64Url = Base64.getUrlEncoder().withoutPadding()
    private val keyPair = KeyPairGenerator.getInstance("EC")
        .apply { initialize(ECGenParameterSpec("secp256r1")) }
        .generateKeyPair()
    private val rawCredentialId = ByteArray(16).also { SecureRandom().nextBytes(it) }
    private var signCount = 0L

    /** The credential's id, Base64Url without padding, as the server stores it. */
    val credentialId: String = base64Url.encodeToString(rawCredentialId)

    private val rpIdHash get() = MessageDigest.getInstance("SHA-256").digest(AppLinks.host.toByteArray())

    private fun flags(userVerified: Boolean, attestedCredentialData: Boolean): Byte {
        var flags = AuthenticatorData.BIT_UP.toInt()
        if (userVerified) flags = flags or AuthenticatorData.BIT_UV.toInt()
        if (attestedCredentialData) flags = flags or AuthenticatorData.BIT_AT.toInt()
        return flags.toByte()
    }

    private fun clientData(type: ClientDataType, challenge: String, origin: String): ByteArray =
        CollectedClientDataConverter(objectConverter).convertToBytes(
            CollectedClientData(type, DefaultChallenge(Base64.getUrlDecoder().decode(challenge)), Origin.create(origin), null)
        )

    /**
     * Creates the credential, for the options with [challenge].
     * @param userVerified Whether the user unlocked the device: always for passkeys, never for restore keys.
     */
    fun registrationResponseJson(challenge: String, origin: String, userVerified: Boolean): String {
        val authenticatorData = AuthenticatorData<RegistrationExtensionAuthenticatorOutput>(
            rpIdHash,
            flags(userVerified, attestedCredentialData = true),
            signCount,
            AttestedCredentialData(
                AAGUID.ZERO,
                rawCredentialId,
                EC2COSEKey.create(keyPair.public as ECPublicKey, COSEAlgorithmIdentifier.ES256),
            ),
        )
        val attestationObject = AttestationObjectConverter(objectConverter)
            .convertToBytes(AttestationObject(authenticatorData, NoneAttestationStatement()))
        val clientData = clientData(ClientDataType.WEBAUTHN_CREATE, challenge, origin)

        return """{"id":"$credentialId","rawId":"$credentialId","type":"public-key","response":{"clientDataJSON":"${base64Url.encodeToString(clientData)}","attestationObject":"${base64Url.encodeToString(attestationObject)}"},"clientExtensionResults":{}}"""
    }

    /** Signs in with the credential, for the options with [challenge]. */
    fun authenticationResponseJson(challenge: String, origin: String, userVerified: Boolean): String {
        signCount++
        val authenticatorData = ByteBuffer.allocate(37)
            .put(rpIdHash)
            .put(flags(userVerified, attestedCredentialData = false))
            .putInt(signCount.toInt())
            .array()
        val clientData = clientData(ClientDataType.WEBAUTHN_GET, challenge, origin)
        val signature = Signature.getInstance("SHA256withECDSA").run {
            initSign(keyPair.private)
            update(authenticatorData)
            update(MessageDigest.getInstance("SHA-256").digest(clientData))
            sign()
        }

        return """{"id":"$credentialId","rawId":"$credentialId","type":"public-key","response":{"clientDataJSON":"${base64Url.encodeToString(clientData)}","authenticatorData":"${base64Url.encodeToString(authenticatorData)}","signature":"${base64Url.encodeToString(signature)}"},"clientExtensionResults":{}}"""
    }
}
