package org.centrexcursionistalcoi.app.security

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TestPasswords {
    @Test
    fun `test isSafe`() {
        // short
        assertFalse { Passwords.isSafe("Pass123".toCharArray()) }
        // no lowercase
        assertFalse { Passwords.isSafe("PASSWORD123".toCharArray()) }
        // no uppercase
        assertFalse { Passwords.isSafe("password123".toCharArray()) }
        // no digits
        assertFalse { Passwords.isSafe("Password".toCharArray()) }
        // valid
        assertTrue { Passwords.isSafe("ValidPass123".toCharArray()) }
    }
}
