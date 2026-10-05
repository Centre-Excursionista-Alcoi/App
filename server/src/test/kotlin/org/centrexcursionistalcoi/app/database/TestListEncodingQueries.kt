package org.centrexcursionistalcoi.app.database

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import org.centrexcursionistalcoi.app.data.ZonedDateTime
import org.centrexcursionistalcoi.app.database.entity.DepartmentEntity
import org.centrexcursionistalcoi.app.database.entity.InventoryItemTypeEntity
import org.centrexcursionistalcoi.app.database.entity.MemberEntity
import org.centrexcursionistalcoi.app.database.entity.MemoryEntity
import org.centrexcursionistalcoi.app.database.utils.encodeEntityListToString
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.routes.memoriesFor
import org.centrexcursionistalcoi.app.security.UserSession
import org.centrexcursionistalcoi.app.test.FakeAdminUser
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.statements.StatementContext
import org.jetbrains.exposed.v1.core.statements.StatementInterceptor
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.SizedCollection
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager

/**
 * Encoding a list must not run queries for each of its entities (an N+1): the number of queries can't depend on how
 * many there are.
 */
class TestListEncodingQueries {
    private val admin = UserSession(FakeAdminUser.SUB, "Admin", "admin@example.com", listOf(org.centrexcursionistalcoi.app.ADMIN_GROUP_NAME))

    /** The number of statements [block] runs. */
    private fun countStatements(block: () -> Unit): Int = Database {
        var count = 0
        TransactionManager.current().registerInterceptor(object : StatementInterceptor {
            override fun beforeExecution(transaction: Transaction, context: StatementContext) {
                count++
            }
        })
        block()
        count
    }

    private var created = 0

    context(_: JdbcTransaction)
    private fun newMemory() {
        val n = created++
        val now = ZonedDateTime.fromInstant(Clock.System.now(), TimeZone.currentSystemDefault())
        MemoryEntity.new {
            text = "Memory $n"
            submittedBy = FakeAdminUser.provideEntity()
            from = now
            to = now
            department = DepartmentEntity.new { displayName = "Department $n" }
        }.also { it.members = SizedCollection(listOf(MemberEntity.new((1000 + n).toUInt()) { fullName = "Member $n" })) }
    }

    @Test
    fun test_memories_run_the_same_queries_for_any_amount() = runTest {
        Database.initForTests()
        Database { repeat(2) { newMemory() } }
        val few = countStatements { Database { json.encodeEntityListToString(memoriesFor(admin), MemoryEntity) } }

        Database { repeat(8) { newMemory() } }
        val many = countStatements { Database { json.encodeEntityListToString(memoriesFor(admin), MemoryEntity) } }

        assertEquals(few, many, "Listing 10 memories ran more queries than listing 2")
    }

    @Test
    fun test_item_types_run_the_same_queries_for_any_amount() = runTest {
        Database.initForTests()
        fun newType() {
            val n = created++
            InventoryItemTypeEntity.new {
                displayName = "Type $n"
                department = DepartmentEntity.new { displayName = "Department $n" }
            }
        }
        Database { repeat(2) { newType() } }
        val few = countStatements { Database { json.encodeEntityListToString(InventoryItemTypeEntity.all().toList(), InventoryItemTypeEntity) } }

        Database { repeat(8) { newType() } }
        val many = countStatements { Database { json.encodeEntityListToString(InventoryItemTypeEntity.all().toList(), InventoryItemTypeEntity) } }

        assertEquals(few, many, "Listing 10 item types ran more queries than listing 2")
    }
}
