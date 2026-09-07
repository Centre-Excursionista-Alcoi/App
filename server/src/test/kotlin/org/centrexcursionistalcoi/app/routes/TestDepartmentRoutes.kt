package org.centrexcursionistalcoi.app.routes

import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.centrexcursionistalcoi.app.ApplicationTestBase
import org.centrexcursionistalcoi.app.CEAInfo
import org.centrexcursionistalcoi.app.assertBody
import org.centrexcursionistalcoi.app.assertError
import org.centrexcursionistalcoi.app.assertStatusCode
import org.centrexcursionistalcoi.app.assertSuccess
import org.centrexcursionistalcoi.app.data.DepartmentJoinRequest
import org.centrexcursionistalcoi.app.data.DepartmentRole
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.DepartmentEntity
import org.centrexcursionistalcoi.app.database.entity.DepartmentMemberEntity
import org.centrexcursionistalcoi.app.error.Error
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.request.UpdateDepartmentMemberRolesRequest
import org.centrexcursionistalcoi.app.serialization.list
import org.centrexcursionistalcoi.app.test.*
import org.centrexcursionistalcoi.app.utils.isZero
import org.centrexcursionistalcoi.app.utils.toUUID

class TestDepartmentRoutes : ApplicationTestBase() {
    private val departmentId = "54015d8b-951b-4492-b2a8-847f88d1f457".toUUID()
    private val joinRequestId = "a82b9bc2-e357-4cfb-abe0-4c5444680757".toUUID()

    @Test
    fun test_join_notLoggedIn() = ProvidedRouteTests.test_notLoggedIn("/departments/$departmentId/join", HttpMethod.Post)

    @Test
    fun test_join_departmentNotFound() = runApplicationTest(
        shouldLogIn = LoginType.USER
    ) {
        client.post("/departments/$departmentId/join").apply {
            assertStatusCode(HttpStatusCode.NotFound)
        }
    }

