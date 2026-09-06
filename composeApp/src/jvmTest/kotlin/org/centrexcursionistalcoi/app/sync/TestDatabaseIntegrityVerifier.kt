package org.centrexcursionistalcoi.app.sync

import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.centrexcursionistalcoi.app.data.InventoryItemType
import org.centrexcursionistalcoi.app.data.UserData
import org.centrexcursionistalcoi.app.data.ZonedDateTime
import org.centrexcursionistalcoi.app.database.AppDatabase
import org.centrexcursionistalcoi.app.database.InventoryItemTypesRepository
import org.centrexcursionistalcoi.app.database.UsersRepository
import org.centrexcursionistalcoi.app.database.dao.InventoryItemDao
import org.centrexcursionistalcoi.app.database.dao.LendingDao
import org.centrexcursionistalcoi.app.database.dao.MemoryDao
import org.centrexcursionistalcoi.app.database.entity.InventoryItemEntity
import org.centrexcursionistalcoi.app.database.entity.InventoryItemTypeEntity
import org.centrexcursionistalcoi.app.database.entity.InventoryItemTypeEntity.Companion.toEntity
import org.centrexcursionistalcoi.app.database.entity.LendingEntity
import org.centrexcursionistalcoi.app.database.entity.MemoryEntity
import org.centrexcursionistalcoi.app.database.entity.UserEntity
import org.centrexcursionistalcoi.app.database.relation.InventoryItemTypeWithRelations
import org.centrexcursionistalcoi.app.database.relation.InventoryItemWithRelations
import org.centrexcursionistalcoi.app.database.relation.LendingWithRelations
import org.centrexcursionistalcoi.app.database.relation.MemoryWithRelations
import org.centrexcursionistalcoi.app.database.relation.toReferenced
import org.centrexcursionistalcoi.app.di.DispatcherProvider
import org.centrexcursionistalcoi.app.exception.MissingCrossReferenceException
import org.centrexcursionistalcoi.app.network.InventoryItemTypesRemoteRepository
import org.centrexcursionistalcoi.app.network.UsersRemoteRepository
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Instant
import kotlin.uuid.Uuid

class TestDatabaseIntegrityVerifier {

    private val db = mockk<AppDatabase>()
    private val dao = mockk<InventoryItemDao>()
    private val memoryDao = mockk<MemoryDao>()
    private val lendingDao = mockk<LendingDao>()
    private val remoteRepository = mockk<InventoryItemTypesRemoteRepository>()
    private val repository = mockk<InventoryItemTypesRepository>()
    private val usersRemoteRepository = mockk<UsersRemoteRepository>()
    private val usersRepository = mockk<UsersRepository>()
    private val backgroundJobCoordinator = mockk<BackgroundJobCoordinator>()

    private lateinit var verifier: DatabaseIntegrityVerifier

    @BeforeTest
    fun setUp() {
        every { db.inventoryItemDao() } returns dao
        every { db.memoryDao() } returns memoryDao
        every { db.lendingDao() } returns lendingDao
        // Default to "nothing to fix" for the daos not under test in a given test, since a wipe+resync recurses
        // through the *whole* verifyAndFixReferences(), touching all three.
        coEvery { dao.selectAll() } returns emptyList()
        coEvery { memoryDao.selectAll() } returns emptyList()
        coEvery { lendingDao.selectAll() } returns emptyList()
        verifier = DatabaseIntegrityVerifier(db, remoteRepository, repository, usersRemoteRepository, usersRepository, backgroundJobCoordinator)
    }

    @AfterTest
    fun tearDown() {
        unmockkAll()
    }

    private fun typeEntity(id: Uuid = Uuid.random()) = InventoryItemTypeEntity(
        id = id,
        displayName = "Type $id",
        description = null,
        categories = null,
        department = null,
        image = null,
    )

    private fun itemEntity(id: Uuid = Uuid.random(), typeId: Uuid) = InventoryItemEntity(
        id = id,
        variation = null,
        type = typeId,
        nfcId = null,
        manufacturerTraceabilityCode = null,
    )

    private fun remoteType(id: Uuid) = InventoryItemType(
        id = id,
        displayName = "Remote type $id",
        description = null,
        categories = null,
        department = null,
        image = null,
    )

