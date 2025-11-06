package org.centrexcursionistalcoi.app

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TestVersion {
    @Test
    fun `test version follows semver`() {
        val parts = version.split(".")
        assertEquals(3, parts.size, "Version ($version) should have three parts separated by dots")
        parts.forEachIndexed { idx, part ->
            val number = part.toIntOrNull()
            assertNotNull(number, "Each part of the version should be an integer")
            assertTrue("Each part of the version should be a positive integer: index $idx => $part") { number >= 0 }
        }
    }
}
