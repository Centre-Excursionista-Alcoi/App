package org.centrexcursionistalcoi.app.security

import java.io.File
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import org.jetbrains.annotations.TestOnly
import org.jetbrains.annotations.VisibleForTesting
import org.jetbrains.exposed.v1.crypt.Encryptor
import org.slf4j.LoggerFactory

object AES {
    @JvmStatic
    @VisibleForTesting
    var secretKey: SecretKey? = null

    @JvmStatic
    @VisibleForTesting
    private var ivParameterSpec: IvParameterSpec? = null

    val encryptor = Encryptor(
        encryptFn = { data ->
            val encrypted = encrypt(data.toByteArray())
            Base64.getEncoder().encodeToString(encrypted)
        },
        decryptFn = { data ->
            val decoded = Base64.getDecoder().decode(data)
            val decrypted = decrypt(decoded)
            String(decrypted)
        },
        maxColLengthFn = { 512 },
    )

    private val logger = LoggerFactory.getLogger(this.javaClass)

    fun generateKey(keySize: Int = 256): SecretKey {
        val keyGenerator = KeyGenerator.getInstance("AES")
        keyGenerator.init(keySize)
        return keyGenerator.generateKey()
    }

    fun isInitialized() = secretKey != null

    fun init() {
        val keysDir = File(System.getenv("KEYS_PATH") ?: "/keys")

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

        // Legacy IV loading for migration
        val ivParameterFile = File(keysDir, "aes.iv")
        if (!ivParameterFile.exists()) {
            // No legacy IV file found. If this is a new installation, we don't need it.
            // If it's an existing installation, this might be an issue if we need to decrypt old data.
            // For now, we'll just log it.
            logger.warn("Legacy AES IV file not found.")
        } else {
            val ivBytes = ivParameterFile.readBytes()
            this.ivParameterSpec = IvParameterSpec(ivBytes)
        }
    }

    /**
     * Initializes AES with test keys. Only to be used in tests.
     */
    @TestOnly
    fun initForTests() {
        secretKey = generateKey()

        // This is required for legacy V5 migration checks
        ivParameterSpec = IvParameterSpec(ByteArray(16)) // Example IV
    }

    fun encrypt(data: ByteArray): ByteArray {
        requireNotNull(secretKey) { "AES not initialized: secret key is null" }

        // Generate a random IV
        val iv = ByteArray(16)
        SecureRandom().nextBytes(iv)
        val ivSpec = IvParameterSpec(iv)

        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)
        val encrypted = cipher.doFinal(data)

        // Return IV + Encrypted Data
        return iv + encrypted
    }

    fun decrypt(encryptedData: ByteArray): ByteArray {
        requireNotNull(secretKey) { "AES not initialized: secret key is null" }

        // Extract IV
        if (encryptedData.size < 16) error("Invalid encrypted data: too short to contain IV")
        val iv = encryptedData.copyOfRange(0, 16)
        val ivSpec = IvParameterSpec(iv)

        // Extract actual encrypted data
        val data = encryptedData.copyOfRange(16, encryptedData.size)

        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
        return cipher.doFinal(data)
    }

    @Deprecated("Use encrypt(ByteArray) instead. This method is only for testing migration.")
    @VisibleForTesting
    fun encryptLegacy(data: ByteArray): ByteArray {
        requireNotNull(secretKey) { "AES not initialized: secret key is null" }
        requireNotNull(ivParameterSpec) { "AES not initialized: IV is null" }

        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec)
        return cipher.doFinal(data)
    }

    @Deprecated("Use decrypt(ByteArray) instead. This method is only for migration.")
    fun decryptLegacy(encryptedData: ByteArray): ByteArray {
        requireNotNull(secretKey) { "AES not initialized: secret key is null" }
        requireNotNull(ivParameterSpec) { "AES not initialized: IV is null" }

        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec)
        return cipher.doFinal(encryptedData)
    }
}
