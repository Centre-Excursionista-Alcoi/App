package org.centrexcursionistalcoi.app.integration

import kotlin.test.Test
import kotlin.test.assertEquals
import org.centrexcursionistalcoi.app.ResourcesUtils
import org.centrexcursionistalcoi.app.integration.CEA.filterInvalid

class TestCEA {
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
}
