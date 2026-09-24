package org.centrexcursionistalcoi.app.database

import androidx.sqlite.SQLiteException
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.centrexcursionistalcoi.app.data.Lending
import org.centrexcursionistalcoi.app.data.ReceivedItem
import org.centrexcursionistalcoi.app.database.dao.LendingDao
import org.centrexcursionistalcoi.app.database.dao.LendingItemDao
import org.centrexcursionistalcoi.app.database.dao.ReceivedItemDao
import org.centrexcursionistalcoi.app.database.entity.LendingEntity
import org.centrexcursionistalcoi.app.database.entity.LendingEntity.Companion.toEntity
import org.centrexcursionistalcoi.app.database.entity.ReceivedItemEntity.Companion.toEntity
import org.centrexcursionistalcoi.app.database.entity.UserEntity
import org.centrexcursionistalcoi.app.database.relation.LendingWithRelations
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * Regression coverage for:
 * - the received-items part of a lending sync: a foreign key violation while caching one received item locally
 *   (e.g. its `item`/`receivedBy` isn't present in the local database yet -- see
 *   [org.centrexcursionistalcoi.app.network.LendingsRemoteRepository]) must not abort the whole sync.
 * - the crash fixed after an iOS report: [LendingWithRelations.toReferenced] throws
 *   [org.centrexcursionistalcoi.app.exception.MissingCrossReferenceException] when a lending's borrower isn't
 *   resolvable locally yet (expected -- [org.centrexcursionistalcoi.app.sync.DatabaseIntegrityVerifier] relies
 *   on that throw to detect and repair it), but every read here used to let that exception propagate straight
 *   out of a live, collecting Room Flow with nothing catching it.
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

    private fun lendingEntity(id: Uuid = Uuid.random(), userSub: String) = LendingEntity(
        id = id,
        userSub = userSub,
        timestamp = Instant.fromEpochMilliseconds(0),
        fromDate = LocalDate(2024, 1, 1),
        toDate = LocalDate(2024, 1, 2),
        confirmed = false,
        taken = false,
        givenBy = null,
        givenAt = null,
        returned = false,
        memorySubmitted = false,
        memorySubmittedAt = null,
        memoryReviewed = false,
        notes = null,
    )

    private fun userEntity(sub: String = Uuid.random().toString()) = UserEntity(
        sub = sub,
        memberNumber = 1,
        fullName = "User $sub",
        email = "$sub@example.com",
        groups = emptyList(),
        departments = emptyList(),
        lendingUser = null,
        insurances = emptyList(),
        isDisabled = false,
    )

    private fun lendingWithRelations(lending: LendingEntity, user: UserEntity?) =
        LendingWithRelations(lending, user = user, givenByUser = null, items = emptyList(), receivedItems = emptyList(), memory = null)

    @Test
    fun `insertRaw does not propagate a foreign key failure from a single received item`() = runTest {
        setUp()
        val receivedItem = aReceivedItem(Uuid.random())
        val lending = aLending(listOf(receivedItem))
        coEvery { receivedItemDao.insert(receivedItem.toEntity()) } throws SQLiteException("FOREIGN KEY constraint failed")

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
        coEvery { receivedItemDao.insert(failing.toEntity()) } throws SQLiteException("FOREIGN KEY constraint failed")
        coEvery { receivedItemDao.insert(succeeding.toEntity()) } just Runs

        repository.insertRaw(lending)

        coVerify(exactly = 1) { receivedItemDao.insert(failing.toEntity()) }
        coVerify(exactly = 1) { receivedItemDao.insert(succeeding.toEntity()) }
    }

    @Test
    fun `insertRaw does not swallow cancellation`() = runTest {
        setUp()
        val receivedItem = aReceivedItem(Uuid.random())
        val lending = aLending(listOf(receivedItem))
        coEvery { receivedItemDao.insert(receivedItem.toEntity()) } throws CancellationException("cancelled")

        assertFailsWith<CancellationException> {
            repository.insertRaw(lending)
        }
    }

    @Test
    fun `get returns null instead of throwing when the borrower is missing locally`() = runTest {
        setUp()
        val broken = lendingWithRelations(lendingEntity(userSub = Uuid.random().toString()), user = null)
        coEvery { lendingDao.get(broken.lending.id) } returns broken

        assertNull(repository.get(broken.lending.id))
    }

    @Test
    fun `selectAll skips a lending with a missing borrower but keeps the resolvable ones`() = runTest {
        setUp()
        val user = userEntity()
        val resolvable = lendingWithRelations(lendingEntity(userSub = user.sub), user)
        val broken = lendingWithRelations(lendingEntity(userSub = Uuid.random().toString()), user = null)
        coEvery { lendingDao.selectAll() } returns listOf(resolvable, broken)

        val result = repository.selectAll()

        assertEquals(listOf(resolvable.lending.id), result.map { it.id })
    }

    @Test
    fun `selectAllAsFlow skips a lending with a missing borrower instead of crashing the collector`() = runTest {
        setUp()
        val user = userEntity()
        val resolvable = lendingWithRelations(lendingEntity(userSub = user.sub), user)
        val broken = lendingWithRelations(lendingEntity(userSub = Uuid.random().toString()), user = null)
        every { lendingDao.selectAllAsFlow() } returns flowOf(listOf(resolvable, broken))

        var emitted: List<Uuid> = emptyList()
        repository.selectAllAsFlow().collect { emitted = it.map { lending -> lending.id } }

        assertEquals(listOf(resolvable.lending.id), emitted)
    }
}