    @Test
    fun test_join_alreadyJoined_pendingConfirmation() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = {
            DepartmentMemberEntity.new {
                userReference = FakeUser.provideEntity()
                department = DepartmentEntity.new(departmentId) {
                    displayName = "Test Department"
                }
                confirmed = false
            }
        }
    ) {
        client.post("/departments/$departmentId/join").apply {
            assertStatusCode(HttpStatusCode.Conflict)
            val header = headers[HttpHeaders.CEAInfo]
            assertNotNull(header)
            assertEquals("pending", header)
        }
    }

    @Test
    fun test_join_alreadyJoined_member() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = {
            DepartmentMemberEntity.new {
                userReference = FakeUser.provideEntity()
                department = DepartmentEntity.new(departmentId) {
                    displayName = "Test Department"
                }
                confirmed = true
            }
        }
    ) {
        client.post("/departments/$departmentId/join").apply {
            assertStatusCode(HttpStatusCode.Conflict)
            val header = headers[HttpHeaders.CEAInfo]
            assertNotNull(header)
            assertEquals("member", header)
        }
    }

    @Test
    fun test_join_success_notAdmin() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = {
            DepartmentEntity.new(departmentId) {
                displayName = "Test Department"
            }
        }
    ) {
        client.post("/departments/$departmentId/join").apply {
            assertStatusCode(HttpStatusCode.Created)
            val header = headers[HttpHeaders.CEAInfo]
            assertNotNull(header)
            assertEquals("pending", header)
        }
    }

    @Test
    fun test_join_success_isAdmin() = runApplicationTest(
        shouldLogIn = LoginType.ADMIN,
        databaseInitBlock = {
            DepartmentEntity.new(departmentId) {
                displayName = "Test Department"
            }
        }
    ) {
        client.post("/departments/$departmentId/join").apply {
            assertStatusCode(HttpStatusCode.OK)
            val header = headers[HttpHeaders.CEAInfo]
            assertNotNull(header)
            assertEquals("member", header)
        }
    }


    @Test
    fun test_members_notLoggedIn() = ProvidedRouteTests.test_notLoggedIn("/departments/$departmentId/members")

    @Test
    fun test_members_departmentNotFound() = runApplicationTest(
        shouldLogIn = LoginType.ADMIN
    ) {
        client.get("/departments/$departmentId/members").apply {
            assertStatusCode(HttpStatusCode.NotFound)
        }
    }

    @Test
    fun test_members_notAdmin() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = {
            val mockDepartment = DepartmentEntity.new(departmentId) {
                displayName = "Test Department"
            }
            DepartmentMemberEntity.new {
                userReference = FakeUser.provideEntity()
                department = mockDepartment
                confirmed = false
            }
            DepartmentMemberEntity.new {
                userReference = FakeAdminUser.provideEntity()
                department = mockDepartment
                confirmed = true
            }
        }
    ) {
        client.get("/departments/$departmentId/members").apply {
            assertStatusCode(HttpStatusCode.OK)
            assertBody(DepartmentJoinRequest.serializer().list()) { requests ->
                assertEquals(1, requests.size)
                val request = requests[0]
                assertEquals(FakeUser.SUB, request.userSub)
                // ID is non-deterministic, just check it's not zero
                assertFalse(request.requestId.isZero())
            }
        }
    }

    @Test
    fun test_members_correct() = runApplicationTest(
        shouldLogIn = LoginType.ADMIN,
        databaseInitBlock = {
            val mockDepartment = DepartmentEntity.new(departmentId) {
                displayName = "Test Department"
            }
            DepartmentMemberEntity.new {
                userReference = FakeUser.provideEntity()
                department = mockDepartment
                confirmed = false
            }
            DepartmentMemberEntity.new {
                userReference = FakeAdminUser.provideEntity()
                department = mockDepartment
                confirmed = true
            }
        }
    ) {
        client.get("/departments/$departmentId/members").apply {
            assertStatusCode(HttpStatusCode.OK)
            assertBody(DepartmentJoinRequest.serializer().list()) { requests ->
                assertEquals(2, requests.size)
                requests[0].let { request ->
                    assertEquals(FakeUser.SUB, request.userSub)
                    // ID is non-deterministic, just check it's not zero
                    assertFalse(request.requestId.isZero())
                }
                requests[1].let { request ->
                    assertEquals(FakeAdminUser.SUB, request.userSub)
                    // ID is non-deterministic, just check it's not zero
                    assertFalse(request.requestId.isZero())
                }
            }
        }
    }


    @Test
    fun test_confirm_notLoggedIn() = ProvidedRouteTests.test_notLoggedIn("/departments/abc/confirm/abc", HttpMethod.Post)

    @Test
    fun test_confirm_notAdmin() = runApplicationTest(
        shouldLogIn = LoginType.USER
    ) {
        client.post("/departments/abc/confirm/abc").apply {
            assertStatusCode(HttpStatusCode.Forbidden)
        }
    }

    @Test
    fun test_confirm_departmentNotFound() = runApplicationTest(
        shouldLogIn = LoginType.ADMIN
    ) {
        client.post("/departments/$departmentId/confirm/abc").apply {
            assertError(Error.PermissionRejected())
        }
    }

    @Test
    fun test_confirm_requestNotFound() = runApplicationTest(
        shouldLogIn = LoginType.ADMIN,
        databaseInitBlock = {
            DepartmentEntity.new(departmentId) {
                displayName = "Test Department"
            }
        }
    ) {
        client.post("/departments/$departmentId/confirm/$joinRequestId").apply {
            assertStatusCode(HttpStatusCode.NotFound)
        }
    }

    @Test
    fun test_confirm_alreadyConfirmed() = runApplicationTest(
        shouldLogIn = LoginType.ADMIN,
        databaseInitBlock = {
            val mockDepartment = DepartmentEntity.new(departmentId) {
                displayName = "Test Department"
            }
            DepartmentMemberEntity.new(joinRequestId) {
                userReference = FakeUser.provideEntity()
                department = mockDepartment
                confirmed = true
            }
        }
    ) {
        client.post("/departments/$departmentId/confirm/$joinRequestId").apply {
            assertStatusCode(HttpStatusCode.OK)
            headers[HttpHeaders.CEAInfo]?.let { ceaInfo ->
                assertEquals("member", ceaInfo)
            } ?: throw AssertionError("Missing CEA-Info header")
        }
    }

    @Test
    fun test_confirm_correct() = runApplicationTest(
        shouldLogIn = LoginType.ADMIN,
        databaseInitBlock = {
            val mockDepartment = DepartmentEntity.new(departmentId) {
                displayName = "Test Department"
            }
            DepartmentMemberEntity.new(joinRequestId) {
                userReference = FakeUser.provideEntity()
                department = mockDepartment
                confirmed = false
            }
        }
    ) {
        client.post("/departments/$departmentId/confirm/$joinRequestId").apply {
            assertStatusCode(HttpStatusCode.OK)
        }

        val entity = Database { DepartmentMemberEntity.findById(joinRequestId) }
        assertNotNull(entity)
        assertTrue { entity.confirmed }
    }


    @Test
    fun test_leave_notMember() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = {
            DepartmentEntity.new(departmentId) {
                displayName = "Test Department"
            }
        }
    ) {
        client.post("/departments/$departmentId/leave").apply {
            assertSuccess()
        }
    }

    @Test
    fun test_leave_correct() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = {
            val mockDepartment = DepartmentEntity.new(departmentId) {
                displayName = "Test Department"
            }
            DepartmentMemberEntity.new(joinRequestId) {
                userReference = FakeUser.provideEntity()
                department = mockDepartment
                confirmed = true
            }
        }
    ) { context ->
        context.dibResult!!

        client.post("/departments/$departmentId/leave").apply {
            assertSuccess()
        }

        val entity = Database { DepartmentMemberEntity.findById(context.dibResult.id) }
        assertNull(entity)
    }


    @Test
    fun test_kick_forbidden() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = {
            DepartmentEntity.new(departmentId) {
                displayName = "Test Department"
            }
        }
    ) {
        client.post("/departments/$departmentId/leave/${FakeUser.SUB}").apply {
            assertError(Error.PermissionRejected())
        }
    }

    @Test
    fun test_kick_notMember() = runApplicationTest(
        shouldLogIn = LoginType.ADMIN,
        databaseInitBlock = {
            DepartmentEntity.new(departmentId) {
                displayName = "Test Department"
            }
        }
    ) {
        client.post("/departments/$departmentId/leave/${FakeUser.SUB}").apply {
            assertError(Error.EntityNotFound(DepartmentMemberEntity::class, FakeUser.SUB))
        }
    }

    @Test
    fun test_kick_correct() = runApplicationTest(
        shouldLogIn = LoginType.ADMIN,
        databaseInitBlock = {
            val mockDepartment = DepartmentEntity.new(departmentId) {
                displayName = "Test Department"
            }
            DepartmentMemberEntity.new(joinRequestId) {
                userReference = FakeUser.provideEntity()
                department = mockDepartment
                confirmed = true
            }
        }
    ) { context ->
        context.dibResult!!

        client.post("/departments/$departmentId/leave/${FakeUser.SUB}").apply {
            assertSuccess()
        }

        val entity = Database { DepartmentMemberEntity.findById(context.dibResult.id) }
        assertNull(entity)
    }

    @Test
    fun test_setRoles_asDepartmentAdmin() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = {
            val department = DepartmentEntity.new(departmentId) {
                displayName = "Test Department"
            }
            // The logged-in user (FakeUser) is a department ADMIN, and may (re)assign roles.
            DepartmentMemberEntity.new {
                userReference = FakeUser.provideEntity()
                this.department = department
                confirmed = true
                roles = listOf(DepartmentRole.ADMIN)
            }
            // The target member being promoted to INVENTORY_MANAGER.
            DepartmentMemberEntity.new(joinRequestId) {
                userReference = FakeUser2.provideEntity()
                this.department = department
                confirmed = true
                roles = emptyList()
            }
        }
    ) { context ->
        val member = context.dibResult!!

        client.patch("/departments/$departmentId/members/${member.id.value}/roles") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(UpdateDepartmentMemberRolesRequest.serializer(), UpdateDepartmentMemberRolesRequest(listOf(DepartmentRole.INVENTORY_MANAGER))))
        }.apply {
            assertStatusCode(HttpStatusCode.NoContent)
        }

        val updatedRoles = Database { DepartmentMemberEntity.findById(member.id.value)?.roles }
        assertEquals(listOf(DepartmentRole.INVENTORY_MANAGER), updatedRoles)
    }

    @Test
    fun test_setRoles_asPeopleManagerOnly_forbidden() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = {
            val department = DepartmentEntity.new(departmentId) {
                displayName = "Test Department"
            }
            // The logged-in user (FakeUser) is only a PEOPLE_MANAGER, not a department ADMIN.
            DepartmentMemberEntity.new {
                userReference = FakeUser.provideEntity()
                this.department = department
                confirmed = true
                roles = listOf(DepartmentRole.PEOPLE_MANAGER)
            }
            DepartmentMemberEntity.new(joinRequestId) {
                userReference = FakeUser2.provideEntity()
                this.department = department
                confirmed = true
                roles = emptyList()
            }
        }
    ) { context ->
        val member = context.dibResult!!

        client.patch("/departments/$departmentId/members/${member.id.value}/roles") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(UpdateDepartmentMemberRolesRequest.serializer(), UpdateDepartmentMemberRolesRequest(listOf(DepartmentRole.INVENTORY_MANAGER))))
        }.apply {
            assertError(Error.PermissionRejected())
        }

        val updatedRoles = Database { DepartmentMemberEntity.findById(member.id.value)?.roles }
        assertEquals(emptyList(), updatedRoles)
    }

    @Test
    fun test_setRoles_asGlobalAdmin() = runApplicationTest(
        shouldLogIn = LoginType.ADMIN,
        databaseInitBlock = {
            val department = DepartmentEntity.new(departmentId) {
                displayName = "Test Department"
            }
            DepartmentMemberEntity.new(joinRequestId) {
                userReference = FakeUser.provideEntity()
                this.department = department
                confirmed = true
                roles = emptyList()
            }
        }
    ) { context ->
        val member = context.dibResult!!

        client.patch("/departments/$departmentId/members/${member.id.value}/roles") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(UpdateDepartmentMemberRolesRequest.serializer(), UpdateDepartmentMemberRolesRequest(listOf(DepartmentRole.LENDING_MANAGER))))
        }.apply {
            assertStatusCode(HttpStatusCode.NoContent)
        }

        val updatedRoles = Database { DepartmentMemberEntity.findById(member.id.value)?.roles }
        assertEquals(listOf(DepartmentRole.LENDING_MANAGER), updatedRoles)
    }
}
