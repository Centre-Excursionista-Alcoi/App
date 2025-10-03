package org.centrexcursionistalcoi.app.routes

import io.ktor.client.request.post
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.centrexcursionistalcoi.app.ApplicationTestBase
import org.centrexcursionistalcoi.app.CEAInfo
import org.centrexcursionistalcoi.app.assertStatusCode
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
                userSub = FakeUser.SUB
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
                userSub = FakeUser.SUB
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
    fun test_join_success() = runApplicationTest(
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
}
