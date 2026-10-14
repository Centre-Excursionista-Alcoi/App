package org.centrexcursionistalcoi.app.database.migrations

import kotlin.test.Test
import kotlin.test.assertEquals
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.PostgresTestBase

/** Tests that `payment_audit_log` is created, and that running the migration again keeps what it holds. */
class TestV16Migration : PostgresTestBase() {
    private fun columns() = Database.execQuery(
        "SELECT column_name FROM information_schema.columns WHERE table_name = 'payment_audit_log'"
    ).let { rs -> buildSet { while (rs.next()) add(rs.getString("column_name")) } }

    @Test
    fun test() {
        Database { V16.migrate() }

        assertEquals(
            setOf(
                "id", "createdAt", "transactionId", "referenceKey", "provider", "event", "actor",
                "previousStatus", "newStatus", "amountCents", "detail",
            ),
            columns(),
        )
    }

    @Test
    fun test_alreadyMigrated_keepsEntries() {
        Database { V16.migrate() }
        Database { exec("""INSERT INTO payment_audit_log (event, actor) VALUES ('CREATED', 'system')""") }

        Database { V16.migrate() }

        val rs = Database.execQuery("SELECT count(*) FROM payment_audit_log")
        rs.next()
        assertEquals(1, rs.getInt(1))
    }
}
