package org.centrexcursionistalcoi.app.security

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TestNIFValidation {
    @Test
    fun `test dni`() {
        assertFalse { NIFValidation.validate("invalid") }
        assertFalse { NIFValidation.validate("1234567A") }
        assertFalse { NIFValidation.validate("876543215Y") }
        assertTrue { NIFValidation.validate("87654321X") }
    }

    @Test
    fun `test nie`() {
        assertFalse { NIFValidation.validate("invalid") }
        assertFalse { NIFValidation.validate("X1234567A") }
        assertFalse { NIFValidation.validate("Z876543215Y") }
        assertTrue { NIFValidation.validate("X8765432V") }
    }
}
