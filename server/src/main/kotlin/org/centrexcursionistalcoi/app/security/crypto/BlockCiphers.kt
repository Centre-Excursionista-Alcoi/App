package org.centrexcursionistalcoi.app.security.crypto

import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Raw block ciphers, for protocols that define their own key derivation. What the server stores is encrypted with [AES]
 * instead.
 */
object BlockCiphers {
    /**
     * AES-CBC with an all-zero IV and PKCS#7 padding. A fixed IV is only safe where the result isn't the secret
     * itself, like a key derived per operation (Redsys).
     */
    fun aesCbcZeroIv(key: ByteArray, data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(ByteArray(16)))
        return cipher.doFinal(data)
    }
}
