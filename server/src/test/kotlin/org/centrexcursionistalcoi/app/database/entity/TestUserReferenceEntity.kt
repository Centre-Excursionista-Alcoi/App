package org.centrexcursionistalcoi.app.database.entity

import kotlin.test.Test
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.security.AES

class TestUserReferenceEntity {
    @Test
    fun test_femecv() {
        Database.initForTests()

        AES.secretKey = AES.generateKey()

        // Test that we can create a user with FEMECV credentials and no errors occur
        Database {
            UserReferenceEntity.new("abc") {
                nif = "12345678Z"
                fullName = "Test User"
                email = "mail@example.com"
                memberNumber = 1000u
                groups = emptyList()

                password = ByteArray(0)

                femecvUsername = "valid_username"
                femecvPassword = "valid_password"
            }
        }
    }
}