    private fun aZonedDateTime() = ZonedDateTime.fromInstant(Instant.fromEpochMilliseconds(0), TimeZone.UTC)

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

    private fun remoteUser(sub: String) = userEntity(sub).toUser()

    private fun memoryEntity(id: Uuid = Uuid.random(), submittedBy: String) = MemoryEntity(
        id = id,
        place = null,
        externalUsers = null,
        text = "Memory text",
        sport = null,
        department = null,
        attachments = null,
        submittedBy = submittedBy,
        fromDate = aZonedDateTime(),
        toDate = aZonedDateTime(),
        pdf = null,
        lending = null,
    )

    private fun memoryWithRelations(memory: MemoryEntity, submittedBy: UserEntity?) =
        MemoryWithRelations(memory, department = null, submittedBy = submittedBy, members = emptyList())

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

    private fun lendingWithRelations(
        lending: LendingEntity,
        user: UserEntity?,
        items: List<InventoryItemWithRelations> = emptyList(),
    ) = LendingWithRelations(lending, user = user, givenByUser = null, items = items, receivedItems = emptyList(), memory = null)

    /**
     * [InventoryItemWithRelations.toReferenced] only ever throws [MissingCrossReferenceException] when its `type`
     * relation is null, so it's the only way [item] can naturally enter the catch block with a *non-null* `type`
     * (needed to exercise the "type known, but missing from the server" branch). Force it via the top-level
     * extension function's static facade instead.
     */
    private fun forceMissingCrossReference(item: InventoryItemWithRelations) {
        mockkStatic("org.centrexcursionistalcoi.app.database.relation.InventoryItemWithRelationsKt")
        every { item.toReferenced() } throws MissingCrossReferenceException("InventoryItemType", item.item.type)
    }

    /**
     * [BackgroundJobCoordinator.schedule] is `inline` and tied to the real coroutinesWorkers actual implementation,
     * so its body is compiled straight into the caller and can't be intercepted by mocking the coordinator. Instead,
     * this lets that real (inlined) body run on a harmless dispatcher, stubs away everything else it touches, and
     * controls the outcome through `observe()`, which -is- a normal virtual call we can stub.
     */
    private fun stubSync(finalState: BackgroundJobState) {
        val observableJob = mockk<ObservableBackgroundJob>()
        every { observableJob.stateFlow() } returns flowOf(finalState)

        every { backgroundJobCoordinator.coordinatorLog } returns mockk(relaxed = true)
        every { backgroundJobCoordinator.dispatcherProvider } returns object : DispatcherProvider {
            override val main = Dispatchers.Unconfined
            override val io = Dispatchers.Unconfined
            override val default = Dispatchers.Unconfined
        }
        coEvery { backgroundJobCoordinator.emitState(any(), any(), any()) } just Runs
        every { backgroundJobCoordinator.observe(any()) } returns observableJob
    }

    @Test
    fun `no items does nothing`() = runTest {
        coEvery { dao.selectAll() } returns emptyList()

        verifier.verifyAndFixInventoryItemTypesCrossReferences()

        coVerify(exactly = 0) { remoteRepository.get(any()) }
        coVerify(exactly = 0) { repository.insert(any<InventoryItemTypeEntity>()) }
        coVerify(exactly = 0) { db.clearAllTables() }
    }

    @Test
    fun `item with resolvable type is left untouched`() = runTest {
        val type = typeEntity()
        val item = InventoryItemWithRelations(itemEntity(typeId = type.id), InventoryItemTypeWithRelations(type, null))
        coEvery { dao.selectAll() } returns listOf(item)

        verifier.verifyAndFixInventoryItemTypesCrossReferences()

        coVerify(exactly = 0) { remoteRepository.get(any()) }
        coVerify(exactly = 0) { repository.insert(any<InventoryItemTypeEntity>()) }
        coVerify(exactly = 0) { db.clearAllTables() }
    }

    @Test
    fun `missing type relation wipes database and resyncs on success`() = runTest {
        val type = typeEntity()
        val brokenItem = InventoryItemWithRelations(itemEntity(typeId = type.id), type = null)
        // First pass finds the broken item; after the simulated resync the database comes back fixed, ending recursion.
        coEvery { dao.selectAll() } returnsMany listOf(listOf(brokenItem), emptyList())
        coEvery { db.clearAllTables() } just Runs
        stubSync(BackgroundJobState.SUCCEEDED)

        verifier.verifyAndFixInventoryItemTypesCrossReferences()

        coVerify(exactly = 1) { db.clearAllTables() }
        coVerify(exactly = 2) { dao.selectAll() }
    }

