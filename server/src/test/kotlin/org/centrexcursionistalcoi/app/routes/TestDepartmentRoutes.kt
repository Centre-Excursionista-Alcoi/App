package org.centrexcursionistalcoi.app.routes

import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.centrexcursionistalcoi.app.ApplicationTestBase
import org.centrexcursionistalcoi.app.CEAInfo
import org.centrexcursionistalcoi.app.assertBody
import org.centrexcursionistalcoi.app.assertStatusCode
import org.centrexcursionistalcoi.app.data.DepartmentJoinRequestsResponse
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.DepartmentEntity
import org.centrexcursionistalcoi.app.database.entity.DepartmentMemberEntity

class TestDepartmentRoutes : ApplicationTestBase() {
    @Test
    fun test_join_notLoggedIn() = ProvidedRouteTests.test_notLoggedIn("/departments/123/join", HttpMethod.Post)

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
        client.post("/departments/123/join").apply {
            assertStatusCode(HttpStatusCode.NotFound)
        }
    }

    @Test
    fun test_join_alreadyJoined_pendingConfirmation() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = {
            DepartmentMemberEntity.new {
                userSub = FakeUser.provideEntity().id
                department = DepartmentEntity.new(123) {
                    displayName = "Test Department"
                }
                confirmed = false
            }
        }
    ) {
        client.post("/departments/123/join").apply {
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
                department = DepartmentEntity.new(123) {
                    displayName = "Test Department"
                }
                confirmed = true
            }
        }
    ) {
        client.post("/departments/123/join").apply {
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
            DepartmentEntity.new(123) {
                displayName = "Test Department"
            }
        }
    ) {
        client.post("/departments/123/join").apply {
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
            DepartmentEntity.new(123) {
                displayName = "Test Department"
            }
        }
    ) {
        client.post("/departments/123/join").apply {
            assertStatusCode(HttpStatusCode.OK)
            val header = headers[HttpHeaders.CEAInfo]
            assertNotNull(header)
            assertEquals("member", header)
        }
    }


    @Test
    fun test_requests_notLoggedIn() = ProvidedRouteTests.test_notLoggedIn("/departments/123/requests")

    @Test
    fun test_requests_notAdmin() = runApplicationTest(
        shouldLogIn = LoginType.USER
    ) {
        client.get("/departments/abc/requests").apply {
            assertStatusCode(HttpStatusCode.Forbidden)
        }
    }

    @Test
    fun test_requests_malformedId() = runApplicationTest(
        shouldLogIn = LoginType.ADMIN
    ) {
        client.get("/departments/abc/requests").apply {
            assertStatusCode(HttpStatusCode.BadRequest)
        }
    }

    @Test
    fun test_requests_departmentNotFound() = runApplicationTest(
        shouldLogIn = LoginType.ADMIN
    ) {
        client.get("/departments/123/requests").apply {
            assertStatusCode(HttpStatusCode.NotFound)
        }
    }

    @Test
    fun test_requests_correct() = runApplicationTest(
        shouldLogIn = LoginType.ADMIN,
        databaseInitBlock = {
            val mockDepartment = DepartmentEntity.new(123) {
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
        client.get("/departments/123/requests").apply {
            assertStatusCode(HttpStatusCode.OK)
            assertBody(DepartmentJoinRequestsResponse.serializer()) { response ->
                assertEquals(1, response.requests.size)
                val request = response.requests[0]
                assertEquals(FakeUser.SUB, request.userSub)
                // ID is non-deterministic, just check it's positive
                assert(request.requestId > 0)
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
        client.post("/departments/123/confirm/abc").apply {
            assertStatusCode(HttpStatusCode.NotFound)
        }
    }

    @Test
    fun test_confirm_malformedRequestId() = runApplicationTest(
        shouldLogIn = LoginType.ADMIN,
        databaseInitBlock = {
            DepartmentEntity.new(123) {
                displayName = "Test Department"
            }
        }
    ) {
        client.post("/departments/123/confirm/abc").apply {
            assertStatusCode(HttpStatusCode.BadRequest)
        }
    }

    @Test
    fun test_confirm_requestNotFound() = runApplicationTest(
        shouldLogIn = LoginType.ADMIN,
        databaseInitBlock = {
            DepartmentEntity.new(123) {
                displayName = "Test Department"
            }
        }
    ) {
        client.post("/departments/123/confirm/123").apply {
            assertStatusCode(HttpStatusCode.NotFound)
        }
    }

    @Test
    fun test_confirm_alreadyConfirmed() = runApplicationTest(
        shouldLogIn = LoginType.ADMIN,
        databaseInitBlock = {
            val mockDepartment = DepartmentEntity.new(123) {
                displayName = "Test Department"
            }
            DepartmentMemberEntity.new(123) {
                userSub = FakeUser.provideEntity().id
                department = mockDepartment
                confirmed = true
            }
        }
    ) {
        client.post("/departments/123/confirm/123").apply {
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
            val mockDepartment = DepartmentEntity.new(123) {
                displayName = "Test Department"
            }
            DepartmentMemberEntity.new(123) {
                userSub = FakeUser.provideEntity().id
                department = mockDepartment
                confirmed = false
            }
        }
    ) {
        client.post("/departments/123/confirm/123").apply {
            assertStatusCode(HttpStatusCode.OK)
        }

        val entity = Database { DepartmentMemberEntity.findById(123) }
        assertNotNull(entity)
        assertTrue { entity.confirmed }
    }
}
