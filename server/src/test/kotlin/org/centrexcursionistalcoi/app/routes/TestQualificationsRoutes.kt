package org.centrexcursionistalcoi.app.routes

import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.uuid.toKotlinUuid
import kotlinx.serialization.KSerializer
import org.centrexcursionistalcoi.app.ApplicationTestBase
import org.centrexcursionistalcoi.app.assertBody
import org.centrexcursionistalcoi.app.assertError
import org.centrexcursionistalcoi.app.assertStatusCode
import org.centrexcursionistalcoi.app.data.DepartmentRole
import org.centrexcursionistalcoi.app.data.DepartmentRosterMember
import org.centrexcursionistalcoi.app.data.Qualification
import org.centrexcursionistalcoi.app.data.QualificationGrant
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.DepartmentEntity
import org.centrexcursionistalcoi.app.database.entity.DepartmentMemberEntity
import org.centrexcursionistalcoi.app.database.entity.QualificationEntity
import org.centrexcursionistalcoi.app.database.table.UserQualifications
import org.centrexcursionistalcoi.app.error.Error
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.request.CreateQualificationRequest
import org.centrexcursionistalcoi.app.request.GrantQualificationRequest
import org.centrexcursionistalcoi.app.request.UpdateQualificationRequest
import org.centrexcursionistalcoi.app.serialization.list
import org.centrexcursionistalcoi.app.test.FakeAdminUser
import org.centrexcursionistalcoi.app.test.FakeUser
import org.centrexcursionistalcoi.app.test.FakeUser2
import org.centrexcursionistalcoi.app.test.LoginType
import org.centrexcursionistalcoi.app.test.StubUser
import org.centrexcursionistalcoi.app.utils.toUUID
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll

class TestQualificationsRoutes : ApplicationTestBase() {
    private val departmentId = "54015d8b-951b-4492-b2a8-847f88d1f457".toUUID()
    private val qualificationId = "0c1b7a51-4d5a-4c88-a0b5-3e7ab3a9d001".toUUID()
    private val otherDepartmentId = "6f2d0b4e-1b0c-4a39-9d55-0c6a3c1de002".toUUID()
    private val otherQualificationId = "b8a4c2f7-5e83-4f6b-8d3e-2a9c7f5b0003".toUUID()

    /**
     * Seeds "Test Department" with the qualification "Lead climbing", the [caller] holding [callerRoles] in it
     * (or not a member at all, if `null`), and [FakeUser2] as a confirmed plain member. Also seeds a second
     * department, of which nobody is a member, with its own qualification.
     */
    private fun JdbcTransaction.seed(callerRoles: List<DepartmentRole>?, caller: StubUser = FakeUser) {
        val department = DepartmentEntity.new(departmentId) { displayName = "Test Department" }
        val otherDepartment = DepartmentEntity.new(otherDepartmentId) { displayName = "Other Department" }
        QualificationEntity.new(qualificationId) {
            this.department = department
            name = "Lead climbing"
        }
        QualificationEntity.new(otherQualificationId) {
            this.department = otherDepartment
            name = "Ice climbing"
        }
        if (callerRoles != null) {
            DepartmentMemberEntity.new {
                userReference = caller.provideEntity()
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
    }

    private suspend fun HttpClient.postJson(url: String, body: String): HttpResponse = post(url) {
        contentType(ContentType.Application.Json)
        setBody(body)
    }

    private suspend fun <T> HttpClient.postJson(url: String, serializer: KSerializer<T>, body: T) =
        postJson(url, json.encodeToString(serializer, body))

    private fun grantRow(sub: String, id: java.util.UUID = qualificationId) = Database {
        UserQualifications.selectAll()
            .where { (UserQualifications.qualification eq id) and (UserQualifications.userSub eq sub) }
            .firstOrNull()
    }

    // ---- Definitions: listing ----

    @Test
    fun test_list_notLoggedIn() = ProvidedRouteTests.test_notLoggedIn("/qualifications")

    @Test
    fun test_list_anyLoggedInUser_seesAllDefinitions() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = { seed(callerRoles = null) },
    ) {
        client.get("/qualifications").apply {
            assertStatusCode(HttpStatusCode.OK)
            assertBody(Qualification.serializer().list()) { list ->
                // Sorted by name, from every department
                assertEquals(listOf("Ice climbing", "Lead climbing"), list.map { it.name })
            }
        }
    }

    // ---- Definitions: single ----

    @Test
    fun test_get_notLoggedIn() = ProvidedRouteTests.test_notLoggedIn("/qualifications/$qualificationId")

    @Test
    fun test_get_anyLoggedInUser_canReadAnyDefinition() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        // not a member of either department: definitions are public
        databaseInitBlock = { seed(callerRoles = null) },
    ) {
        client.get("/qualifications/$qualificationId").apply {
            assertStatusCode(HttpStatusCode.OK)
            assertBody(Qualification.serializer()) {
                assertEquals(qualificationId.toKotlinUuid(), it.id)
                assertEquals("Lead climbing", it.name)
                assertEquals(departmentId.toKotlinUuid(), it.departmentId)
            }
        }
        // ... including one from another department
        client.get("/qualifications/$otherQualificationId").apply {
            assertStatusCode(HttpStatusCode.OK)
            assertBody(Qualification.serializer()) { assertEquals("Ice climbing", it.name) }
        }
    }