    @Test
    fun `missing type relation throws when the resync job fails`() = runTest {
        val type = typeEntity()
        val brokenItem = InventoryItemWithRelations(itemEntity(typeId = type.id), type = null)
        coEvery { dao.selectAll() } returns listOf(brokenItem)
        coEvery { db.clearAllTables() } just Runs
        stubSync(BackgroundJobState.FAILED)

        val exception = assertFailsWith<IllegalStateException> {
            verifier.verifyAndFixInventoryItemTypesCrossReferences()
        }
        assertEquals("Failed to sync data after clearing database: FAILED", exception.message)
    }

    @Test
    fun `missing reference with resolvable type inserts the type fetched from the server`() = runTest {
        val type = typeEntity()
        val item = InventoryItemWithRelations(itemEntity(typeId = type.id), InventoryItemTypeWithRelations(type, null))
        forceMissingCrossReference(item)
        coEvery { dao.selectAll() } returns listOf(item)
        val fetchedType = remoteType(type.id)
        coEvery { remoteRepository.get(type.id) } returns fetchedType
        coEvery { repository.insert(any<InventoryItemTypeEntity>()) } just Runs

        verifier.verifyAndFixInventoryItemTypesCrossReferences()

        coVerify(exactly = 1) { repository.insert(fetchedType.toEntity()) }
        coVerify(exactly = 0) { db.clearAllTables() }
    }

    @Test
    fun `missing reference throws when item disappears entirely from the database`() = runTest {
        val type = typeEntity()
        val item = InventoryItemWithRelations(itemEntity(typeId = type.id), InventoryItemTypeWithRelations(type, null))
        forceMissingCrossReference(item)
        coEvery { dao.selectAll() } returns listOf(item)
        coEvery { remoteRepository.get(type.id) } returns null
        coEvery { dao.get(item.item.id) } returns null

        val exception = assertFailsWith<IllegalStateException> {
            verifier.verifyAndFixInventoryItemTypesCrossReferences()
        }
        assertEquals(
            "Inventory item ${item.item.id} is missing from the database, and the type ${type.id} is also missing from the server.",
            exception.message,
        )
    }

    @Test
    fun `missing reference throws when the item's type reference is unchanged`() = runTest {
        val type = typeEntity()
        val item = InventoryItemWithRelations(itemEntity(typeId = type.id), InventoryItemTypeWithRelations(type, null))
        forceMissingCrossReference(item)
        coEvery { dao.selectAll() } returns listOf(item)
        coEvery { remoteRepository.get(type.id) } returns null
        // Re-fetching the item still points at the same (still-missing) type.
        coEvery { dao.get(item.item.id) } returns InventoryItemWithRelations(item.item, null)

        val exception = assertFailsWith<IllegalStateException> {
            verifier.verifyAndFixInventoryItemTypesCrossReferences()
        }
        assertEquals(
            "Inventory item ${item.item.id} is missing from the database, and the type ${type.id} is also missing from the server.",
            exception.message,
        )
    }

    @Test
    fun `missing reference throws when the item's new type still cannot be resolved locally`() = runTest {
        val type = typeEntity()
        val item = InventoryItemWithRelations(itemEntity(typeId = type.id), InventoryItemTypeWithRelations(type, null))
        forceMissingCrossReference(item)
        coEvery { dao.selectAll() } returns listOf(item)
        coEvery { remoteRepository.get(type.id) } returns null
        val updatedEntity = item.item.copy(type = Uuid.random())
        coEvery { dao.get(item.item.id) } returns InventoryItemWithRelations(updatedEntity, null)

        val exception = assertFailsWith<IllegalStateException> {
            verifier.verifyAndFixInventoryItemTypesCrossReferences()
        }
        assertEquals(
            "Inventory item ${item.item.id} has been updated to a null type, which is not allowed.",
            exception.message,
        )
    }

