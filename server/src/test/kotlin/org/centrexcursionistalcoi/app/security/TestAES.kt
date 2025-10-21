package org.centrexcursionistalcoi.app.security

import javax.crypto.spec.IvParameterSpec
import kotlin.test.Test

class TestAES {
    @Test
    fun test_encrypt_decrypt() {
        AES.secretKey = AES.generateKey()
        AES.ivParameterSpec = IvParameterSpec(ByteArray(16) { 0 }) // Example IV

        val data = "Hello, World!".toByteArray()
        val encryptedData = AES.encrypt(data)
        val decryptedData = AES.decrypt(encryptedData)
        assert(data.contentEquals(decryptedData)) { "Decrypted data does not match original" }
    }
}
