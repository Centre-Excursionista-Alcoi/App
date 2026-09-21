package org.centrexcursionistalcoi.app.routes

import io.ktor.client.request.delete
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.toKotlinUuid
import org.centrexcursionistalcoi.app.ApplicationTestBase
import org.centrexcursionistalcoi.app.assertError
import org.centrexcursionistalcoi.app.assertStatusCode
import org.centrexcursionistalcoi.app.assertSuccess
import org.centrexcursionistalcoi.app.data.DepartmentRole
import org.centrexcursionistalcoi.app.data.Event
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.DepartmentEntity
import org.centrexcursionistalcoi.app.database.entity.DepartmentMemberEntity
import org.centrexcursionistalcoi.app.database.entity.EventEntity
import org.centrexcursionistalcoi.app.database.entity.QualificationEntity
import org.centrexcursionistalcoi.app.database.table.EventMembers
import org.centrexcursionistalcoi.app.database.table.EventQualificationRequirements
import org.centrexcursionistalcoi.app.database.table.UserQualifications
import org.centrexcursionistalcoi.app.error.Error
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.request.UpdateEventRequest
import org.centrexcursionistalcoi.app.test.FakeAdminUser
import org.centrexcursionistalcoi.app.test.FakeUser
import org.centrexcursionistalcoi.app.test.FakeUser2
import org.centrexcursionistalcoi.app.test.LoginType
import org.centrexcursionistalcoi.app.utils.toUUID
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll

/**
 * Events may require qualifications: creating/patching them, exposing them, and enforcing them when a user
 * confirms assistance. Requirements are `AND` of `OR`s (see `Event.qualificationRequirements`).
 */
class TestEventQualificationRequirements : ApplicationTestBase() {
    private val departmentId = "54015d8b-951b-4492-b2a8-847f88d1f457".toUUID()
    private val otherDepartmentId = "6f2d0b4e-1b0c-4a39-9d55-0c6a3c1de002".toUUID()

    private val basic = "0c1b7a51-4d5a-4c88-a0b5-3e7ab3a9d001".toUUID()
    private val lead = "0c1b7a51-4d5a-4c88-a0b5-3e7ab3a9d002".toUUID()
    private val topRope = "0c1b7a51-4d5a-4c88-a0b5-3e7ab3a9d003".toUUID()
    private val ice = "b8a4c2f7-5e83-4f6b-8d3e-2a9c7f5b0003".toUUID()

    private val eventId = "9d0c1b2a-3e4f-4a5b-8c6d-7e8f9a0b1c01".toUUID()

    /**
     * Seeds two departments: "Test Department", with the qualifications [basic], [lead] and [topRope], and
     * "Other Department", with [ice]. The logged-in [FakeUser] is a confirmed member of "Test Department" with
     * [callerRoles] (and of "Other Department" too, if [callerRolesInOther] isn't `null`).
     */
    private fun JdbcTransaction.seed(
        callerRoles: List<DepartmentRole> = emptyList(),
        callerRolesInOther: List<DepartmentRole>? = null,
    ) {
        val department = DepartmentEntity.new(departmentId) { displayName = "Test Department" }
        val other = DepartmentEntity.new(otherDepartmentId) { displayName = "Other Department" }
        for ((id, name) in listOf(basic to "Basic climbing", lead to "Lead climbing", topRope to "Top rope")) {
            QualificationEntity.new(id) { this.department = department; this.name = name }
        }
        QualificationEntity.new(ice) { this.department = other; name = "Ice climbing" }

        val caller = FakeUser.provideEntity()
        DepartmentMemberEntity.new {
            userReference = caller
            this.department = department
            confirmed = true
            roles = callerRoles
        }
        if (callerRolesInOther != null) {
            DepartmentMemberEntity.new {
                userReference = caller
                this.department = other
                confirmed = true
                roles = callerRolesInOther
            }
        }
        FakeUser2.provideEntity()
    }

    /** A future event of "Test Department" requiring [requirements]. */
    private fun JdbcTransaction.seedEvent(vararg requirements: List<UUID>): EventEntity =
        EventEntity.new(eventId) {
            start = Instant.now().plusSeconds(7 * 24 * 3600)
            place = "Somewhere"
            title = "Climbing day"
            department = DepartmentEntity[departmentId]
        }.also { it.setQualificationRequirements(requirements.toList()) }

