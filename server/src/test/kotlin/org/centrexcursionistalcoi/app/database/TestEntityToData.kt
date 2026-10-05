package org.centrexcursionistalcoi.app.database

import io.ktor.http.ContentType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import org.centrexcursionistalcoi.app.ADMIN_GROUP_NAME
import org.centrexcursionistalcoi.app.data.Category
import org.centrexcursionistalcoi.app.data.CategoryPrice
import org.centrexcursionistalcoi.app.data.DepartmentRole
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.data.Entity
import org.centrexcursionistalcoi.app.data.Event
import org.centrexcursionistalcoi.app.data.InventoryItem
import org.centrexcursionistalcoi.app.data.InventoryItemType
import org.centrexcursionistalcoi.app.data.Lending
import org.centrexcursionistalcoi.app.data.Memory
import org.centrexcursionistalcoi.app.data.Post
import org.centrexcursionistalcoi.app.data.Space
import org.centrexcursionistalcoi.app.data.SpaceKey
import org.centrexcursionistalcoi.app.data.SpaceLending
import org.centrexcursionistalcoi.app.data.Sports
import org.centrexcursionistalcoi.app.data.ZonedDateTime
import org.centrexcursionistalcoi.app.database.entity.DepartmentEntity
import org.centrexcursionistalcoi.app.database.entity.DepartmentMemberEntity
import org.centrexcursionistalcoi.app.database.entity.EntityDataConverter
import org.centrexcursionistalcoi.app.database.entity.EventEntity
import org.centrexcursionistalcoi.app.database.entity.FileEntity
import org.centrexcursionistalcoi.app.database.entity.InventoryItemEntity
import org.centrexcursionistalcoi.app.database.entity.InventoryItemTypeEntity
import org.centrexcursionistalcoi.app.database.entity.LendingEntity
import org.centrexcursionistalcoi.app.database.entity.MemoryEntity
import org.centrexcursionistalcoi.app.database.entity.PostEntity
import org.centrexcursionistalcoi.app.database.entity.QualificationEntity
import org.centrexcursionistalcoi.app.database.entity.ReceivedItemEntity
import org.centrexcursionistalcoi.app.database.entity.SpaceEntity
import org.centrexcursionistalcoi.app.database.entity.SpaceKeyEntity
import org.centrexcursionistalcoi.app.database.entity.SpaceLendingEntity
import org.centrexcursionistalcoi.app.database.table.EventMembers
import org.centrexcursionistalcoi.app.database.table.LendingItems
import org.centrexcursionistalcoi.app.database.table.MemoriesFiles
import org.centrexcursionistalcoi.app.database.table.PostFiles
import org.centrexcursionistalcoi.app.database.table.UserQualifications
import org.centrexcursionistalcoi.app.database.utils.encodeList
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.security.UserSession
import org.centrexcursionistalcoi.app.test.FakeAdminUser
import org.centrexcursionistalcoi.app.test.FakeUser
import org.centrexcursionistalcoi.app.test.FakeUser2
import org.jetbrains.exposed.v1.dao.EntityClass
import org.jetbrains.exposed.v1.jdbc.SizedCollection
import org.jetbrains.exposed.v1.jdbc.insert
import kotlin.uuid.Uuid

/**
 * What the server answers for an entity is its shared data class (`toData`): for every kind of entity and for several
 * users, the list can be decoded as the app does, and means the same once decoded.
 */
class TestEntityToData {
    private fun session(sub: String, name: String, vararg groups: String) = UserSession(sub, name, "$sub@example.com", groups.toList())

    private val sessions = listOf(
        session(FakeAdminUser.SUB, "Admin", ADMIN_GROUP_NAME),
        // Manages the department of the fixture
        session(FakeUser.SUB, "User"),
        // Has nothing to do with anything
        session(FakeUser2.SUB, "Other"),
        null,
    )

    private fun <ID : Any, E : org.jetbrains.exposed.v1.dao.Entity<ID>, D : Entity<*>> assertRoundTrip(
        name: String,
        entityClass: EntityClass<ID, E>,
        serializer: KSerializer<D>,
    ) where E : EntityDataConverter<D, *> {
        for (session in sessions) {
            val encoded = Database {
                val entities = entityClass.all().toList()
                assert(entities.isNotEmpty()) { "No $name in the fixture" }
                encodeList(serializer, entities, session)
            }
            // The app decodes it, and it means the same once decoded (the database keeps more precision for instants)
            val decoded = json.decodeFromString(ListSerializer(serializer), encoded)
            assertEquals(
                json.parseToJsonElement(encoded),
                json.parseToJsonElement(json.encodeToString(ListSerializer(serializer), decoded)),
                "$name differ for ${session?.sub}",
            )
        }
    }

