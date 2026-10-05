package org.centrexcursionistalcoi.app.database

import io.ktor.http.ContentType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.centrexcursionistalcoi.app.ADMIN_GROUP_NAME
import org.centrexcursionistalcoi.app.data.Category
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.data.DepartmentRole
import org.centrexcursionistalcoi.app.data.Event
import org.centrexcursionistalcoi.app.data.InventoryItemType
import org.centrexcursionistalcoi.app.data.Lending
import org.centrexcursionistalcoi.app.data.Memory
import org.centrexcursionistalcoi.app.data.Post
import org.centrexcursionistalcoi.app.data.SpaceLending
import org.centrexcursionistalcoi.app.data.ZonedDateTime
import org.centrexcursionistalcoi.app.database.entity.DepartmentEntity
import org.centrexcursionistalcoi.app.database.entity.DepartmentMemberEntity
import org.centrexcursionistalcoi.app.database.entity.EventEntity
import org.centrexcursionistalcoi.app.database.entity.FileEntity
import org.centrexcursionistalcoi.app.database.entity.InventoryItemEntity
import org.centrexcursionistalcoi.app.database.entity.InventoryItemTypeEntity
import org.centrexcursionistalcoi.app.database.entity.LendingEntity
import org.centrexcursionistalcoi.app.database.entity.MemberEntity
import org.centrexcursionistalcoi.app.database.entity.MemoryEntity
import org.centrexcursionistalcoi.app.database.entity.PostEntity
import org.centrexcursionistalcoi.app.database.entity.QualificationEntity
import org.centrexcursionistalcoi.app.database.entity.ReceivedItemEntity
import org.centrexcursionistalcoi.app.database.entity.SpaceEntity
import org.centrexcursionistalcoi.app.database.entity.SpaceKeyEntity
import org.centrexcursionistalcoi.app.database.entity.SpaceKeyTypeEntity
import org.centrexcursionistalcoi.app.database.entity.SpaceLendingEntity
import org.centrexcursionistalcoi.app.database.table.EventMembers
import org.centrexcursionistalcoi.app.database.table.LendingItems
import org.centrexcursionistalcoi.app.database.table.PostFiles
import org.centrexcursionistalcoi.app.database.table.SpaceLendingFiles
import org.centrexcursionistalcoi.app.database.table.SpaceLendingKeys
import org.centrexcursionistalcoi.app.database.utils.encodeList
import org.centrexcursionistalcoi.app.data.SpaceLendingFileKind
import org.centrexcursionistalcoi.app.routes.lendingsFor
import org.centrexcursionistalcoi.app.routes.memoriesFor
import org.centrexcursionistalcoi.app.routes.spaceLendingsFor
import org.centrexcursionistalcoi.app.security.UserSession
import org.centrexcursionistalcoi.app.test.FakeAdminUser
import org.centrexcursionistalcoi.app.test.FakeUser
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.statements.StatementContext
import org.jetbrains.exposed.v1.core.statements.StatementInterceptor
import org.jetbrains.exposed.v1.dao.with
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.SizedCollection
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager

/**
 * Answering a list must not run queries for each of its entities (an N+1): the number of queries can't depend on how
 * many there are. Each test lists 2 entities, creates 8 more, and lists again.
 */
class TestListEncodingQueries {
    private val admin = UserSession(FakeAdminUser.SUB, "Admin", "admin@example.com", listOf(ADMIN_GROUP_NAME))

    private var created = 0

    private val now get() = Clock.System.now()
    private val zoned get() = ZonedDateTime.fromInstant(now, TimeZone.currentSystemDefault())

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

    private fun assertConstant(name: String, create: JdbcTransaction.() -> Unit, list: () -> Unit) = runTest {
        Database.initForTests()
        Database { repeat(2) { create() } }
        val few = countStatements(list)
        Database { repeat(8) { create() } }
        val many = countStatements(list)
        assertEquals(few, many, "Listing 10 $name ran more queries than listing 2")
    }

    private fun JdbcTransaction.file(): FileEntity =
        FileEntity.create(byteArrayOf(1, 2, 3), "file ${created++}.png", ContentType.Image.PNG)

    @Test
    fun test_memories() = assertConstant(
        "memories",
        create = {
            val n = created++
            MemoryEntity.new {
                text = "Memory $n"
                submittedBy = FakeAdminUser.provideEntity()
                from = zoned
                to = zoned
                department = DepartmentEntity.new { displayName = "Department $n" }
                pdf = file()
            }.also { it.members = SizedCollection(listOf(MemberEntity.new((1000 + n).toUInt()) { fullName = "Member $n" })) }
        },
        list = { encodeList(Memory.serializer(), admin) { memoriesFor(admin) } },
    )