    private fun JdbcTransaction.grant(qualification: UUID, sub: String = FakeUser.SUB, expiresAt: Instant? = null) {
        UserQualifications.insert {
            it[UserQualifications.qualification] = qualification
            it[userSub] = sub
            it[UserQualifications.expiresAt] = expiresAt
        }
    }

    private fun requirements(id: UUID = eventId): List<List<UUID>> = Database { EventEntity[id].qualificationRequirements() }

    private fun eventCount(): Long = Database { EventEntity.count() }

    private suspend fun HttpResponse.missing(): List<List<UUID>> {
        val error = json.decodeFromString(Error.serializer(), bodyAsText())
        assertIs<Error.MissingQualifications>(error)
        return error.missing.map { group -> group.map { UUID.fromString(it.toString()) } }
    }

    private fun sorted(groups: List<List<UUID>>) = groups.map { it.sortedBy(UUID::toString) }.sortedBy { it.first().toString() }

    // ---- Enforcement: POST /events/{id}/confirm ----

    @Test
    fun test_confirm_noRequirements_isUnaffected() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = { seed(); seedEvent() },
    ) {
        client.post("/events/$eventId/confirm").assertStatusCode(HttpStatusCode.NoContent)
    }

    @Test
    fun test_confirm_missingSingleRequirement_isRejected() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = { seed(); seedEvent(listOf(basic)) },
    ) {
        client.post("/events/$eventId/confirm").apply {
            assertError(Error.MissingQualifications())
            assertEquals(listOf(listOf(basic)), missing())
        }
        // Rejected, so the user is not signed up
        assertEquals(0, Database { EventMembers.selectAll().count() })
    }

    @Test
    fun test_confirm_holdingTheRequirement_succeeds() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = { seed(); seedEvent(listOf(basic)); grant(basic) },
    ) {
        client.post("/events/$eventId/confirm").assertStatusCode(HttpStatusCode.NoContent)
        assertEquals(1, Database { EventMembers.selectAll().count() })
    }

    @Test
    fun test_confirm_separateGroups_areAnd() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = { seed(); seedEvent(listOf(basic), listOf(lead)); grant(basic) },
    ) {
        client.post("/events/$eventId/confirm").apply {
            assertError(Error.MissingQualifications())
            assertEquals(listOf(listOf(lead)), missing())
        }
    }

    @Test
    fun test_confirm_groupAlternatives_areOr() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        // basic AND (lead OR topRope), holding basic and only the second alternative
        databaseInitBlock = { seed(); seedEvent(listOf(basic), listOf(lead, topRope)); grant(basic); grant(topRope) },
    ) {
        client.post("/events/$eventId/confirm").assertStatusCode(HttpStatusCode.NoContent)
    }

    @Test
    fun test_confirm_alternativesAlone_doNotMakeUpForMissingRequiredGroup() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = { seed(); seedEvent(listOf(basic), listOf(lead, topRope)); grant(lead); grant(topRope) },
    ) {
        client.post("/events/$eventId/confirm").apply {
            assertError(Error.MissingQualifications())
            assertEquals(listOf(listOf(basic)), missing())
        }
    }

    @Test
    fun test_confirm_reportsEveryUnmetGroup() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = { seed(); seedEvent(listOf(basic), listOf(lead, topRope)) },
    ) {
        client.post("/events/$eventId/confirm").apply {
            assertError(Error.MissingQualifications())
            assertEquals(sorted(listOf(listOf(basic), listOf(lead, topRope))), sorted(missing()))
        }
    }

    @Test
    fun test_confirm_expiredGrant_doesNotCount() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = { seed(); seedEvent(listOf(basic)); grant(basic, expiresAt = Instant.now().minusSeconds(3600)) },
    ) {
        client.post("/events/$eventId/confirm").assertError(Error.MissingQualifications())
    }

    @Test
    fun test_confirm_grantExpiringInTheFuture_counts() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = { seed(); seedEvent(listOf(basic)); grant(basic, expiresAt = Instant.now().plusSeconds(3600)) },
    ) {
        client.post("/events/$eventId/confirm").assertStatusCode(HttpStatusCode.NoContent)
    }

    @Test
    fun test_confirm_anotherUsersGrant_doesNotCount() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = { seed(); seedEvent(listOf(basic)); grant(basic, sub = FakeUser2.SUB) },
    ) {
        client.post("/events/$eventId/confirm").assertError(Error.MissingQualifications())
    }

    @Test
    fun test_confirm_globalAdmin_isNotExemptFromRequirements() = runApplicationTest(
        shouldLogIn = LoginType.ADMIN,
        databaseInitBlock = { seed(); seedEvent(listOf(basic)); FakeAdminUser.provideEntity() },
    ) {
        client.post("/events/$eventId/confirm").assertError(Error.MissingQualifications())
    }

    // ---- Exposing: GET /events ----

    @Test
    fun test_get_event_exposesRequirements() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = { seed(); seedEvent(listOf(basic), listOf(lead, topRope)) },
    ) {
        client.get("/events/$eventId").apply {
            assertStatusCode(HttpStatusCode.OK)
            val event = json.decodeFromString(Event.serializer(), bodyAsText())
            assertEquals(
                sorted(listOf(listOf(basic), listOf(lead, topRope))),
                sorted(event.qualificationRequirements.map { group -> group.map { UUID.fromString(it.toString()) } }),
            )
        }
    }

    @Test
    fun test_get_event_withoutRequirements_exposesEmptyList() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = { seed(); seedEvent() },
    ) {
        client.get("/events/$eventId").apply {
            val event = json.decodeFromString(Event.serializer(), bodyAsText())
            assertEquals(emptyList(), event.qualificationRequirements)
        }
    }

    // ---- Creating: POST /events ----

    private suspend fun io.ktor.client.HttpClient.createEvent(
        department: UUID?,
        qualificationRequirements: String?,
    ): HttpResponse = submitFormWithBinaryData(
        "/events",
        formData {
            append("start", Instant.now().plusSeconds(3600).toEpochMilli())
            append("title", "New event")
            append("place", "Somewhere")
            department?.let { append("department", it.toString()) }
            qualificationRequirements?.let { append("qualificationRequirements", it) }
        },
    )

    @Test
    fun test_create_withRequirements_storesThem() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = { seed(callerRoles = listOf(DepartmentRole.CONTENT_MANAGER)) },
    ) {
        val response = client.createEvent(departmentId, """[["$basic"],["$lead","$topRope"]]""")
        assertEquals(HttpStatusCode.Created, response.status)

        val id = response.headers[HttpHeaders.Location]!!.substringAfterLast('/').toUUID()
        assertEquals(sorted(listOf(listOf(basic), listOf(lead, topRope))), sorted(requirements(id)))
    }

    @Test
    fun test_create_emptyRequirements_isFine() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = { seed(callerRoles = listOf(DepartmentRole.CONTENT_MANAGER)) },
    ) {
        assertEquals(HttpStatusCode.Created, client.createEvent(departmentId, "[]").status)
    }

    @Test
    fun test_create_noRequirementsField_isFine() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = { seed(callerRoles = listOf(DepartmentRole.CONTENT_MANAGER)) },
    ) {
        assertEquals(HttpStatusCode.Created, client.createEvent(departmentId, null).status)
    }

    @Test
    fun test_create_duplicates_areNormalized() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = { seed(callerRoles = listOf(DepartmentRole.CONTENT_MANAGER)) },
    ) {
        val response = client.createEvent(departmentId, """[["$basic","$basic"],["$basic"]]""")
        assertEquals(HttpStatusCode.Created, response.status)
        val id = response.headers[HttpHeaders.Location]!!.substringAfterLast('/').toUUID()
        assertEquals(listOf(listOf(basic)), requirements(id))
    }

    @Test
    fun test_create_requirementFromAnotherDepartment_isRejected_andNothingIsCreated() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = { seed(callerRoles = listOf(DepartmentRole.CONTENT_MANAGER)) },
    ) {
        client.createEvent(departmentId, """[["$ice"]]""").assertStatusCode(HttpStatusCode.BadRequest)
        assertEquals(0, eventCount())
    }

    @Test
    fun test_create_requirementsWithoutDepartment_isRejected() = runApplicationTest(
        shouldLogIn = LoginType.ADMIN,
        databaseInitBlock = { seed() },
    ) {
        client.createEvent(department = null, qualificationRequirements = """[["$basic"]]""")
            .assertStatusCode(HttpStatusCode.BadRequest)
        assertEquals(0, eventCount())
    }

    @Test
    fun test_create_unknownQualification_isRejected() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = { seed(callerRoles = listOf(DepartmentRole.CONTENT_MANAGER)) },
    ) {
        client.createEvent(departmentId, """[["${UUID.randomUUID()}"]]""").assertStatusCode(HttpStatusCode.BadRequest)
        assertEquals(0, eventCount())
    }

    @Test
    fun test_create_emptyGroup_isRejected() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = { seed(callerRoles = listOf(DepartmentRole.CONTENT_MANAGER)) },
    ) {
        client.createEvent(departmentId, "[[]]").assertStatusCode(HttpStatusCode.BadRequest)
        assertEquals(0, eventCount())
    }

    @Test
    fun test_create_malformedRequirements_areRejected() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = { seed(callerRoles = listOf(DepartmentRole.CONTENT_MANAGER)) },
    ) {
        client.createEvent(departmentId, "not json").assertStatusCode(HttpStatusCode.BadRequest)
        client.createEvent(departmentId, """[["not-a-uuid"]]""").assertStatusCode(HttpStatusCode.BadRequest)
        client.createEvent(departmentId, """["$basic"]""").assertStatusCode(HttpStatusCode.BadRequest)
        assertEquals(0, eventCount())
    }

    @Test
    fun test_create_asNonManager_isStillRejected_andRolledBack() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        // a plain member, without CONTENT_MANAGER
        databaseInitBlock = { seed() },
    ) {
        client.createEvent(departmentId, """[["$basic"]]""").assertError(Error.PermissionRejected())
        assertEquals(0, eventCount())
        assertEquals(0, Database { EventQualificationRequirements.selectAll().count() })
    }

    // ---- Updating: PATCH /events/{id} ----

    private suspend fun io.ktor.client.HttpClient.patchEvent(request: UpdateEventRequest): HttpResponse =
        patch("/events/$eventId") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(UpdateEventRequest.serializer(), request))
        }

    private fun UUID.k() = toKotlinUuid()

    @Test
    fun test_patch_setsRequirements() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = { seed(callerRoles = listOf(DepartmentRole.CONTENT_MANAGER)); seedEvent() },
    ) {
        client.patchEvent(UpdateEventRequest(qualificationRequirements = listOf(listOf(basic.k()), listOf(lead.k(), topRope.k())))).assertSuccess()
        assertEquals(sorted(listOf(listOf(basic), listOf(lead, topRope))), sorted(requirements()))
    }

    @Test
    fun test_patch_replacesRequirements() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = { seed(callerRoles = listOf(DepartmentRole.CONTENT_MANAGER)); seedEvent(listOf(basic), listOf(lead)) },
    ) {
        client.patchEvent(UpdateEventRequest(qualificationRequirements = listOf(listOf(topRope.k())))).assertSuccess()
        assertEquals(listOf(listOf(topRope)), requirements())
    }

    @Test
    fun test_patch_emptyList_clearsRequirements() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = { seed(callerRoles = listOf(DepartmentRole.CONTENT_MANAGER)); seedEvent(listOf(basic)) },
    ) {
        client.patchEvent(UpdateEventRequest(qualificationRequirements = emptyList())).assertSuccess()
        assertEquals(emptyList(), requirements())
    }

    @Test
    fun test_patch_otherFields_leaveRequirementsUntouched() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = { seed(callerRoles = listOf(DepartmentRole.CONTENT_MANAGER)); seedEvent(listOf(basic)) },
    ) {
        client.patchEvent(UpdateEventRequest(title = "Renamed")).assertSuccess()
        assertEquals(listOf(listOf(basic)), requirements())
    }

    @Test
    fun test_patch_foreignQualification_isRejected_andRolledBack() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = { seed(callerRoles = listOf(DepartmentRole.CONTENT_MANAGER)); seedEvent(listOf(basic)) },
    ) {
        client.patchEvent(UpdateEventRequest(title = "Renamed", qualificationRequirements = listOf(listOf(ice.k()))))
            .assertError(Error.InvalidArgument("qualificationRequirements"))
        // Nothing of the patch was applied: neither the requirements nor the other fields
        assertEquals(listOf(listOf(basic)), requirements())
        assertEquals("Climbing day", Database { EventEntity[eventId].title })
    }

    @Test
    fun test_patch_movingDepartment_withoutReplacingRequirements_isRejected() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = {
            seed(
                callerRoles = listOf(DepartmentRole.CONTENT_MANAGER),
                callerRolesInOther = listOf(DepartmentRole.CONTENT_MANAGER),
            )
            seedEvent(listOf(basic))
        },
    ) {
        client.patchEvent(UpdateEventRequest(department = otherDepartmentId.k()))
            .assertError(Error.InvalidArgument("qualificationRequirements"))
        assertEquals(departmentId, Database { EventEntity[eventId].department?.id?.value })
        assertEquals(listOf(listOf(basic)), requirements())
    }

    @Test
    fun test_patch_movingDepartment_withNewRequirements_succeeds() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = {
            seed(
                callerRoles = listOf(DepartmentRole.CONTENT_MANAGER),
                callerRolesInOther = listOf(DepartmentRole.CONTENT_MANAGER),
            )
            seedEvent(listOf(basic))
        },
    ) {
        client.patchEvent(UpdateEventRequest(department = otherDepartmentId.k(), qualificationRequirements = listOf(listOf(ice.k())))).assertSuccess()
        assertEquals(otherDepartmentId, Database { EventEntity[eventId].department?.id?.value })
        assertEquals(listOf(listOf(ice)), requirements())
    }

    @Test
    fun test_patch_movingDepartment_clearingRequirements_succeeds() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = {
            seed(
                callerRoles = listOf(DepartmentRole.CONTENT_MANAGER),
                callerRolesInOther = listOf(DepartmentRole.CONTENT_MANAGER),
            )
            seedEvent(listOf(basic))
        },
    ) {
        client.patchEvent(UpdateEventRequest(department = otherDepartmentId.k(), qualificationRequirements = emptyList())).assertSuccess()
        assertEquals(emptyList(), requirements())
    }

    @Test
    fun test_patch_movingDepartment_withoutRequirements_isFine() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = {
            seed(
                callerRoles = listOf(DepartmentRole.CONTENT_MANAGER),
                callerRolesInOther = listOf(DepartmentRole.CONTENT_MANAGER),
            )
            seedEvent()
        },
    ) {
        client.patchEvent(UpdateEventRequest(department = otherDepartmentId.k())).assertSuccess()
        assertEquals(otherDepartmentId, Database { EventEntity[eventId].department?.id?.value })
    }

    @Test
    fun test_patch_requirementsOnly_isNotAnEmptyUpdate() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = { seed(callerRoles = listOf(DepartmentRole.CONTENT_MANAGER)); seedEvent() },
    ) {
        val response = client.patchEvent(UpdateEventRequest(qualificationRequirements = emptyList()))
        response.assertSuccess()
    }

    @Test
    fun test_patch_asNonManager_cannotChangeRequirements() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = { seed(); seedEvent(listOf(basic)) },
    ) {
        client.patchEvent(UpdateEventRequest(qualificationRequirements = emptyList())).assertError(Error.PermissionRejected())
        assertEquals(listOf(listOf(basic)), requirements())
    }

    // ---- Deleting ----

    @Test
    fun test_deleteQualification_requiredByAnEvent_isRejected() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = { seed(callerRoles = listOf(DepartmentRole.QUALIFICATIONS_MANAGER)); seedEvent(listOf(basic, lead)) },
    ) {
        client.delete("/qualifications/$basic").assertError(Error.EntityDeleteReferencesExist())
        assertTrue(Database { QualificationEntity.findById(basic) != null })
        // Requirements are untouched
        assertEquals(listOf(listOf(basic, lead).sortedBy(UUID::toString)), requirements())
    }

    @Test
    fun test_deleteQualification_notRequiredAnymore_succeeds() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = {
            seed(callerRoles = listOf(DepartmentRole.QUALIFICATIONS_MANAGER))
            seedEvent(listOf(basic)).setQualificationRequirements(emptyList())
        },
    ) {
        client.delete("/qualifications/$basic").assertStatusCode(HttpStatusCode.NoContent)
        assertNull(Database { QualificationEntity.findById(basic) })
    }

    @Test
    fun test_deleteEvent_removesItsRequirements_soTheQualificationCanBeDeleted() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = {
            seed(callerRoles = listOf(DepartmentRole.QUALIFICATIONS_MANAGER, DepartmentRole.CONTENT_MANAGER))
            seedEvent(listOf(basic))
        },
    ) {
        client.delete("/events/$eventId").assertStatusCode(HttpStatusCode.NoContent)
        assertEquals(0, Database { EventQualificationRequirements.selectAll().where { EventQualificationRequirements.qualification eq basic }.count() })
        client.delete("/qualifications/$basic").assertStatusCode(HttpStatusCode.NoContent)
    }
}
