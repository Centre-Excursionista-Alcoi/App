package org.centrexcursionistalcoi.app.database

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import org.centrexcursionistalcoi.app.data.ZonedDateTime
import org.centrexcursionistalcoi.app.database.dao.MemoryDao
import org.centrexcursionistalcoi.app.database.entity.MemoryEntity
import org.centrexcursionistalcoi.app.database.entity.UserEntity
import org.centrexcursionistalcoi.app.database.relation.MemoryWithRelations
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * Regression coverage for the crash fixed after an iOS report: [MemoryWithRelations.toReferenced] throws
 * [org.centrexcursionistalcoi.app.exception.MissingCrossReferenceException] when a memory's submitter isn't
 * resolvable locally yet (expected -- [org.centrexcursionistalcoi.app.sync.DatabaseIntegrityVerifier] relies on
 * that throw to detect and repair it), but every read here used to let that exception propagate straight out of
 * a live, collecting Room Flow with nothing catching it -- fatal, since a regular (non-admin, no department)
 * user can perfectly legitimately have a locally-synced memory whose submitter they can't resolve via `/users`.
 */
class TestMemoriesRepository {

    private val db = mockk<AppDatabase>()
    private val dao = mockk<MemoryDao>()

    private lateinit var repository: MemoriesRepository

    private fun setUp() {
        every { db.memoryDao() } returns dao
        repository = MemoriesRepository(db)
    }

    @AfterTest
    fun tearDown() {
        unmockkAll()
    }

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

    @Test
    fun `get returns null instead of throwing when the submitter is missing locally`() = runTest {
        setUp()
        val broken = memoryWithRelations(memoryEntity(submittedBy = Uuid.random().toString()), submittedBy = null)
        coEvery { dao.get(broken.memory.id) } returns broken

        assertNull(repository.get(broken.memory.id))
    }

    @Test
    fun `selectAll skips a memory with a missing submitter but keeps the resolvable ones`() = runTest {
        setUp()
        val user = userEntity()
        val resolvable = memoryWithRelations(memoryEntity(submittedBy = user.sub), user)
        val broken = memoryWithRelations(memoryEntity(submittedBy = Uuid.random().toString()), submittedBy = null)
        coEvery { dao.selectAll() } returns listOf(resolvable, broken)

        val result = repository.selectAll()

        assertEquals(listOf(resolvable.memory.id), result.map { it.id })
    }

    @Test
    fun `selectAllAsFlow skips a memory with a missing submitter instead of crashing the collector`() = runTest {
        setUp()
        val user = userEntity()
        val resolvable = memoryWithRelations(memoryEntity(submittedBy = user.sub), user)
        val broken = memoryWithRelations(memoryEntity(submittedBy = Uuid.random().toString()), submittedBy = null)
        every { dao.selectAllAsFlow() } returns flowOf(listOf(resolvable, broken))

        val emitted = repository.selectAllAsFlow().let { flow ->
            var result: List<Uuid> = emptyList()
            flow.collect { result = it.map { memory -> memory.id } }
            result
        }

        assertEquals(listOf(resolvable.memory.id), emitted)
    }

    @Test
    fun `getByIdList skips a memory with a missing submitter but keeps the resolvable ones`() = runTest {
        setUp()
        val user = userEntity()
        val resolvable = memoryWithRelations(memoryEntity(submittedBy = user.sub), user)
        val broken = memoryWithRelations(memoryEntity(submittedBy = Uuid.random().toString()), submittedBy = null)
        val ids = listOf(resolvable.memory.id, broken.memory.id)
        coEvery { dao.getByIdList(ids) } returns listOf(resolvable, broken)

        val result = repository.getByIdList(ids)

        assertEquals(listOf(resolvable.memory.id), result.map { it.id })
    }
}