    @Test
    fun test_item_types() = assertConstant(
        "item types",
        create = {
            val n = created++
            InventoryItemTypeEntity.new {
                displayName = "Type $n"
                department = DepartmentEntity.new { displayName = "Department $n" }
                image = file()
            }
        },
        list = { encodeList(InventoryItemType.serializer(), admin) { InventoryItemTypeEntity.all().toList() } },
    )

    @Test
    fun test_departments() = assertConstant(
        "departments",
        create = {
            val n = created++
            val department = DepartmentEntity.new { displayName = "Department $n"; image = file() }
            for (user in listOf(FakeAdminUser.provideEntity(), FakeUser.provideEntity())) {
                DepartmentMemberEntity.new {
                    this.department = department
                    userReference = user
                    confirmed = true
                    roles = listOf(DepartmentRole.ADMIN)
                }
            }
            QualificationEntity.new { this.department = department; name = "Qualification $n" }
        },
        list = { encodeList(Department.serializer(), admin) { DepartmentEntity.withDataPreloaded(DepartmentEntity.all().toList(), admin) } },
    )

    @Test
    fun test_events() = assertConstant(
        "events",
        create = {
            val n = created++
            val department = DepartmentEntity.new { displayName = "Department $n" }
            val qualification = QualificationEntity.new { this.department = department; name = "Qualification $n" }
            EventEntity.new {
                start = now
                place = "Place"
                title = "Event $n"
                this.department = department
                image = file()
            }.also {
                EventMembers.insert { row ->
                    row[EventMembers.event] = it.id
                    row[EventMembers.userReference] = FakeAdminUser.provideEntity().id
                }
                it.setQualificationRequirements(listOf(listOf(qualification.id.value)))
            }
        },
        list = { encodeList(Event.serializer(), admin) { EventEntity.withDataPreloaded(EventEntity.forSession(admin).toList()) } },
    )

    @Test
    fun test_posts() = assertConstant(
        "posts",
        create = {
            val n = created++
            PostEntity.new {
                title = "Post $n"
                content = "Content"
                department = DepartmentEntity.new { displayName = "Department $n" }
            }.also { post ->
                val file = file()
                PostFiles.insert {
                    it[PostFiles.post] = post.id
                    it[PostFiles.file] = file.id
                }
            }
        },
        list = { encodeList(Post.serializer(), admin) { PostEntity.forSession(admin).with(PostEntity::files).toList() } },
    )

    @Test
    fun test_lendings() = assertConstant(
        "lendings",
        create = {
            val n = created++
            val user = FakeAdminUser.provideEntity()
            val lending = LendingEntity.new {
                timestamp = now
                userSub = user
                from = LocalDate(2025, 10, 8)
                to = LocalDate(2025, 10, 9)
                givenBy = user.id
            }
            val item = InventoryItemEntity.new {
                type = InventoryItemTypeEntity.new { displayName = "Type $n" }
            }
            LendingItems.insert {
                it[LendingItems.lending] = lending.id
                it[LendingItems.item] = item.id
            }
            ReceivedItemEntity.new {
                this.lending = lending
                this.item = item
                receivedBy = user
            }
            MemoryEntity.new {
                text = "Memory $n"
                submittedBy = user
                from = zoned
                to = zoned
                this.lending = lending
            }
        },
        list = { encodeList(Lending.serializer(), admin) { lendingsFor(admin) } },
    )

    @Test
    fun test_space_lendings() = assertConstant(
        "space lendings",
        create = {
            val n = created++
            val space = SpaceEntity.new { name = "Space $n"; description = "Description"; prices = emptyList() }
            val keyType = SpaceKeyTypeEntity.new { name = "Door" }
            val key = SpaceKeyEntity.new { this.type = keyType }
            SpaceLendingEntity.new {
                userSub = FakeUser.provideEntity()
                this.space = space
                checkIn = LocalDate(2026, 10, 9)
                checkOut = LocalDate(2026, 10, 10)
                attendees = mapOf(Category.MEMBER to 1)
            }.also { lending ->
                val file = file()
                SpaceLendingFiles.insert {
                    it[SpaceLendingFiles.lending] = lending.id
                    it[SpaceLendingFiles.file] = file.id
                    it[kind] = SpaceLendingFileKind.PAYMENT_PROOF
                }
                SpaceLendingKeys.insert {
                    it[SpaceLendingKeys.lending] = lending.id
                    it[SpaceLendingKeys.key] = key.id
                    it[givenAt] = kotlin.time.Clock.System.now()
                }
            }
        },
        list = { encodeList(SpaceLending.serializer(), admin) { spaceLendingsFor(admin) } },
    )
}
