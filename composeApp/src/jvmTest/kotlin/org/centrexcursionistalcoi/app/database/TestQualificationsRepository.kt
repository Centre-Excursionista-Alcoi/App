package org.centrexcursionistalcoi.app.database

import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.unmockkAll
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.coroutines.test.runTest
import org.centrexcursionistalcoi.app.data.Qualification
import org.centrexcursionistalcoi.app.data.QualificationGrant
import org.centrexcursionistalcoi.app.database.dao.QualificationDao
import org.centrexcursionistalcoi.app.database.dao.QualificationGrantDao
import org.centrexcursionistalcoi.app.database.entity.QualificationEntity
import org.centrexcursionistalcoi.app.database.entity.QualificationEntity.Companion.toEntity
import org.centrexcursionistalcoi.app.database.entity.QualificationGrantEntity
import org.centrexcursionistalcoi.app.database.entity.QualificationGrantEntity.Companion.toEntity

/**
 * Both lists are replaced as a whole on every sync: what's on the server is the truth, so whatever it no longer
 * lists (a deleted qualification, a revoked grant) must not linger locally.
 */
class TestQualificationsRepository {
    private val qualificationDao = mockk<QualificationDao>()
    private val grantDao = mockk<QualificationGrantDao>()
    private val db = mockk<AppDatabase> {
        every { qualificationDao() } returns qualificationDao
        every { qualificationGrantDao() } returns grantDao
    }
    private val repository = QualificationsRepository(db)

    private val department = Uuid.random()
    private val a = Qualification(Uuid.random(), department, "Assegurar", "Sap assegurar")
    private val b = Qualification(Uuid.random(), department, "Top rope", null)
    private val grantA = QualificationGrant(a.id, "sub", "examiner", Instant.fromEpochMilliseconds(1_000), null)
    private val grantB = QualificationGrant(b.id, "sub", null, Instant.fromEpochMilliseconds(2_000), Instant.fromEpochMilliseconds(9_000))

    @AfterTest
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun entities_roundTrip() {
        assertEquals(a, a.toEntity().toQualification())
        assertEquals(b, b.toEntity().toQualification())
        assertEquals(grantA, grantA.toEntity().toGrant())
        assertEquals(grantB, grantB.toEntity().toGrant())
    }

    @Test
    fun replaceAll_upsertsTheGivenOnes_andDropsTheRest() = runTest {
        val upserted = slot<List<QualificationEntity>>()
        val kept = slot<List<Uuid>>()
        coEvery { qualificationDao.upsertAll(capture(upserted)) } just Runs
        coEvery { qualificationDao.deleteAllExcept(capture(kept)) } just Runs

        repository.replaceAll(listOf(a, b))

        assertEquals(listOf(a, b), upserted.captured.map { it.toQualification() })
        assertEquals(listOf(a.id, b.id), kept.captured)
        coVerify(exactly = 0) { qualificationDao.deleteAll() }
    }

    @Test
    fun replaceAll_withNothing_clearsTheTable() = runTest {
        coEvery { qualificationDao.deleteAll() } just Runs

        repository.replaceAll(emptyList())

        coVerify(exactly = 1) { qualificationDao.deleteAll() }
        coVerify(exactly = 0) { qualificationDao.upsertAll(any()) }
        coVerify(exactly = 0) { qualificationDao.deleteAllExcept(any()) }
    }

    @Test
    fun replaceMyGrants_upsertsTheGivenOnes_andDropsTheRest() = runTest {
        val upserted = slot<List<QualificationGrantEntity>>()
        val kept = slot<List<Uuid>>()
        coEvery { grantDao.upsertAll(capture(upserted)) } just Runs
        coEvery { grantDao.deleteAllExcept(capture(kept)) } just Runs

        repository.replaceMyGrants(listOf(grantA, grantB))

        assertEquals(listOf(grantA, grantB), upserted.captured.map { it.toGrant() })
        assertEquals(listOf(a.id, b.id), kept.captured)
    }

    @Test
    fun replaceMyGrants_withNothing_clearsTheTable() = runTest {
        coEvery { grantDao.deleteAll() } just Runs

        repository.replaceMyGrants(emptyList())

        coVerify(exactly = 1) { grantDao.deleteAll() }
        coVerify(exactly = 0) { grantDao.upsertAll(any()) }
    }
}
