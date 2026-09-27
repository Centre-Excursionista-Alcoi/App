package org.centrexcursionistalcoi.app.database.migrations

import io.ktor.http.ContentType
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.centrexcursionistalcoi.app.assertTrue
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.PostgresTestBase
import org.centrexcursionistalcoi.app.database.entity.FileEntity
import org.centrexcursionistalcoi.app.database.entity.UserInsuranceEntity
import org.centrexcursionistalcoi.app.database.table.UserInsurances
import org.centrexcursionistalcoi.app.security.AES
import org.centrexcursionistalcoi.app.test.FakeUser
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/**
 * Tests the migration that moves each insurance's single `document` into `user_insurance_documents`: an insurance
 * with a document keeps it as its only document, one without gets none, and the old column is dropped.
 */
class TestV10Migration : PostgresTestBase() {
    @Test
    fun test() {
        AES.initForTests()

        // Create the current schema (with user_insurance_documents, and no UserInsurances.document column).
        Database.init()

        val user = Database { transaction { FakeUser.provideEntity() } }
        val document = Database {
            FileEntity.new {
                name = "policy.pdf"
                contentType = ContentType.Application.Pdf
                bytes = byteArrayOf(1, 2, 3)
            }
        }
        val withDocument = Database {
            UserInsuranceEntity.new {
                userSub = user
                insuranceCompany = "Rocalsub"
                policyNumber = "WITH"
                validFrom = LocalDate.of(2025, 1, 1)
                validTo = LocalDate.of(2025, 12, 31)
            }
        }
        val withoutDocument = Database {
            UserInsuranceEntity.new {
                userSub = user
                insuranceCompany = "Rocalsub"
                policyNumber = "WITHOUT"
                validFrom = LocalDate.of(2025, 1, 1)
                validTo = LocalDate.of(2025, 12, 31)
            }
        }

        // Simulate a pre-V10 database: the document is a column of the insurance itself.
        val insurances = Database { identity(UserInsurances) }
        Database.exec(
            """
                ALTER TABLE $insurances ADD COLUMN document uuid NULL REFERENCES files(id);
                UPDATE $insurances SET document = '${document.id.value}' WHERE id = '${withDocument.id.value}';
            """.trimIndent()
        ).assertTrue()

        // Migrate
        Database { V10.migrate() }

        Database {
            assertEquals(listOf(document.id.value), UserInsuranceEntity[withDocument.id].documentIds())
            assertTrue(UserInsuranceEntity[withoutDocument.id].documentIds().isEmpty())
        }
        Database.execQuery(
            """
                SELECT 1 FROM information_schema.columns
                WHERE lower(table_name) = 'userinsurances' AND column_name = 'document'
            """.trimIndent()
        ).let { rs ->
            assertFalse(rs.next())
        }
    }
}
