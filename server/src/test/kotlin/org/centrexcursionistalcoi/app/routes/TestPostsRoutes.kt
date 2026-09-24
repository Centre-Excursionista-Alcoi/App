package org.centrexcursionistalcoi.app.routes

import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.centrexcursionistalcoi.app.ApplicationTestBase
import org.centrexcursionistalcoi.app.ResourcesUtils
import org.centrexcursionistalcoi.app.assertStatusCode
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.DepartmentEntity
import org.centrexcursionistalcoi.app.database.entity.FileEntity
import org.centrexcursionistalcoi.app.database.entity.PostEntity
import org.centrexcursionistalcoi.app.database.table.DepartmentMembers
import org.centrexcursionistalcoi.app.database.table.PostFiles
import org.centrexcursionistalcoi.app.test.FakeUser
import org.centrexcursionistalcoi.app.test.LoginType
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll

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

    // deleteReferencesCheck previously compared Posts.department against the post's own id (copy-pasted from a
    // department-scoped check and never adapted -- see EventsRoutes.kt's identical pattern, where it was
    // actually load-bearing), which could never match. It's been removed entirely: PostFiles.post has
    // onDelete = ReferenceOption.CASCADE, so the DB already drops the join row cleanly with no check needed.
    // This test guards that a post with an attached file still deletes cleanly (no raw FK exception) and its
    // PostFiles join row is gone afterward.
    @Test
    fun test_delete_post_withAttachedFile_succeedsAndCascadesJoinRow() = runApplicationTest(
        shouldLogIn = LoginType.ADMIN,
        databaseInitBlock = {
            val file = FileEntity.new {
                name = "square.png"
                contentType = ContentType.Image.PNG
                bytes = ResourcesUtils.bytesFromResource("/square.png")
            }
            val post = PostEntity.new {
                title = "Post with a file"
                content = "Has an attached file"
            }
            PostFiles.insert {
                it[this.post] = post.id
                it[this.file] = file.id
            }
            post
        },
    ) { context ->
        val post = context.dibResult!!

        client.delete("/posts/${post.id.value}").assertStatusCode(HttpStatusCode.NoContent)

        val remainingPost = Database { PostEntity.findById(post.id) }
        assertNull(remainingPost, "Post should have been deleted")

        val remainingJoinRows = Database {
            PostFiles.selectAll().where { PostFiles.post eq post.id.value }.count()
        }
        assertEquals(0, remainingJoinRows, "PostFiles join row should have cascaded away with the post")
    }
}
