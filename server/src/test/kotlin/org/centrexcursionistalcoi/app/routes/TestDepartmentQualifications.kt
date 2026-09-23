package org.centrexcursionistalcoi.app.routes

import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import org.centrexcursionistalcoi.app.ApplicationTestBase
import org.centrexcursionistalcoi.app.assertBody
import org.centrexcursionistalcoi.app.assertStatusCode
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.data.DepartmentRole
import org.centrexcursionistalcoi.app.database.entity.DepartmentEntity
import org.centrexcursionistalcoi.app.database.entity.DepartmentMemberEntity
import org.centrexcursionistalcoi.app.database.entity.QualificationEntity
import org.centrexcursionistalcoi.app.database.table.UserQualifications
import org.centrexcursionistalcoi.app.test.FakeUser
import org.centrexcursionistalcoi.app.test.FakeUser2
import org.centrexcursionistalcoi.app.test.LoginType
import org.centrexcursionistalcoi.app.utils.toUUID
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.insert
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Qualifications and their grants are embedded on `GET /departments/{id}` (see `Departments.extraColumns` and
 * `DepartmentEntity.visibleQualificationGrantsFor`) rather than fetched through their own listing routes -- see
 * `TestQualificationsRoutes.kt` for the mutation routes (create/update/delete/grant/revoke), unaffected by this.
 */
class TestDepartmentQualifications : ApplicationTestBase() {
    private val departmentId = "d5f6a6d0-6b0d-4e2a-9f9a-2b7f6a5b0001".toUUID()
    private val qualificationId = "6f4a2b5e-2d0c-4a7a-9b3e-1c8a6f2b0002".toUUID()

    /** Seeds "Test Department" with the qualification "Lead climbing" and a grant of it to [FakeUser2]. */
    private fun JdbcTransaction.seed(callerRoles: List<DepartmentRole>?) {
        val department = DepartmentEntity.new(departmentId) { displayName = "Test Department" }
        QualificationEntity.new(qualificationId) {
            this.department = department
            name = "Lead climbing"
        }
        if (callerRoles != null) {
            DepartmentMemberEntity.new {
                userReference = FakeUser.provideEntity()
                this.department = department
                confirmed = true
                roles = callerRoles
            }
        }
        DepartmentMemberEntity.new {
            userReference = FakeUser2.provideEntity()
            this.department = department
            confirmed = true
        }
        UserQualifications.insert {
            it[qualification] = qualificationId
            it[userSub] = FakeUser2.SUB
        }
    }

    @Test
    fun test_qualifications_alwaysIncluded_regardlessOfRole() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = { seed(callerRoles = null) },
    ) {
        client.get("/departments/$departmentId").apply {
            assertStatusCode(HttpStatusCode.OK)
            assertBody(Department.serializer()) { department ->
                assertEquals(listOf("Lead climbing"), department.qualifications?.map { it.name })
            }
        }
    }

    @Test
    fun test_grants_asExaminer_seesEveryGrant() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = { seed(callerRoles = listOf(DepartmentRole.EXAMINER)) },
    ) {
        client.get("/departments/$departmentId").apply {
            assertStatusCode(HttpStatusCode.OK)
            assertBody(Department.serializer()) { department ->
                assertEquals(listOf(FakeUser2.SUB), department.qualificationGrants?.map { it.userSub })
            }
        }
    }

    @Test
    fun test_grants_asQualificationsManager_impliesExaminer_seesEveryGrant() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = { seed(callerRoles = listOf(DepartmentRole.QUALIFICATIONS_MANAGER)) },
    ) {
        client.get("/departments/$departmentId").assertBody(Department.serializer()) { department ->
            assertEquals(1, department.qualificationGrants?.size)
        }
    }

    @Test
    fun test_grants_asGlobalAdmin_seesEveryGrant() = runApplicationTest(
        shouldLogIn = LoginType.ADMIN,
        databaseInitBlock = { seed(callerRoles = null) },
    ) {
        client.get("/departments/$departmentId").assertBody(Department.serializer()) { department ->
            assertEquals(1, department.qualificationGrants?.size)
        }
    }

    @Test
    fun test_grants_plainMember_seesOnlyOwnGrant() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = {
            seed(callerRoles = emptyList())
            UserQualifications.insert {
                it[qualification] = qualificationId
                it[userSub] = FakeUser.SUB
            }
        },
    ) {
        client.get("/departments/$departmentId").assertBody(Department.serializer()) { department ->
            assertEquals(listOf(FakeUser.SUB), department.qualificationGrants?.map { it.userSub })
        }
    }

    @Test
    fun test_grants_plainMember_withNoGrantOfTheirOwn_seesNone() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = { seed(callerRoles = emptyList()) },
    ) {
        client.get("/departments/$departmentId").assertBody(Department.serializer()) { department ->
            assertEquals(emptyList(), department.qualificationGrants)
        }
    }

    @Test
    fun test_grants_nonMember_seesNone() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = { seed(callerRoles = null) },
    ) {
        client.get("/departments/$departmentId").assertBody(Department.serializer()) { department ->
            assertEquals(emptyList(), department.qualificationGrants)
        }
    }

    @Test
    fun test_qualifications_departmentWithNone_isEmptyNotNull() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = {
            DepartmentEntity.new(departmentId) { displayName = "Empty Department" }
        },
    ) {
        client.get("/departments/$departmentId").assertBody(Department.serializer()) { department ->
            assertEquals(emptyList(), department.qualifications)
            assertEquals(emptyList(), department.qualificationGrants)
        }
    }
}