    @Test
    fun test_get_notFound() = runApplicationTest(shouldLogIn = LoginType.USER) {
        client.get("/qualifications/$qualificationId").assertError(Error.EntityNotFound(QualificationEntity::class, qualificationId))
    }

    @Test
    fun test_get_malformedId() = runApplicationTest(shouldLogIn = LoginType.USER) {
        client.get("/qualifications/nope").assertError(Error.MalformedId())
    }

    // ---- Definitions: create ----

    @Test
    fun test_create_notLoggedIn() = ProvidedRouteTests.test_notLoggedIn("/departments/$departmentId/qualifications", HttpMethod.Post)

    @Test
    fun test_create_plainMember_forbidden() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = { seed(callerRoles = emptyList()) },
    ) {
        client.postJson("/departments/$departmentId/qualifications", CreateQualificationRequest.serializer(), CreateQualificationRequest("Rappel"))
            .assertError(Error.PermissionRejected())
    }

    @Test
    fun test_create_examinerOnly_forbidden() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = { seed(callerRoles = listOf(DepartmentRole.EXAMINER)) },
    ) {
        client.postJson("/departments/$departmentId/qualifications", CreateQualificationRequest.serializer(), CreateQualificationRequest("Rappel"))
            .assertError(Error.PermissionRejected())
    }

    @Test
    fun test_create_managerOfAnotherDepartment_forbidden() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = { seed(callerRoles = listOf(DepartmentRole.QUALIFICATIONS_MANAGER)) },
    ) {
        // The caller manages qualifications in "Test Department", not in the other one
        client.postJson("/departments/$otherDepartmentId/qualifications", CreateQualificationRequest.serializer(), CreateQualificationRequest("Rappel"))
            .assertError(Error.PermissionRejected())
    }

    @Test
    fun test_create_asQualificationsManager() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = { seed(callerRoles = listOf(DepartmentRole.QUALIFICATIONS_MANAGER)) },
    ) {
        client.postJson(
            "/departments/$departmentId/qualifications",
            CreateQualificationRequest.serializer(),
            CreateQualificationRequest("  Rappel  ", "  Can set up a rappel  "),
        ).apply {
            assertStatusCode(HttpStatusCode.Created)
            assertBody(Qualification.serializer()) { created ->
                assertEquals("Rappel", created.name)
                assertEquals("Can set up a rappel", created.description)
                assertEquals(departmentId.toKotlinUuid(), created.departmentId)
            }
        }
        assertEquals(1, Database { QualificationEntity.all().count { it.name == "Rappel" } })
    }

    @Test
    fun test_create_asDepartmentAdmin() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = { seed(callerRoles = listOf(DepartmentRole.ADMIN)) },
    ) {
        client.postJson("/departments/$departmentId/qualifications", CreateQualificationRequest.serializer(), CreateQualificationRequest("Rappel"))
            .assertStatusCode(HttpStatusCode.Created)
    }

    @Test
    fun test_create_asGlobalAdmin() = runApplicationTest(
        shouldLogIn = LoginType.ADMIN,
        databaseInitBlock = { seed(callerRoles = null) },
    ) {
        client.postJson("/departments/$departmentId/qualifications", CreateQualificationRequest.serializer(), CreateQualificationRequest("Rappel"))
            .assertStatusCode(HttpStatusCode.Created)
    }

    @Test
    fun test_create_duplicateNameInDepartment_caseInsensitive() = runApplicationTest(
        shouldLogIn = LoginType.ADMIN,
        databaseInitBlock = { seed(callerRoles = null) },
    ) {
        client.postJson("/departments/$departmentId/qualifications", CreateQualificationRequest.serializer(), CreateQualificationRequest("lead CLIMBING"))
            .assertError(Error.QualificationAlreadyExists())
    }

    @Test
    fun test_create_sameNameInAnotherDepartment_allowed() = runApplicationTest(
        shouldLogIn = LoginType.ADMIN,
        databaseInitBlock = { seed(callerRoles = null) },
    ) {
        client.postJson("/departments/$otherDepartmentId/qualifications", CreateQualificationRequest.serializer(), CreateQualificationRequest("Lead climbing"))
            .assertStatusCode(HttpStatusCode.Created)
    }

    @Test
    fun test_create_blankName() = runApplicationTest(
        shouldLogIn = LoginType.ADMIN,
        databaseInitBlock = { seed(callerRoles = null) },
    ) {
        client.postJson("/departments/$departmentId/qualifications", CreateQualificationRequest.serializer(), CreateQualificationRequest("   "))
            .assertError(Error.InvalidArgument("name"))
    }

    @Test
    fun test_create_malformedBody() = runApplicationTest(
        shouldLogIn = LoginType.ADMIN,
        databaseInitBlock = { seed(callerRoles = null) },
    ) {
        client.postJson("/departments/$departmentId/qualifications", """{"nope":1}""")
            .assertError(Error.MalformedRequest())
    }

    @Test
    fun test_create_unknownDepartment() = runApplicationTest(shouldLogIn = LoginType.USER) {
        client.postJson("/departments/$departmentId/qualifications", CreateQualificationRequest.serializer(), CreateQualificationRequest("Rappel"))
            .assertError(Error.PermissionRejected())
    }

    // ---- Definitions: update / delete ----

    @Test
    fun test_patch_asQualificationsManager() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = { seed(callerRoles = listOf(DepartmentRole.QUALIFICATIONS_MANAGER)) },
    ) {
        client.patch("/qualifications/$qualificationId") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(UpdateQualificationRequest.serializer(), UpdateQualificationRequest(name = "Sport climbing", description = "Leads sport routes")))
        }.apply {
            assertStatusCode(HttpStatusCode.OK)
            assertBody(Qualification.serializer()) {
                assertEquals("Sport climbing", it.name)
                assertEquals("Leads sport routes", it.description)
            }
        }
    }

    @Test
    fun test_patch_blankDescription_clearsIt() = runApplicationTest(
        shouldLogIn = LoginType.ADMIN,
        databaseInitBlock = {
            seed(callerRoles = null)
            QualificationEntity[qualificationId].description = "Something"
        },
    ) {
        client.patch("/qualifications/$qualificationId") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(UpdateQualificationRequest.serializer(), UpdateQualificationRequest(description = " ")))
        }.assertStatusCode(HttpStatusCode.OK)
        assertNull(Database { QualificationEntity[qualificationId].description })
    }

    @Test
    fun test_patch_renameToExistingName() = runApplicationTest(
        shouldLogIn = LoginType.ADMIN,
        databaseInitBlock = {
            seed(callerRoles = null)
            QualificationEntity.new { department = DepartmentEntity[departmentId]; name = "Rappel" }
        },
    ) {
        client.patch("/qualifications/$qualificationId") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(UpdateQualificationRequest.serializer(), UpdateQualificationRequest(name = "rappel")))
        }.assertError(Error.QualificationAlreadyExists())
    }

    @Test
    fun test_patch_changeOnlyCase_ofOwnName_allowed() = runApplicationTest(
        shouldLogIn = LoginType.ADMIN,
        databaseInitBlock = { seed(callerRoles = null) },
    ) {
        client.patch("/qualifications/$qualificationId") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(UpdateQualificationRequest.serializer(), UpdateQualificationRequest(name = "LEAD CLIMBING")))
        }.assertStatusCode(HttpStatusCode.OK)
    }

    @Test
    fun test_patch_nothingToUpdate() = runApplicationTest(
        shouldLogIn = LoginType.ADMIN,
        databaseInitBlock = { seed(callerRoles = null) },
    ) {
        client.patch("/qualifications/$qualificationId") {
            contentType(ContentType.Application.Json)
            setBody("{}")
        }.assertError(Error.NothingToUpdate())
    }

    @Test
    fun test_patch_examinerOnly_forbidden() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = { seed(callerRoles = listOf(DepartmentRole.EXAMINER)) },
    ) {
        client.patch("/qualifications/$qualificationId") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(UpdateQualificationRequest.serializer(), UpdateQualificationRequest(name = "Nope")))
        }.assertError(Error.PermissionRejected())
        assertEquals("Lead climbing", Database { QualificationEntity[qualificationId].name })
    }

    @Test
    fun test_patch_notFound() = runApplicationTest(shouldLogIn = LoginType.ADMIN) {
        client.patch("/qualifications/$qualificationId") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(UpdateQualificationRequest.serializer(), UpdateQualificationRequest(name = "Nope")))
        }.assertError(Error.EntityNotFound(QualificationEntity::class, qualificationId))
    }

    @Test
    fun test_delete_asQualificationsManager_alsoDeletesGrants() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = {
            seed(callerRoles = listOf(DepartmentRole.QUALIFICATIONS_MANAGER))
            UserQualifications.insert {
                it[qualification] = qualificationId
                it[userSub] = FakeUser2.SUB
            }
        },
    ) {
        assertNotNull(grantRow(FakeUser2.SUB))
        client.delete("/qualifications/$qualificationId").assertStatusCode(HttpStatusCode.NoContent)
        assertNull(Database { QualificationEntity.findById(qualificationId) })
        assertNull(grantRow(FakeUser2.SUB))
    }

    @Test
    fun test_delete_examinerOnly_forbidden() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = { seed(callerRoles = listOf(DepartmentRole.EXAMINER)) },
    ) {
        client.delete("/qualifications/$qualificationId").assertError(Error.PermissionRejected())
        assertNotNull(Database { QualificationEntity.findById(qualificationId) })
    }

    // ---- Grants ----

    @Test
    fun test_grant_notLoggedIn() = ProvidedRouteTests.test_notLoggedIn("/qualifications/$qualificationId/grants", HttpMethod.Post)

    @Test
    fun test_grant_asExaminer() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = { seed(callerRoles = listOf(DepartmentRole.EXAMINER)) },
    ) {
        client.postJson("/qualifications/$qualificationId/grants", GrantQualificationRequest.serializer(), GrantQualificationRequest(FakeUser2.SUB)).apply {
            assertStatusCode(HttpStatusCode.OK)
            assertBody(QualificationGrant.serializer()) {
                assertEquals(FakeUser2.SUB, it.userSub)
                assertEquals(FakeUser.SUB, it.grantedBy)
                assertNull(it.expiresAt)
            }
        }
        assertNotNull(grantRow(FakeUser2.SUB))
    }

    @Test
    fun test_grant_asQualificationsManager_impliesExaminer() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = { seed(callerRoles = listOf(DepartmentRole.QUALIFICATIONS_MANAGER)) },
    ) {
        client.postJson("/qualifications/$qualificationId/grants", GrantQualificationRequest.serializer(), GrantQualificationRequest(FakeUser2.SUB))
            .assertStatusCode(HttpStatusCode.OK)
    }

    @Test
    fun test_grant_plainMember_forbidden() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = { seed(callerRoles = emptyList()) },
    ) {
        client.postJson("/qualifications/$qualificationId/grants", GrantQualificationRequest.serializer(), GrantQualificationRequest(FakeUser2.SUB))
            .assertError(Error.PermissionRejected())
        assertNull(grantRow(FakeUser2.SUB))
    }

    @Test
    fun test_grant_peopleManagerOnly_forbidden() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = { seed(callerRoles = listOf(DepartmentRole.PEOPLE_MANAGER)) },
    ) {
        client.postJson("/qualifications/$qualificationId/grants", GrantQualificationRequest.serializer(), GrantQualificationRequest(FakeUser2.SUB))
            .assertError(Error.PermissionRejected())
    }

    @Test
    fun test_grant_examinerOfAnotherDepartment_forbidden() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = { seed(callerRoles = listOf(DepartmentRole.EXAMINER)) },
    ) {
        // The caller is an examiner of "Test Department"; this qualification belongs to the other one
        client.postJson("/qualifications/$otherQualificationId/grants", GrantQualificationRequest.serializer(), GrantQualificationRequest(FakeUser2.SUB))
            .assertError(Error.PermissionRejected())
    }

    @Test
    fun test_grant_asGlobalAdmin() = runApplicationTest(
        shouldLogIn = LoginType.ADMIN,
        databaseInitBlock = { seed(callerRoles = null) },
    ) {
        client.postJson("/qualifications/$qualificationId/grants", GrantQualificationRequest.serializer(), GrantQualificationRequest(FakeUser2.SUB))
            .assertStatusCode(HttpStatusCode.OK)
    }

    @Test
    fun test_grant_toUnconfirmedMember_notFound() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = {
            seed(callerRoles = listOf(DepartmentRole.EXAMINER))
            DepartmentMemberEntity.new {
                userReference = FakeAdminUser.provideEntity()
                department = DepartmentEntity[departmentId]
                confirmed = false
            }
        },
    ) {
        client.postJson("/qualifications/$qualificationId/grants", GrantQualificationRequest.serializer(), GrantQualificationRequest(FakeAdminUser.SUB))
            .assertError(Error.EntityNotFound(DepartmentMemberEntity::class, FakeAdminUser.SUB))
        assertNull(grantRow(FakeAdminUser.SUB))
    }

    @Test
    fun test_grant_toNonMember_andToUnknownSub_areIndistinguishable() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = {
            seed(callerRoles = listOf(DepartmentRole.EXAMINER))
            // exists as a user, but isn't in the department
            FakeAdminUser.provideEntity()
        },
    ) {
        val nonMember = client.postJson("/qualifications/$qualificationId/grants", GrantQualificationRequest.serializer(), GrantQualificationRequest(FakeAdminUser.SUB))
        val unknown = client.postJson("/qualifications/$qualificationId/grants", GrantQualificationRequest.serializer(), GrantQualificationRequest("nobody"))
        assertEquals(HttpStatusCode.NotFound, nonMember.status)
        assertEquals(nonMember.status, unknown.status)
    }

    @Test
    fun test_grant_expiryInThePast() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = { seed(callerRoles = listOf(DepartmentRole.EXAMINER)) },
    ) {
        client.postJson(
            "/qualifications/$qualificationId/grants",
            GrantQualificationRequest.serializer(),
            GrantQualificationRequest(FakeUser2.SUB, Clock.System.now() - 1.days),
        ).assertError(Error.DateMustBeInFuture())
        assertNull(grantRow(FakeUser2.SUB))
    }

    @Test
    fun test_grant_again_replacesTheExistingGrant() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = { seed(callerRoles = listOf(DepartmentRole.EXAMINER)) },
    ) {
        client.postJson("/qualifications/$qualificationId/grants", GrantQualificationRequest.serializer(), GrantQualificationRequest(FakeUser2.SUB))
            .assertStatusCode(HttpStatusCode.OK)
        assertNull(grantRow(FakeUser2.SUB)!![UserQualifications.expiresAt])

        client.postJson(
            "/qualifications/$qualificationId/grants",
            GrantQualificationRequest.serializer(),
            GrantQualificationRequest(FakeUser2.SUB, Clock.System.now() + 30.days),
        ).assertStatusCode(HttpStatusCode.OK)

        assertNotNull(grantRow(FakeUser2.SUB)!![UserQualifications.expiresAt])
        assertEquals(1, Database { UserQualifications.selectAll().count() })
    }

    // ---- Grants: reading ----

    @Test
    fun test_grants_list_asExaminer_includesExpired() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = {
            seed(callerRoles = listOf(DepartmentRole.EXAMINER))
            UserQualifications.insert {
                it[qualification] = qualificationId
                it[userSub] = FakeUser2.SUB
                it[expiresAt] = java.time.Instant.now().minusSeconds(3600)
            }
        },
    ) {
        client.get("/qualifications/$qualificationId/grants").apply {
            assertStatusCode(HttpStatusCode.OK)
            assertBody(QualificationGrant.serializer().list()) {
                assertEquals(listOf(FakeUser2.SUB), it.map { grant -> grant.userSub })
                assertNotNull(it[0].expiresAt)
            }
        }
    }

    @Test
    fun test_grants_list_asPeopleManager() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = { seed(callerRoles = listOf(DepartmentRole.PEOPLE_MANAGER)) },
    ) {
        client.get("/qualifications/$qualificationId/grants").assertStatusCode(HttpStatusCode.OK)
    }

    @Test
    fun test_grants_list_plainMember_forbidden() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = {
            seed(callerRoles = emptyList())
            UserQualifications.insert {
                it[qualification] = qualificationId
                it[userSub] = FakeUser2.SUB
            }
        },
    ) {
        client.get("/qualifications/$qualificationId/grants").assertError(Error.PermissionRejected())
    }

    @Test
    fun test_grants_list_nonMember_forbidden() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = { seed(callerRoles = null) },
    ) {
        client.get("/qualifications/$qualificationId/grants").assertError(Error.PermissionRejected())
    }

    @Test
    fun test_profile_qualifications_onlyOwn_includingExpired() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = {
            seed(callerRoles = emptyList())
            UserQualifications.insert {
                it[qualification] = qualificationId
                it[userSub] = FakeUser.SUB
                it[expiresAt] = java.time.Instant.now().minusSeconds(3600)
            }
            UserQualifications.insert {
                it[qualification] = otherQualificationId
                it[userSub] = FakeUser.SUB
            }
            // somebody else's, must not show up
            UserQualifications.insert {
                it[qualification] = qualificationId
                it[userSub] = FakeUser2.SUB
            }
        },
    ) {
        client.get("/profile/qualifications").apply {
            assertStatusCode(HttpStatusCode.OK)
            assertBody(QualificationGrant.serializer().list()) { grants ->
                assertEquals(setOf(FakeUser.SUB), grants.map { it.userSub }.toSet())
                assertEquals(2, grants.size)
            }
        }
    }

    @Test
    fun test_profile_qualifications_notLoggedIn() = ProvidedRouteTests.test_notLoggedIn("/profile/qualifications")

    // ---- Grants: revoking ----

    @Test
    fun test_revoke_asExaminer() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = {
            seed(callerRoles = listOf(DepartmentRole.EXAMINER))
            UserQualifications.insert {
                it[qualification] = qualificationId
                it[userSub] = FakeUser2.SUB
            }
        },
    ) {
        client.delete("/qualifications/$qualificationId/grants/${FakeUser2.SUB}").assertStatusCode(HttpStatusCode.NoContent)
        assertNull(grantRow(FakeUser2.SUB))
        // Idempotent
        client.delete("/qualifications/$qualificationId/grants/${FakeUser2.SUB}").assertStatusCode(HttpStatusCode.NoContent)
    }

    @Test
    fun test_revoke_plainMember_forbidden() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = {
            seed(callerRoles = emptyList())
            UserQualifications.insert {
                it[qualification] = qualificationId
                it[userSub] = FakeUser2.SUB
            }
        },
    ) {
        client.delete("/qualifications/$qualificationId/grants/${FakeUser2.SUB}").assertError(Error.PermissionRejected())
        assertNotNull(grantRow(FakeUser2.SUB))
    }

    // ---- Roster ----

    @Test
    fun test_roster_asExaminer_onlyConfirmedMembers_onlyIdentifyingFields() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = {
            seed(callerRoles = listOf(DepartmentRole.EXAMINER))
            // A pending join request must not be listed
            DepartmentMemberEntity.new {
                userReference = FakeAdminUser.provideEntity()
                department = DepartmentEntity[departmentId]
                confirmed = false
            }
        },
    ) {
        client.get("/departments/$departmentId/roster").apply {
            assertStatusCode(HttpStatusCode.OK)
            assertBody { body ->
                // Only the id and name of each member, nothing else
                assertTrue("@" !in body, "the roster must not expose emails: $body")
                assertTrue(FakeUser.NIF !in body && FakeUser2.NIF !in body, "the roster must not expose NIFs: $body")
            }
        }
        client.get("/departments/$departmentId/roster").assertBody(DepartmentRosterMember.serializer().list()) { roster ->
            assertEquals(
                setOf(FakeUser.SUB, FakeUser2.SUB),
                roster.map { it.sub }.toSet(),
            )
            assertEquals(FakeUser2.FULL_NAME, roster.first { it.sub == FakeUser2.SUB }.fullName)
        }
    }

    @Test
    fun test_roster_search() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = { seed(callerRoles = listOf(DepartmentRole.EXAMINER)) },
    ) {
        val fragment = FakeUser2.FULL_NAME.take(4).uppercase()
        client.get("/departments/$departmentId/roster?q=$fragment").assertBody(DepartmentRosterMember.serializer().list()) { roster ->
            assertTrue(roster.all { it.fullName.contains(fragment, ignoreCase = true) })
            assertTrue(roster.any { it.sub == FakeUser2.SUB })
        }
        client.get("/departments/$departmentId/roster?q=zzzz-no-match").assertBody(DepartmentRosterMember.serializer().list()) {
            assertTrue(it.isEmpty())
        }
    }

    @Test
    fun test_roster_limit() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = { seed(callerRoles = listOf(DepartmentRole.EXAMINER)) },
    ) {
        client.get("/departments/$departmentId/roster?limit=1").assertBody(DepartmentRosterMember.serializer().list()) {
            assertEquals(1, it.size)
        }
    }

    @Test
    fun test_roster_asQualificationsManager() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = { seed(callerRoles = listOf(DepartmentRole.QUALIFICATIONS_MANAGER)) },
    ) {
        client.get("/departments/$departmentId/roster").assertStatusCode(HttpStatusCode.OK)
    }

    @Test
    fun test_roster_plainMember_forbidden() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = { seed(callerRoles = emptyList()) },
    ) {
        client.get("/departments/$departmentId/roster").assertError(Error.PermissionRejected())
    }

    @Test
    fun test_roster_peopleManagerOnly_forbidden() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = { seed(callerRoles = listOf(DepartmentRole.PEOPLE_MANAGER)) },
    ) {
        client.get("/departments/$departmentId/roster").assertError(Error.PermissionRejected())
    }

    @Test
    fun test_roster_examinerOfAnotherDepartment_forbidden() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = { seed(callerRoles = listOf(DepartmentRole.EXAMINER)) },
    ) {
        client.get("/departments/$otherDepartmentId/roster").assertError(Error.PermissionRejected())
    }
}
