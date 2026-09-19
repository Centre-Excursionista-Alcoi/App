package org.centrexcursionistalcoi.app.database

import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.centrexcursionistalcoi.app.data.Lending
import org.centrexcursionistalcoi.app.data.ReceivedItem
import org.centrexcursionistalcoi.app.database.dao.LendingDao
import org.centrexcursionistalcoi.app.database.dao.LendingItemDao
import org.centrexcursionistalcoi.app.database.dao.ReceivedItemDao
import org.centrexcursionistalcoi.app.database.entity.LendingEntity.Companion.toEntity
import org.centrexcursionistalcoi.app.database.entity.ReceivedItemEntity.Companion.toEntity
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * Regression coverage for the received-items part of a lending sync: a foreign key violation while caching one
 * received item locally (e.g. its `item`/`receivedBy` isn't present in the local database yet -- see
 * [org.centrexcursionistalcoi.app.network.LendingsRemoteRepository]) must not abort the whole sync.
 */
class TestLendingsRepository {
    private val db = mockk<AppDatabase>()
    private val lendingDao = mockk<LendingDao>()
    private val lendingItemDao = mockk<LendingItemDao>()
    private val receivedItemDao = mockk<ReceivedItemDao>()
    private val memoriesRepository = mockk<MemoriesRepository>()

    private lateinit var repository: LendingsRepository

    @AfterTest
    fun tearDown() {
        unmockkAll()
    }

    private fun setUp() {
        every { db.lendingDao() } returns lendingDao
        every { db.lendingItemDao() } returns lendingItemDao
        every { db.receivedItemDao() } returns receivedItemDao
        coEvery { lendingDao.insert(any()) } just Runs
        coEvery { lendingItemDao.get(any(), any()) } returns null
        coEvery { lendingItemDao.insert(any()) } just Runs
        coEvery { receivedItemDao.get(any()) } returns null
        repository = LendingsRepository(db, memoriesRepository)
    }

    private fun aLending(receivedItems: List<ReceivedItem>) = Lending(
        id = Uuid.random(),
        userSub = "user",
        timestamp = Instant.fromEpochMilliseconds(0),
        confirmed = true,
        taken = true,
        givenBy = null,
        givenAt = null,
        returned = true,
        receivedItems = receivedItems,
        memorySubmitted = false,
        memorySubmittedAt = null,
        memory = null,
        memoryReviewed = false,
        from = LocalDate(2024, 1, 1),
        to = LocalDate(2024, 1, 2),
        notes = null,
        items = emptyList(),
    )

    private fun aReceivedItem(lendingId: Uuid) = ReceivedItem(
        id = Uuid.random(),
        lendingId = lendingId,
        itemId = Uuid.random(),
        notes = null,
        receivedBy = "receiver",
        receivedAt = Instant.fromEpochMilliseconds(0),
    )

    @Test
    fun `insertRaw does not propagate a foreign key failure from a single received item`() = runTest {
        setUp()
        val receivedItem = aReceivedItem(Uuid.random())
        val lending = aLending(listOf(receivedItem))
        coEvery { receivedItemDao.insert(receivedItem.toEntity()) } throws IllegalStateException("FOREIGN KEY constraint failed")

        // Must not throw.
        repository.insertRaw(lending)

        coVerify(exactly = 1) { lendingDao.insert(lending.toEntity()) }
    }

    @Test
    fun `insertRaw still stores every received item even if an earlier one fails`() = runTest {
        setUp()
        val failing = aReceivedItem(Uuid.random())
        val succeeding = aReceivedItem(Uuid.random())
        val lending = aLending(listOf(failing, succeeding))
        coEvery { receivedItemDao.insert(failing.toEntity()) } throws IllegalStateException("FOREIGN KEY constraint failed")
        coEvery { receivedItemDao.insert(succeeding.toEntity()) } just Runs

        repository.insertRaw(lending)

        coVerify(exactly = 1) { receivedItemDao.insert(failing.toEntity()) }
        coVerify(exactly = 1) { receivedItemDao.insert(succeeding.toEntity()) }
    }
}
