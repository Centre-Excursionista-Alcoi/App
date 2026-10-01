package org.centrexcursionistalcoi.app.database

import androidx.room3.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.centrexcursionistalcoi.app.data.Memory
import org.centrexcursionistalcoi.app.data.ZonedDateTime
import org.centrexcursionistalcoi.app.database.entity.InventoryItemEntity
import org.centrexcursionistalcoi.app.database.entity.InventoryItemTypeEntity
import org.centrexcursionistalcoi.app.database.entity.LendingEntity
import org.centrexcursionistalcoi.app.database.entity.MemoryEntity
import org.centrexcursionistalcoi.app.database.entity.ReceivedItemEntity
import org.centrexcursionistalcoi.app.database.entity.UserEntity
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * Writes on a real (in-memory) database: upserts storing the same entity at once, and deleting a lending that other
 * rows reference.
 */
class TestDatabaseWrites {
    private val db = getRoomDatabase(Room.inMemoryDatabaseBuilder<AppDatabase>(), Dispatchers.IO)

    @AfterTest
    fun tearDown() {
        db.close()
    }

    private val epoch = Instant.fromEpochMilliseconds(0)

    private fun memory(lending: Uuid? = null) = MemoryEntity(
        id = Uuid.random(),
        place = null,
        externalUsers = null,
        text = "Memory text",
        sport = null,
        department = null,
        attachments = null,
        submittedBy = "submitter",
        fromDate = ZonedDateTime.fromInstant(epoch, TimeZone.UTC),
        toDate = ZonedDateTime.fromInstant(epoch, TimeZone.UTC),
        pdf = null,
        lending = lending,
    )

    private fun lending() = LendingEntity(
        id = Uuid.random(),
        userSub = "user",
        timestamp = epoch,
        fromDate = LocalDate(2024, 1, 1),
        toDate = LocalDate(2024, 1, 2),
        confirmed = true,
        taken = true,
        givenBy = null,
        givenAt = null,
        returned = true,
        memorySubmitted = true,
        memorySubmittedAt = epoch,
        memoryReviewed = false,
        notes = null,
    )

    @Test
    fun `storing the same memory concurrently doesn't fail`() = runTest {
        val repository = MemoriesRepository(db)
        val memory = Memory(
            id = Uuid.random(),
            place = null,
            members = emptyList(),
            externalUsers = null,
            text = "Memory text",
            sport = null,
            department = null,
            attachments = emptyList(),
            submittedBy = "submitter",
            from = ZonedDateTime.fromInstant(epoch, TimeZone.UTC),
            to = ZonedDateTime.fromInstant(epoch, TimeZone.UTC),
            pdf = null,
            lending = null,
        )

        // Like a full sync and a push notification's sync storing it at once
        withContext(Dispatchers.IO) {
            (1..20).map { async { repository.insertOrUpdate(memory) } }.awaitAll()
        }

        assertNotNull(db.memoryDao().get(memory.id))
    }

    @Test
    fun `deleting a lending detaches its memory and deletes its received items`() = runTest {
        val repository = LendingsRepository(db, MemoriesRepository(db))
        val lending = lending()
        db.lendingDao().insert(lending)
        val memory = memory(lending = lending.id)
        db.memoryDao().insert(memory)
        val type = InventoryItemTypeEntity(Uuid.random(), "Type", null, null, null, null)
        db.inventoryItemTypeDao().insert(type)
        val item = InventoryItemEntity(Uuid.random(), null, type.id, null, null)
        db.inventoryItemDao().insert(item)
        val user = UserEntity(
            sub = "receiver",
            memberNumber = 1,
            fullName = "Receiver",
            email = "receiver@example.com",
            groups = emptyList(),
            departments = emptyList(),
            lendingUser = null,
            insurances = emptyList(),
            isDisabled = false,
        )
        db.userDao().insert(user)
        val receivedItem = ReceivedItemEntity(Uuid.random(), lending.id, item.id, null, user.sub, epoch)
        db.receivedItemDao().insert(receivedItem)

        repository.delete(lending.id)

        assertNull(db.lendingDao().get(lending.id))
        assertNull(db.receivedItemDao().get(receivedItem.id))
        val stored = assertNotNull(db.memoryDao().get(memory.id), "The memory must be kept")
        assertEquals(null, stored.memory.lending)
    }
}
