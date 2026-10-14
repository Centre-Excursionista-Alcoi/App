package org.centrexcursionistalcoi.app.integration

import kotlinx.coroutines.test.runTest
import org.centrexcursionistalcoi.app.ResourcesUtils
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.MemberEntity
import org.centrexcursionistalcoi.app.database.entity.UserReferenceEntity
import org.centrexcursionistalcoi.app.integration.CEA.Member
import org.centrexcursionistalcoi.app.integration.CEA.filterInvalid
import org.centrexcursionistalcoi.app.security.crypto.AES
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TestCEA {
    @AfterTest
    fun cleanup() = Database.clear()

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
            assertEquals(1956u, member.number)
            assertEquals("Alta", member.status)
            assertEquals("Usuari Exemple 1", member.fullName)
            assertEquals("12345678Z", member.nif)
            assertEquals("mail1@example.com", member.email)
        }
        members[1].let { member ->
            assertEquals(2943u, member.number)
            assertEquals("Alta", member.status)
            assertEquals("Usuari Exemple 2", member.fullName)
            assertEquals("87654321X", member.nif)
            assertEquals("mail2@example.com", member.email)
        }
    }

    @Test
    fun `test cleanupMembersNIF`() {
        val members = listOf(
            Member(1u, "Alta", "Test User 1", "12345678Z", "test1@example.com"),
            Member(2u, "Alta", "Test User 2", "87654321 ", "test2@example.com"),
        )
        val cleanedMembers = CEA.cleanupMembersNIF(members)
        assertEquals("12345678Z", cleanedMembers[0].nif)
        assertEquals("87654321X", cleanedMembers[1].nif)
    }

    @Test
    fun `test synchronizeWithDatabase`() = runTest {
        AES.initForTests()
        Database.initForTests()

        Database {
            // This user will be removed (not in CEA data)
            MemberEntity.new(123u) {
                fullName = "Previous User"
            }
            // This user will be disabled (status not "Alta")
            UserReferenceEntity.new("000000") {
                nif = "00000000A"
                memberNumber = 2u
                fullName = "Disabled User"
                email = "user2@example.com"
                password = ByteArray(0)
                isDisabled = false
            }
            // This user will be enabled (status "Alta"), email will also be updated
            UserReferenceEntity.new("000001") {
                nif = "11111111H"
                memberNumber = 4u
                fullName = "Test User 4"
                email = "user4@example.com"
                password = ByteArray(0)
                isDisabled = false
            }
        }

        CEA.synchronizeWithDatabase(
            listOf(
                Member(1u, "Alta", "Test User 1", "12345678Z", "test1@example.com"),
                Member(2u, "Baixa", "Test User 2", "87654321X", "test2@example.com"),
                Member(3u, "Pendent", "Test User 3", "00000000", "test3@example.com"),
                Member(4u, "Alta", "Test User 4", "11111111H", "test4@example.com"),
            )
        )

        val members = Database { MemberEntity.all().toList() }
        assertEquals(4, members.size)
        assertEquals("12345678Z", members[0].nif)
        assertEquals("87654321X", members[1].nif)
        assertEquals("00000000", members[2].nif)
        assertEquals("11111111H", members[3].nif)

        val references = Database { UserReferenceEntity.all().toList() }
        assertEquals(2, references.size)
        references[0].let {
            assertEquals(2u, it.memberNumber)
            assertTrue(it.isDisabled)
        }
        references[1].let {
            assertEquals(4u, it.memberNumber)
            assertFalse(it.isDisabled)
        }
    }

    @Test
    fun `members with no NIF are stored with none, however the source leaves it`() = runTest {
        AES.initForTests()
        Database.initForTests()

        CEA.synchronizeWithDatabase(
            listOf(
                Member(1u, "Alta", "Placeholder 1", "--", "a@example.com"),
                Member(2u, "Alta", "Placeholder 2", "--", "b@example.com"),
                Member(3u, "Alta", "Blank", "  ", "c@example.com"),
                Member(4u, "Alta", "Missing", null, "d@example.com"),
                Member(5u, "Alta", "Has one", "12345678Z", "e@example.com"),
            )
        )

        val members = Database { MemberEntity.all().toList().sortedBy { it.id.value } }
        assertEquals(5, members.size)
        assertEquals(listOf(null, null, null, null, "12345678Z"), members.map { it.nif })
    }

    @Test
    fun `parsing turns a placeholder NIF into none`() {
        val parsed = CEA.parse(
            "Núm. soci/a,Estat,Nom i cognoms,NIF/NIE,Correu electrònic\n1,Alta,Un,--,a@example.com\n2,Alta,Dos,,b@example.com\n3,Alta,Tres,12345678Z,c@example.com\n"
        )

        assertEquals(listOf(null, null, "12345678Z"), parsed.map { it.nif })
    }
}
