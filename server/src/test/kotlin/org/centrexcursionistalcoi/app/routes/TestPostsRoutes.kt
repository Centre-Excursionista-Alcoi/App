package org.centrexcursionistalcoi.app.routes

import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import org.centrexcursionistalcoi.app.ApplicationTestBase
import org.centrexcursionistalcoi.app.assertStatusCode
import org.centrexcursionistalcoi.app.database.entity.DepartmentEntity
import org.centrexcursionistalcoi.app.database.entity.PostEntity
import org.centrexcursionistalcoi.app.database.table.DepartmentMembers
import org.centrexcursionistalcoi.app.test.FakeUser
import org.centrexcursionistalcoi.app.test.LoginType
import org.jetbrains.exposed.v1.jdbc.insert

/**
 * Regression coverage for GET /posts/{id}: it previously fetched the entity directly with no check at all,
 * bypassing the department-scoped visibility GET /posts (its listProvider) already enforces, and leaking a
 * department-private post's title/content/attached files to any caller who knew or guessed its id.
 */
class TestPostsRoutes : ApplicationTestBase() {
    @Test
    fun test_get_post_byId_notLoggedIn_privateDepartmentPost_notVisible() = runApplicationTest(
        databaseInitBlock = {
            val department = DepartmentEntity.new { displayName = "Private Department" }
            PostEntity.new {
                title = "Private post"
                content = "Only for department members"
                this.department = department
            }
        },
    ) { context ->
        val post = context.dibResult!!

        client.get("/posts/${post.id.value}").assertStatusCode(HttpStatusCode.NotFound)
    }

    @Test
    fun test_get_post_byId_loggedIn_privateDepartmentPost_notVisibleToOutsider() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = {
            FakeUser.provideEntity()
            val otherDepartment = DepartmentEntity.new { displayName = "Other Department" }
            PostEntity.new {
                title = "Private post"
                content = "Only for department members"
                this.department = otherDepartment
            }
        },
    ) { context ->
        val post = context.dibResult!!

        // FakeUser is logged in, but not a member of the post's department.
        client.get("/posts/${post.id.value}").assertStatusCode(HttpStatusCode.NotFound)
    }

    @Test
    fun test_get_post_byId_departmentMember_visible() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = {
            FakeUser.provideEntity()
            val department = DepartmentEntity.new { displayName = "Managed Department" }
            DepartmentMembers.insert {
                it[userSub] = FakeUser.SUB
                it[departmentId] = department.id
                it[confirmed] = true
                it[roles] = emptyList()
            }
            PostEntity.new {
                title = "Department post"
                content = "Visible to department members"
                this.department = department
            }
        },
    ) { context ->
        val post = context.dibResult!!

        client.get("/posts/${post.id.value}").assertStatusCode(HttpStatusCode.OK)
    }

    @Test
    fun test_get_post_byId_publicPost_visibleToAnyone() = runApplicationTest(
        databaseInitBlock = {
            PostEntity.new {
                title = "Public post"
                content = "Visible to everyone"
            }
        },
    ) { context ->
        val post = context.dibResult!!

        client.get("/posts/${post.id.value}").assertStatusCode(HttpStatusCode.OK)
    }
}
