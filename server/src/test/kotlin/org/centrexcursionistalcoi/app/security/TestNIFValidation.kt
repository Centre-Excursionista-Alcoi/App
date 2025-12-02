package org.centrexcursionistalcoi.app.security

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TestNIFValidation {
    @Test
    fun `test calculate letter`() {
        assertEquals(null, NIFValidation.calculateLetter("invalid"))
        assertEquals(null, NIFValidation.calculateLetter("1234567"))
        assertEquals('X', NIFValidation.calculateLetter("87654321"))
    }

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
