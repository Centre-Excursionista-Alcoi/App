package org.centrexcursionistalcoi.app.database.migrations

import kotlin.test.Test
import kotlin.test.assertEquals
import org.centrexcursionistalcoi.app.assertTrue
import org.centrexcursionistalcoi.app.data.PaymentStatus
import org.centrexcursionistalcoi.app.data.RedsysPayload
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.PostgresTestBase
import org.centrexcursionistalcoi.app.database.table.Transactions
import org.jetbrains.exposed.v1.jdbc.selectAll

/** Tests that the payload of a transaction, stored as `redsysPayload`, is kept when the column becomes `payload`. */
class TestV14Migration : PostgresTestBase() {
    @Test
    fun test() {
        // Create the current schema, then simulate a pre-V14 database: the column is named after Redsys.
        Database.init()
        Database.exec("""ALTER TABLE transactions RENAME COLUMN payload TO "redsysPayload"""").assertTrue()
        Database {
            exec(
                """
                    INSERT INTO transactions (id, "orderId", amount, status, "redsysPayload")
                    VALUES (gen_random_uuid(), '1234', 10, 'COMPLETED',
                        '{"Ds_Order":"1234","Ds_Response":"0000","Ds_Amount":"1000","Ds_Currency":"978","Ds_MerchantCode":"1","Ds_Terminal":"1","Ds_TransactionType":"0"}')
                """.trimIndent()
            )
        }

        Database { V14.migrate() }

        val row = Database { Transactions.selectAll().single() }
        assertEquals(PaymentStatus.COMPLETED, row[Transactions.status])
        val payload = row[Transactions.payload]
        assertEquals(RedsysPayload("1234", "0000", "1000", "978", "1", "1", "0"), payload)
    }
}
