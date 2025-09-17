package org.centrexcursionistalcoi.app.security

import kotlin.test.Test
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

class TestFileReadWriteRules {
    @Test
    fun testCanBeReadBy() {
        val rules1 = FileReadWriteRules()
        assertTrue(rules1.canBeReadBy("user1", listOf("group1")))
        assertTrue(rules1.canBeReadBy(null, null))

        val rules2 = FileReadWriteRules(readUsers = listOf("user1", "user2"))
        assertTrue(rules2.canBeReadBy("user1", listOf("group1")))
        assertTrue(rules2.canBeReadBy("user2", null))
        assertFalse(rules2.canBeReadBy("user3", listOf("group1")))
        assertFalse(rules2.canBeReadBy(null, null))

        val rules3 = FileReadWriteRules(readGroups = listOf("group1", "group2"))
        assertTrue(rules3.canBeReadBy("user1", listOf("group1")))
        assertTrue(rules3.canBeReadBy("user2", listOf("group2", "group3")))
        assertFalse(rules3.canBeReadBy("user3", listOf("group3")))
        assertFalse(rules3.canBeReadBy(null, null))

        val rules4 = FileReadWriteRules(
            readUsers = listOf("user1"),
            readGroups = listOf("group1")
        )
        assertTrue(rules4.canBeReadBy("user1", listOf("group2")))
        assertTrue(rules4.canBeReadBy("user2", listOf("group1")))
        assertFalse(rules4.canBeReadBy("user2", listOf("group2")))
        assertFalse(rules4.canBeReadBy(null, null))
    }
}