    @Test
    fun `missing reference throws when the item's new type is missing from the server`() = runTest {
        val type = typeEntity()
        val item = InventoryItemWithRelations(itemEntity(typeId = type.id), InventoryItemTypeWithRelations(type, null))
        forceMissingCrossReference(item)
        coEvery { dao.selectAll() } returns listOf(item)
        coEvery { remoteRepository.get(type.id) } returns null
        val newType = typeEntity()
        val updatedEntity = item.item.copy(type = newType.id)
        val updatedItem = InventoryItemWithRelations(updatedEntity, InventoryItemTypeWithRelations(newType, null))
        coEvery { dao.get(item.item.id) } returns updatedItem
        coEvery { remoteRepository.get(newType.id) } returns null

        val exception = assertFailsWith<IllegalStateException> {
            verifier.verifyAndFixInventoryItemTypesCrossReferences()
        }
        assertEquals(
            "Inventory item ${item.item.id} has been updated to a new type ${updatedItem.type}, but the new type is missing from the server.",
            exception.message,
        )
    }

    @Test
    fun `missing reference inserts the new type when the item was migrated to a different type`() = runTest {
        val type = typeEntity()
        val item = InventoryItemWithRelations(itemEntity(typeId = type.id), InventoryItemTypeWithRelations(type, null))
        forceMissingCrossReference(item)
        coEvery { dao.selectAll() } returns listOf(item)
        coEvery { remoteRepository.get(type.id) } returns null
        val newType = typeEntity()
        val updatedEntity = item.item.copy(type = newType.id)
        val updatedItem = InventoryItemWithRelations(updatedEntity, InventoryItemTypeWithRelations(newType, null))
        coEvery { dao.get(item.item.id) } returns updatedItem
        val fetchedNewType = remoteType(newType.id)
        coEvery { remoteRepository.get(newType.id) } returns fetchedNewType
        coEvery { repository.insert(any<InventoryItemTypeEntity>()) } just Runs

        verifier.verifyAndFixInventoryItemTypesCrossReferences()

        coVerify(exactly = 1) { repository.insert(fetchedNewType.toEntity()) }
    }

    @Test
    fun `memory with no items does nothing`() = runTest {
        verifier.verifyAndFixMemoriesCrossReferences()

        coVerify(exactly = 0) { usersRemoteRepository.get(any()) }
        coVerify(exactly = 0) { db.clearAllTables() }
    }

    @Test
    fun `memory with resolvable submitter is left untouched`() = runTest {
        val user = userEntity()
        coEvery { memoryDao.selectAll() } returns listOf(memoryWithRelations(memoryEntity(submittedBy = user.sub), user))

        verifier.verifyAndFixMemoriesCrossReferences()

        coVerify(exactly = 0) { usersRemoteRepository.get(any()) }
        coVerify(exactly = 0) { db.clearAllTables() }
    }

    @Test
    fun `memory with missing submitter inserts the user fetched from the server`() = runTest {
        val sub = Uuid.random().toString()
        coEvery { memoryDao.selectAll() } returns listOf(memoryWithRelations(memoryEntity(submittedBy = sub), submittedBy = null))
        val fetchedUser = remoteUser(sub)
        coEvery { usersRemoteRepository.get(sub) } returns fetchedUser
        coEvery { usersRepository.insert(any<UserData>()) } just Runs

        verifier.verifyAndFixMemoriesCrossReferences()

        coVerify(exactly = 1) { usersRepository.insert(fetchedUser) }
        coVerify(exactly = 0) { db.clearAllTables() }
    }

    @Test
    fun `memory with missing submitter wipes database when the user is also missing from the server`() = runTest {
        val sub = Uuid.random().toString()
        val brokenMemory = memoryWithRelations(memoryEntity(submittedBy = sub), submittedBy = null)
        // First pass finds the broken memory; after the simulated resync the database comes back fixed.
        coEvery { memoryDao.selectAll() } returnsMany listOf(listOf(brokenMemory), emptyList())
        coEvery { usersRemoteRepository.get(sub) } returns null
        coEvery { db.clearAllTables() } just Runs
        stubSync(BackgroundJobState.SUCCEEDED)

        verifier.verifyAndFixMemoriesCrossReferences()

        coVerify(exactly = 1) { db.clearAllTables() }
        coVerify(exactly = 2) { memoryDao.selectAll() }
    }

