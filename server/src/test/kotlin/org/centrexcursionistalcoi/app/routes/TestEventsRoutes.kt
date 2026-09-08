package org.centrexcursionistalcoi.app.routes

import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.json.JsonObject
import org.centrexcursionistalcoi.app.ApplicationTestBase
import org.centrexcursionistalcoi.app.ResourcesUtils
import org.centrexcursionistalcoi.app.assertError
import org.centrexcursionistalcoi.app.assertStatusCode
import org.centrexcursionistalcoi.app.data.DepartmentRole
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.DepartmentEntity
import org.centrexcursionistalcoi.app.database.entity.EventEntity
import org.centrexcursionistalcoi.app.database.entity.FileEntity
import org.centrexcursionistalcoi.app.database.table.DepartmentMembers
import org.centrexcursionistalcoi.app.error.Error
import org.centrexcursionistalcoi.app.ifModifiedSinceFormatter
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.test.FakeUser
import org.centrexcursionistalcoi.app.test.LoginType
import org.centrexcursionistalcoi.app.utils.toJsonElement
import org.jetbrains.exposed.v1.jdbc.insert
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Regression coverage for the CONTENT_MANAGER department-scoped write permission on `provideEntityRoutes`
 * (shared by events/posts/inventory): a manager of one department must not be able to create or reassign an
 * entity into a department they don't hold the role in, even though the coarse pre-check only requires holding
 * the role in *some* department.
 */
class TestEventsRoutes : ApplicationTestBase() {

