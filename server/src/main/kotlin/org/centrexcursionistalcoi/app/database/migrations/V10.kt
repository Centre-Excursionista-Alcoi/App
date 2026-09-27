package org.centrexcursionistalcoi.app.database.migrations

import org.centrexcursionistalcoi.app.database.table.UserInsuranceDocuments
import org.centrexcursionistalcoi.app.database.table.UserInsurances
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction

/**
 * Migration V10:
 * - Insurances can have several documents, stored in `user_insurance_documents` (created, like every new table,
 *   before migrations run). Each insurance's single `document` is moved there as its first document, and the
 *   column is dropped.
 */
object V10 : DatabaseMigration {
    override val from: Int = 9
    override val to: Int = 10

    context(tr: JdbcTransaction)
    override fun migrate() {
        // The identifiers exactly as Exposed created the tables.
        val insurances = tr.identity(UserInsurances)
        val documents = tr.identity(UserInsuranceDocuments)

        tr.exec(
            """
                INSERT INTO $documents (id, insurance, file, position)
                SELECT gen_random_uuid(), id, document, 0 FROM $insurances WHERE document IS NOT NULL;
            """.trimIndent()
        )

        tr.exec(
            """
                ALTER TABLE $insurances DROP COLUMN document;
            """.trimIndent()
        )
    }
}
