package org.centrexcursionistalcoi.app.payment.redsys

import java.util.Base64
import org.centrexcursionistalcoi.app.security.crypto.BlockCiphers
import org.centrexcursionistalcoi.app.security.crypto.Hashing
import org.centrexcursionistalcoi.app.security.crypto.toBase64Url

/**
 * Signs and verifies what is exchanged with Redsys (`HMAC_SHA512_V2`): the key of an operation is the terminal's secret
 * key encrypted with AES-CBC (zero IV) over the order number, and the signature is the HMAC-SHA512 of the Base64Url
 * `Ds_MerchantParameters`, in Base64Url, keyed with the Base64 *text* of that derived key (not its bytes).
 */
object RedsysSignature {
    const val VERSION = "HMAC_SHA512_V2"

    /** The key to sign the operation [order] with. */
    fun deriveKey(secretKey: String, order: String): ByteArray {
        // The first 16 characters of the terminal's key
        val key = secretKey.toByteArray().copyOf(16)
        return BlockCiphers.aesCbcZeroIv(key, order.toByteArray())
    }

    /** The `Ds_Signature` for [parameters] (`Ds_MerchantParameters`) of the operation [order]. */
    fun sign(secretKey: String, order: String, parameters: String): String =
        Hashing.hmacSha512(
            key = Base64.getEncoder().encodeToString(deriveKey(secretKey, order)).toByteArray(),
            data = parameters.toByteArray(),
        ).toBase64Url()

    /** Whether [signature] is the one of [parameters], whichever Base64 alphabet and padding it is written in. */
    fun verify(secretKey: String, order: String, parameters: String, signature: String): Boolean =
        Hashing.constantTimeEquals(normalize(sign(secretKey, order, parameters)), normalize(signature))

    private fun normalize(signature: String) = signature.trim().replace('+', '-').replace('/', '_').trimEnd('=')
}