    @Test
    fun test_entities_are_answered_as_their_data_classes() = runTest {
        Database.initForTests()
        Database {
            val user = FakeUser.provideEntity()
            val admin = FakeAdminUser.provideEntity()
            FakeUser2.provideEntity()
            val now = Clock.System.now()
            val zoned = ZonedDateTime.fromInstant(now, TimeZone.currentSystemDefault())

            val image = FileEntity.create(byteArrayOf(1, 2, 3), "image.png", ContentType.Image.PNG)
            val attachment = FileEntity.create(byteArrayOf(4, 5, 6), "attachment.pdf", ContentType.parse("application/pdf"))
            val pdf = FileEntity.create(byteArrayOf(7, 8, 9), "memory.pdf", ContentType.parse("application/pdf"))

            val department = DepartmentEntity.new {
                displayName = "Department"
                this.image = image
            }
            DepartmentMemberEntity.new {
                this.department = department
                userReference = user
                confirmed = true
                roles = listOf(DepartmentRole.PEOPLE_MANAGER)
            }
            DepartmentMemberEntity.new {
                this.department = department
                userReference = admin
                confirmed = false
            }
            val qualification = QualificationEntity.new {
                this.department = department
                name = "Lead climbing"
                description = "Description"
            }
            UserQualifications.insert {
                it[UserQualifications.qualification] = qualification.id
                it[userSub] = user.id
                it[grantedBy] = admin.id
            }

            EventEntity.new {
                start = now
                end = now
                place = "Place"
                title = "Event"
                description = "Description"
                maxPeople = 10
                requiresConfirmation = true
                requiresInsurance = true
                this.department = department
                this.image = image
            }.also {
                EventMembers.insert { row ->
                    row[EventMembers.event] = it.id
                    row[EventMembers.userReference] = user.id
                }
                it.setQualificationRequirements(listOf(listOf(qualification.id.value)))
            }

            PostEntity.new {
                title = "Post"
                content = "Content"
                this.department = department
                link = "https://example.com"
            }.also { post ->
                PostFiles.insert {
                    it[PostFiles.post] = post.id
                    it[file] = attachment.id
                }
            }

            val type = InventoryItemTypeEntity.new {
                displayName = "Type"
                description = "Description"
                categories = listOf("a", "b")
                weight = 1.5
                this.department = department
                this.image = image
            }
            val item = InventoryItemEntity.new {
                this.type = type
                variation = "variation"
                nfcId = byteArrayOf(0, 1, 2, 3)
                manufacturerTraceabilityCode = "abc"
            }
            InventoryItemEntity.new { this.type = type }

            val lending = LendingEntity.new {
                timestamp = now
                userSub = user
                from = LocalDate(2025, 10, 8)
                to = LocalDate(2025, 10, 9)
                confirmed = true
                taken = true
                givenBy = admin.id
                givenAt = now
                returned = true
                notes = "notes"
                memorySubmitted = true
                memorySubmittedAt = now
                memoryReviewed = true
            }
            LendingItems.insert {
                it[LendingItems.lending] = lending.id
                it[LendingItems.item] = item.id
            }
            ReceivedItemEntity.new {
                this.lending = lending
                this.item = item
                notes = "Good"
                receivedAt = now
                receivedBy = admin
            }
            LendingEntity.new {
                timestamp = now
                userSub = user
                from = LocalDate(2025, 11, 1)
                to = LocalDate(2025, 11, 2)
            }

            MemoryEntity.new {
                place = "Place"
                externalPeople = "John Doe"
                text = "Memory"
                sport = Sports.ORIENTEERING
                this.department = department
                submittedBy = user
                from = zoned
                to = zoned
                this.lending = lending
                this.pdf = pdf
            }.also { memory ->
                memory.members = SizedCollection(listOf(FakeUser.provideMemberEntity()))
                MemoriesFiles.insert {
                    it[MemoriesFiles.memory] = memory.id
                    it[file] = attachment.id
                }
            }

            val space = SpaceEntity.new {
                name = "Space"
                description = "Description"
                conditionsOfUse = "Conditions"
                requiresKeys = true
                prices = listOf(CategoryPrice(Category.MEMBER, 3.0))
            }
            SpaceKeyEntity.new {
                this.space = space
                name = "Key"
                maxQuantity = 2
                nfcId = byteArrayOf(9, 9)
            }
            SpaceLendingEntity.new {
                userSub = user
                this.space = space
                checkIn = LocalDate(2026, 10, 9)
                checkOut = LocalDate(2026, 10, 10)
                attendees = mapOf(Category.MEMBER to 2)
                pickedUpAt = now
                pickedUpBy = admin
            }
        }

        assertRoundTrip("departments", DepartmentEntity, Department.serializer())
        assertRoundTrip("events", EventEntity, Event.serializer())
        assertRoundTrip("posts", PostEntity, Post.serializer())
        assertRoundTrip("inventory types", InventoryItemTypeEntity, InventoryItemType.serializer())
        assertRoundTrip("inventory items", InventoryItemEntity, InventoryItem.serializer())
        assertRoundTrip("lendings", LendingEntity, Lending.serializer())
        assertRoundTrip("memories", MemoryEntity, Memory.serializer())
        assertRoundTrip("spaces", SpaceEntity, Space.serializer())
        assertRoundTrip("space keys", SpaceKeyEntity, SpaceKey.serializer())
        assertRoundTrip("space lendings", SpaceLendingEntity, SpaceLending.serializer())
    }
}
