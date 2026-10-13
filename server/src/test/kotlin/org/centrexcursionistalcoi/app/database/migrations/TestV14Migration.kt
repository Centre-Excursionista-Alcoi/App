package org.centrexcursionistalcoi.app.database.migrations

import kotlin.test.Test
import kotlin.test.assertEquals
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.PostgresTestBase

/**
 * Tests that the payload of a transaction, stored as `redsysPayload`, is kept when the column becomes `payload`.
 *
 * Doesn't depend on the current schema: it creates the `transactions` table as it was before V14, with only what the
 * migration touches, and reads it back with plain SQL.
 */
class TestV14Migration : PostgresTestBase() {
    private val payload =
        """{"Ds_Order":"1234","Ds_Response":"0000","Ds_Amount":"1000","Ds_Currency":"978","Ds_MerchantCode":"1","Ds_Terminal":"1","Ds_TransactionType":"0"}"""

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
                        "redsysPayload" jsonb NULL
                    )
                """.trimIndent()
            )
            exec("""INSERT INTO transactions VALUES (gen_random_uuid(), '1234', 10, 'COMPLETED', '$payload')""")
            exec("""INSERT INTO transactions VALUES (gen_random_uuid(), '5678', 10, 'PENDING', NULL)""")
        }

        Database { V14.migrate() }

        assertEquals(setOf("id", "orderId", "amount", "status", "payload"), columns())
        val rs = Database.execQuery("""SELECT "orderId", payload::text AS payload FROM transactions ORDER BY "orderId"""")
        assertEquals(true, rs.next())
        assertEquals("1234", rs.getString("orderId"))
        // jsonb normalises the whitespace, so compare it as it comes back
        assertEquals(
            Database.execQuery("SELECT '$payload'::jsonb::text").let { it.next(); it.getString(1) },
            rs.getString("payload"),
        )
        assertEquals(true, rs.next())
        assertEquals(null, rs.getString("payload"))
    }

    @Test
    fun test_alreadyMigrated_doesNothing() {
        Database {
            exec("""CREATE TABLE transactions (id uuid PRIMARY KEY, "orderId" text NOT NULL, payload jsonb NULL)""")
        }

        Database { V14.migrate() }

        assertEquals(setOf("id", "orderId", "payload"), columns())
    }
}