    @Test
    fun `memory with missing submitter throws when the resync job fails`() = runTest {
        val sub = Uuid.random().toString()
        coEvery { memoryDao.selectAll() } returns listOf(memoryWithRelations(memoryEntity(submittedBy = sub), submittedBy = null))
        coEvery { usersRemoteRepository.get(sub) } returns null
        coEvery { db.clearAllTables() } just Runs
        stubSync(BackgroundJobState.FAILED)

        val exception = assertFailsWith<IllegalStateException> {
            verifier.verifyAndFixMemoriesCrossReferences()
        }
        assertEquals("Failed to sync data after clearing database: FAILED", exception.message)
    }

    @Test
    fun `lending with no items does nothing`() = runTest {
        verifier.verifyAndFixLendingsCrossReferences()

        coVerify(exactly = 0) { usersRemoteRepository.get(any()) }
        coVerify(exactly = 0) { db.clearAllTables() }
    }

    @Test
    fun `lending with resolvable borrower is left untouched`() = runTest {
        val user = userEntity()
        coEvery { lendingDao.selectAll() } returns listOf(lendingWithRelations(lendingEntity(userSub = user.sub), user))

        verifier.verifyAndFixLendingsCrossReferences()

        coVerify(exactly = 0) { usersRemoteRepository.get(any()) }
        coVerify(exactly = 0) { db.clearAllTables() }
    }

    @Test
    fun `lending with missing borrower inserts the user fetched from the server`() = runTest {
        val sub = Uuid.random().toString()
        coEvery { lendingDao.selectAll() } returns listOf(lendingWithRelations(lendingEntity(userSub = sub), user = null))
        val fetchedUser = remoteUser(sub)
        coEvery { usersRemoteRepository.get(sub) } returns fetchedUser
        coEvery { usersRepository.insert(any<UserData>()) } just Runs

        verifier.verifyAndFixLendingsCrossReferences()

        coVerify(exactly = 1) { usersRepository.insert(fetchedUser) }
        coVerify(exactly = 0) { db.clearAllTables() }
    }

    @Test
    fun `lending with missing borrower wipes database when the user is also missing from the server`() = runTest {
        val sub = Uuid.random().toString()
        val brokenLending = lendingWithRelations(lendingEntity(userSub = sub), user = null)
        coEvery { lendingDao.selectAll() } returnsMany listOf(listOf(brokenLending), emptyList())
        coEvery { usersRemoteRepository.get(sub) } returns null
        coEvery { db.clearAllTables() } just Runs
        stubSync(BackgroundJobState.SUCCEEDED)

        verifier.verifyAndFixLendingsCrossReferences()

        coVerify(exactly = 1) { db.clearAllTables() }
        coVerify(exactly = 2) { lendingDao.selectAll() }
    }

    @Test
    fun `lending with missing borrower throws when the resync job fails`() = runTest {
        val sub = Uuid.random().toString()
        coEvery { lendingDao.selectAll() } returns listOf(lendingWithRelations(lendingEntity(userSub = sub), user = null))
        coEvery { usersRemoteRepository.get(sub) } returns null
        coEvery { db.clearAllTables() } just Runs
        stubSync(BackgroundJobState.FAILED)

        val exception = assertFailsWith<IllegalStateException> {
            verifier.verifyAndFixLendingsCrossReferences()
        }
        assertEquals("Failed to sync data after clearing database: FAILED", exception.message)
    }

    @Test
    fun `lending with an item missing its type wipes the database instead of touching users`() = runTest {
        val user = userEntity()
        val brokenItem = InventoryItemWithRelations(itemEntity(typeId = Uuid.random()), type = null)
        val brokenLending = lendingWithRelations(lendingEntity(userSub = user.sub), user, items = listOf(brokenItem))
        coEvery { lendingDao.selectAll() } returnsMany listOf(listOf(brokenLending), emptyList())
        coEvery { db.clearAllTables() } just Runs
        stubSync(BackgroundJobState.SUCCEEDED)

        verifier.verifyAndFixLendingsCrossReferences()

        coVerify(exactly = 1) { db.clearAllTables() }
        coVerify(exactly = 0) { usersRemoteRepository.get(any()) }
    }
}
