package org.centrexcursionistalcoi.app.integration

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.centrexcursionistalcoi.app.ResourcesUtils
import org.centrexcursionistalcoi.app.integration.CEA.filterInvalid

class TestCEA {
    @Test
    fun `test download`() = runTest {
        System.getenv("CEA_USERNAME") ?: return@runTest println("Skipping CEA download test. CEA_USERNAME not set.")
        System.getenv("CEA_PASSWORD") ?: return@runTest println("Skipping CEA download test. CEA_PASSWORD not set.")

        val csv = CEA.download()
        val lines = csv.lines()
        assertTrue("Malformed CSV Response.\n\tLine 1: ${lines[0]}") { lines[0].contains("Núm. soci/a") }
    }

    @Test
    fun `test parse`() {
        val csv = ResourcesUtils.bytesFromResource("/socis-cea.csv").decodeToString()
        val members = CEA.parse(csv).filterInvalid()
        assertEquals(2, members.size)
        members[0].let { member ->
            assertEquals(1956, member.number)
            assertEquals("Alta", member.status)
            assertEquals("Usuari Exemple 1", member.fullName)
            assertEquals("12345678Z", member.nif)
            assertEquals("mail1@example.com", member.email)
        }
        members[1].let { member ->
            assertEquals(2943, member.number)
            assertEquals("Alta", member.status)
            assertEquals("Usuari Exemple 2", member.fullName)
            assertEquals("87654321X", member.nif)
            assertEquals("mail2@example.com", member.email)
        }
    }

    @Test
    fun `test cleanupMembersNIF`() {
        val members = listOf(
            CEA.Member(1, "Alta", "Test User 1", "12345678Z", "test1@example.com"),
            CEA.Member(2, "Alta", "Test User 2", "87654321 ", "test2@example.com"),
        )
        val cleanedMembers = CEA.cleanupMembersNIF(members)
        assertEquals("12345678Z", cleanedMembers[0].nif)
        assertEquals("87654321X", cleanedMembers[1].nif)
    }
}
