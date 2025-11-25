package org.centrexcursionistalcoi.app.routes

import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.centrexcursionistalcoi.app.ApplicationTestBase
import org.centrexcursionistalcoi.app.CEAInfo
import org.centrexcursionistalcoi.app.assertBody
import org.centrexcursionistalcoi.app.assertStatusCode
import org.centrexcursionistalcoi.app.data.DepartmentJoinRequest
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.DepartmentEntity
import org.centrexcursionistalcoi.app.database.entity.DepartmentMemberEntity
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
    fun test_join_malformedId() = runApplicationTest(
        shouldLogIn = LoginType.USER
    ) {
        client.post("/departments/abc/join").apply {
            assertStatusCode(HttpStatusCode.BadRequest)
        }
    }

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
                userSub = FakeUser.provideEntity().id
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
                userSub = FakeUser.provideEntity().id
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
    fun test_members_malformedId() = runApplicationTest(
        shouldLogIn = LoginType.ADMIN
    ) {
        client.get("/departments/abc/members").apply {
            assertStatusCode(HttpStatusCode.BadRequest)
        }
    }

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
                userSub = FakeUser.provideEntity().id
                department = mockDepartment
                confirmed = false
            }
            DepartmentMemberEntity.new {
                userSub = FakeAdminUser.provideEntity().id
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
                userSub = FakeUser.provideEntity().id
                department = mockDepartment
                confirmed = false
            }
            DepartmentMemberEntity.new {
                userSub = FakeAdminUser.provideEntity().id
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
    fun test_confirm_malformedDepartmentId() = runApplicationTest(
        shouldLogIn = LoginType.ADMIN
    ) {
        client.post("/departments/abc/confirm/abc").apply {
            assertStatusCode(HttpStatusCode.BadRequest)
        }
    }

    @Test
    fun test_confirm_departmentNotFound() = runApplicationTest(
        shouldLogIn = LoginType.ADMIN
    ) {
        client.post("/departments/$departmentId/confirm/abc").apply {
            assertStatusCode(HttpStatusCode.NotFound)
        }
    }

    @Test
    fun test_confirm_malformedRequestId() = runApplicationTest(
        shouldLogIn = LoginType.ADMIN,
        databaseInitBlock = {
            DepartmentEntity.new(departmentId) {
                displayName = "Test Department"
            }
        }
    ) {
        client.post("/departments/$departmentId/confirm/abc").apply {
            assertStatusCode(HttpStatusCode.BadRequest)
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
                userSub = FakeUser.provideEntity().id
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
                userSub = FakeUser.provideEntity().id
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
}