    @Test
    fun test_create_event_contentManager_ownDepartment_succeeds() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = {
            FakeUser.provideEntity()
            val department = DepartmentEntity.new { displayName = "Managed Department" }
            DepartmentMembers.insert {
                it[userSub] = FakeUser.SUB
                it[departmentId] = department.id
                it[confirmed] = true
                it[roles] = listOf(DepartmentRole.CONTENT_MANAGER.storageName)
            }
            department
        },
    ) { context ->
        val department = context.dibResult!!

        client.submitFormWithBinaryData(
            "/events",
            formData {
                append("start", Instant.now().toEpochMilli())
                append("title", "Managed event")
                append("place", "Somewhere")
                append("department", department.id.value.toString())
            }
        ).apply {
            assertEquals(HttpStatusCode.Created, status)
            assertNotNull(headers[HttpHeaders.Location])
        }
    }

    @Test
    fun test_create_event_contentManager_otherDepartment_rejectedAndRolledBack() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = {
            FakeUser.provideEntity()
            val managed = DepartmentEntity.new { displayName = "Managed Department" }
            val other = DepartmentEntity.new { displayName = "Other Department" }
            DepartmentMembers.insert {
                it[userSub] = FakeUser.SUB
                it[departmentId] = managed.id
                it[confirmed] = true
                it[roles] = listOf(DepartmentRole.CONTENT_MANAGER.storageName)
            }
            other
        },
    ) { context ->
        val otherDepartment = context.dibResult!!

        client.submitFormWithBinaryData(
            "/events",
            formData {
                append("start", Instant.now().toEpochMilli())
                append("title", "Cross-department event")
                append("place", "Somewhere")
                append("department", otherDepartment.id.value.toString())
            }
        ).apply {
            assertError(Error.PermissionRejected())
        }

        // The rejected creation must not leave an orphaned row behind.
        val remaining = Database { EventEntity.all().toList() }
        assertEquals(0, remaining.size, "Rejected event creation should have been rolled back")
    }

    @Test
    fun test_create_event_contentManager_otherDepartment_withImage_doesNotOrphanFile() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = {
            FakeUser.provideEntity()
            val managed = DepartmentEntity.new { displayName = "Managed Department" }
            val other = DepartmentEntity.new { displayName = "Other Department" }
            DepartmentMembers.insert {
                it[userSub] = FakeUser.SUB
                it[departmentId] = managed.id
                it[confirmed] = true
                it[roles] = listOf(DepartmentRole.CONTENT_MANAGER.storageName)
            }
            other
        },
    ) { context ->
        val otherDepartment = context.dibResult!!

        client.submitFormWithBinaryData(
            "/events",
            formData {
                append("start", Instant.now().toEpochMilli())
                append("title", "Cross-department event with image")
                append("place", "Somewhere")
                append("department", otherDepartment.id.value.toString())
                append(
                    "image",
                    ResourcesUtils.bytesFromResource("/square.png"),
                    Headers.build {
                        append(HttpHeaders.ContentType, ContentType.Image.PNG.toString())
                        append(HttpHeaders.ContentDisposition, "filename=\"square.png\"")
                    },
                )
            }
        ).apply {
            assertError(Error.PermissionRejected())
        }

        // Neither the event nor the image it referenced (uploaded and persisted before the department could be
        // authorized) may survive the rejection.
        val remainingEvents = Database { EventEntity.all().toList() }
        val remainingFiles = Database { FileEntity.all().toList() }
        assertEquals(0, remainingEvents.size, "Rejected event creation should have been rolled back")
        assertEquals(0, remainingFiles.size, "Rejected event creation should not leave an orphaned file behind")
    }

    @Test
    fun test_patch_event_contentManager_cannotReassignToUnmanagedDepartment() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = {
            FakeUser.provideEntity()
            val managed = DepartmentEntity.new { displayName = "Managed Department" }
            val other = DepartmentEntity.new { displayName = "Other Department" }
            DepartmentMembers.insert {
                it[userSub] = FakeUser.SUB
                it[departmentId] = managed.id
                it[confirmed] = true
                it[roles] = listOf(DepartmentRole.CONTENT_MANAGER.storageName)
            }
            val event = EventEntity.new {
                start = Instant.now()
                title = "Managed event"
                place = "Somewhere"
                department = managed
            }
            Triple(event, managed, other)
        },
    ) { context ->
        val (event, managedDepartment, otherDepartment) = context.dibResult!!

        client.patch("/events/${event.id.value}") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(JsonObject(mapOf("department" to otherDepartment.id.value.toJsonElement()))))
        }.apply {
            assertError(Error.PermissionRejected())
        }

        // The reassignment attempt must have been rolled back: the event still belongs to the managed department.
        val departmentIdAfter = Database { event.department?.id?.value }
        assertEquals(managedDepartment.id.value, departmentIdAfter)
    }

    // GET /events/{id} must respect the same department-scoped visibility as GET /events (EventEntity.forSession):
    // it previously fetched the entity directly with no check at all, leaking a department-private event's full
    // details (including its confirmed attendee list) to any caller who knew or guessed its id.
    @Test
    fun test_get_event_byId_privateDepartmentEvent_notVisibleToOutsider() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = {
            FakeUser.provideEntity()
            val otherDepartment = DepartmentEntity.new { displayName = "Other Department" }
            EventEntity.new {
                start = Instant.now().plusSeconds(3600)
                title = "Private event"
                place = "Somewhere"
                department = otherDepartment
            }
        },
    ) { context ->
        val event = context.dibResult!!

        // FakeUser is logged in, but not a member of the event's department.
        client.get("/events/${event.id.value}").assertStatusCode(HttpStatusCode.NotFound)
    }

    @Test
    fun test_get_event_byId_privateDepartmentEvent_visibleToDepartmentMember() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = {
            FakeUser.provideEntity()
            val dept = DepartmentEntity.new { displayName = "Managed Department" }
            DepartmentMembers.insert {
                it[userSub] = FakeUser.SUB
                it[departmentId] = dept.id
                it[confirmed] = true
                it[roles] = emptyList()
            }
            EventEntity.new {
                start = Instant.now().plusSeconds(3600)
                title = "Department event"
                place = "Somewhere"
                department = dept
            }
        },
    ) { context ->
        val event = context.dibResult!!

        client.get("/events/${event.id.value}").assertStatusCode(HttpStatusCode.OK)
    }

    // Visibility must be checked before handleIfModified: otherwise an outsider could send If-Modified-Since on
    // a private event's id and get a 304 (or its Last-Modified header) back, confirming the event's existence
    // and last-modified time despite not being allowed to see it at all.
    @Test
    fun test_get_event_byId_privateDepartmentEvent_ifModifiedSince_stillNotFoundForOutsider() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = {
            FakeUser.provideEntity()
            val otherDepartment = DepartmentEntity.new { displayName = "Other Department" }
            EventEntity.new {
                start = Instant.now().plusSeconds(3600)
                title = "Private event"
                place = "Somewhere"
                department = otherDepartment
            }
        },
    ) { context ->
        val event = context.dibResult!!

        client.get("/events/${event.id.value}") {
            headers.append(HttpHeaders.IfModifiedSince, ifModifiedSinceFormatter.format(Instant.now().atZone(ZoneOffset.UTC)))
        }.assertStatusCode(HttpStatusCode.NotFound)
    }
}
