package org.centrexcursionistalcoi.app.database.migrations

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.PostgresTestBase
import org.centrexcursionistalcoi.app.database.table.Transactions

/**
 * Tests that transactions, as V14 left them (Redsys-only, in euros), become provider-neutral ones.
 *
 * Doesn't depend on the current schema: it creates the `transactions` table as it was before V15, with only what the
 * migration touches, and reads it back with plain SQL.
 */
class TestV15Migration : PostgresTestBase() {
    private fun columns() = Database.execQuery(
        "SELECT column_name FROM information_schema.columns WHERE table_name = 'transactions'"
    ).let { rs -> buildSet { while (rs.next()) add(rs.getString("column_name")) } }

    @Test
    fun test() {
        Database {
            exec(
                """
                    CREATE TABLE transactions (
                        id uuid PRIMARY KEY,
                        "orderId" text NOT NULL,
                        amount double precision NOT NULL,
                        status varchar(20) NOT NULL,
                        payload jsonb NULL
                    )
                """.trimIndent()
            )
            exec("""INSERT INTO transactions VALUES (gen_random_uuid(), '1234ABC', 4.35, 'COMPLETED', NULL)""")
            exec("""INSERT INTO transactions VALUES (gen_random_uuid(), '5678DEF', 3, 'PENDING', NULL)""")
        }

        Database { V15.migrate() }

        assertEquals(setOf("id", "status", "payload", "provider", "reference", "referenceKey", "amountCents"), columns())
        val rs = Database.execQuery(
            """SELECT provider, reference::text, "referenceKey", "amountCents" FROM transactions ORDER BY "referenceKey" """
        )
        assertEquals(true, rs.next())
        assertEquals("redsys", rs.getString("provider"))
        assertEquals("""{"redsys_order": "1234ABC"}""", rs.getString("reference"))
        assertEquals("redsys:1234ABC", rs.getString("referenceKey"))
        assertEquals(435, rs.getInt("amountCents"))
        assertEquals(true, rs.next())
        assertEquals(300, rs.getInt("amountCents"))
    }

    @Test
    fun test_referenceKeyIsUnique() {
        Database {
            exec("""CREATE TABLE transactions (id uuid PRIMARY KEY, "orderId" text NOT NULL, amount double precision NOT NULL)""")
        }
        Database { V15.migrate() }

        val insert = """INSERT INTO transactions (id, provider, reference, "referenceKey", "amountCents") VALUES (gen_random_uuid(), 'redsys', '{}', 'redsys:1', 100)"""
        Database { exec(insert) }
        assertFails { Database { exec(insert) } }
    }

    @Test
    fun test_alreadyMigrated_doesNothing() {
        Database {
            exec("""CREATE TABLE transactions (id uuid PRIMARY KEY, provider text NOT NULL, "amountCents" integer NOT NULL)""")
        }

        Database { V15.migrate() }

        assertEquals(setOf("id", "provider", "amountCents"), columns())
    }

    @Test
    fun test_createsAuditLog_andKeepsItOnRepeat() {
        Database { exec("""CREATE TABLE transactions (id uuid PRIMARY KEY, provider text NOT NULL)""") }

        Database { V15.migrate() }
        Database { exec("""INSERT INTO payment_audit_log (event, actor) VALUES ('CREATED', 'system')""") }
        Database { V15.migrate() }

        val columns = Database.execQuery(
            "SELECT column_name FROM information_schema.columns WHERE table_name = 'payment_audit_log'"
        ).let { rs -> buildSet { while (rs.next()) add(rs.getString("column_name")) } }
        assertEquals(
            setOf(
                "id", "createdAt", "transactionId", "referenceKey", "provider", "event", "actor",
                "previousStatus", "newStatus", "amountCents", "detail",
            ),
            columns,
        )
        val rs = Database.execQuery("SELECT count(*) FROM payment_audit_log")
        rs.next()
        assertEquals(1, rs.getInt(1))
    }
}
