package org.centrexcursionistalcoi.app.database.entity

import javax.crypto.spec.IvParameterSpec
import kotlin.test.Test
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.Database.TEST_URL
import org.centrexcursionistalcoi.app.security.AES

class TestUserReferenceEntity {
    @Test
    fun test_femecv() {
        Database.init(TEST_URL)

        AES.secretKey = AES.generateKey()
        AES.ivParameterSpec = IvParameterSpec(ByteArray(16) { 0 }) // Example IV

        // Test that we can create a user with FEMECV credentials and no errors occur
        Database {
            UserReferenceEntity.new("abc") {
                nif = "12345678Z"
                fullName = "Test User"
                email = "mail@example.com"
                memberNumber = 1000u
                groups = emptyList()

                femecvUsername = "valid_username"
                femecvPassword = "valid_password"
            }
        }
    }
}
