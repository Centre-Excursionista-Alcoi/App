package org.centrexcursionistalcoi.app.security

import java.io.File
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import org.jetbrains.annotations.VisibleForTesting
import org.jetbrains.exposed.v1.crypt.Encryptor
import org.slf4j.LoggerFactory

object AES {
    @JvmStatic
    @VisibleForTesting
    var secretKey: SecretKey? = null

    @JvmStatic
    @VisibleForTesting
    var ivParameterSpec: IvParameterSpec? = null

    val encryptor = Encryptor(
        encryptFn = { encrypt(it.encodeToByteArray()).decodeToString() },
        decryptFn = { decrypt(it.encodeToByteArray()).decodeToString() },
        maxColLengthFn = { 512 },
    )

    private val logger = LoggerFactory.getLogger(this.javaClass)

    fun generateKey(keySize: Int = 256): SecretKey {
        val keyGenerator = KeyGenerator.getInstance("AES")
        keyGenerator.init(keySize)
        return keyGenerator.generateKey()
    }

    fun init() {
        val keysDir = File("/keys")

        val secretKeyFile = File(keysDir, "aes.key")
        if (!keysDir.exists()) keysDir.mkdirs()
        if (!secretKeyFile.exists()) {
            logger.info("AES secret key file not found, generating new key...")
            val secretKey = generateKey()
            secretKeyFile.writeBytes(secretKey.encoded)
            this.secretKey = secretKey
        } else {
            val keyBytes = secretKeyFile.readBytes()
            this.secretKey = SecretKeySpec(keyBytes, "AES")
        }

        val ivParameterFile = File(keysDir, "aes.iv")
        if (!ivParameterFile.exists()) {
            logger.info("AES IV file not found, generating new IV...")
            val iv = ByteArray(16)
            SecureRandom().nextBytes(iv)
            ivParameterFile.writeBytes(iv)
            this.ivParameterSpec = IvParameterSpec(iv)
        } else {
            val ivBytes = ivParameterFile.readBytes()
            this.ivParameterSpec = IvParameterSpec(ivBytes)
        }
    }

    fun encrypt(data: ByteArray): ByteArray {
        requireNotNull(secretKey) { "AES not initialized: secret key is null" }
        requireNotNull(ivParameterSpec) { "AES not initialized: IV is null" }

        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec)
        return cipher.doFinal(data)
    }

    fun decrypt(encryptedData: ByteArray): ByteArray {
        requireNotNull(secretKey) { "AES not initialized: secret key is null" }
        requireNotNull(ivParameterSpec) { "AES not initialized: IV is null" }

        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec)
        return cipher.doFinal(encryptedData)
    }
}
