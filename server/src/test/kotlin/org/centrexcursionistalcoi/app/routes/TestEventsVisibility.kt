package org.centrexcursionistalcoi.app.routes

import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import java.time.Duration
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import org.centrexcursionistalcoi.app.ApplicationTestBase
import org.centrexcursionistalcoi.app.assertBody
import org.centrexcursionistalcoi.app.assertStatusCode
import org.centrexcursionistalcoi.app.data.Event
import org.centrexcursionistalcoi.app.database.entity.DepartmentEntity
import org.centrexcursionistalcoi.app.database.entity.DepartmentMemberEntity
import org.centrexcursionistalcoi.app.database.entity.EventEntity
import org.centrexcursionistalcoi.app.serialization.list
import org.centrexcursionistalcoi.app.test.FakeUser
import org.centrexcursionistalcoi.app.test.LoginType
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction

/**
 * Which events a member is sent: the ones that aren't over yet, i.e. still to come or in progress. An event without
 * an end date is taken to last a day from its start, which covers the rest of the day it starts on in any time
 * zone (the app trims the list to the exact end of the day in the device's own).
 *
 * The list and the single-item route apply the same rule, so both are checked against the same events.
 */
class TestEventsVisibility : ApplicationTestBase() {
    private val now: Instant = Instant.parse("2026-10-05T12:00:00Z")

    private fun hours(h: Long) = Duration.ofHours(h)

    /** Titles of the events a member of "Members' Department" is expected to see, and to be denied. */
    private val expectedVisible = setOf(
        "to come, no end",
        "later today, no end",
        "started this morning, no end",
        "started 23 hours ago, no end",
        "multi-day, in progress",
        "ends right now",
        "public, started 2 hours ago, no end",
    )
    private val expectedHidden = setOf(
        "started 25 hours ago, no end",
        "ended an hour ago",
        "other department, in progress",
    )

    /** One event of each kind, all in "Members' Department" unless the title says otherwise. */
    private fun JdbcTransaction.seed() {
        val mine = DepartmentEntity.new { displayName = "Members' Department" }
        val other = DepartmentEntity.new { displayName = "Other Department" }
        DepartmentMemberEntity.new {
            userReference = FakeUser.provideEntity()
            department = mine
            confirmed = true
        }

        fun event(title: String, start: Instant, end: Instant? = null, department: DepartmentEntity? = mine) = EventEntity.new {
            this.title = title
            place = "Somewhere"
            this.start = start
            this.end = end
            this.department = department
        }

        event("to come, no end", start = now + Duration.ofDays(2))
        event("later today, no end", start = now + hours(3))
        event("started this morning, no end", start = now - hours(4))
        event("started 23 hours ago, no end", start = now - hours(23))
        event("started 25 hours ago, no end", start = now - hours(25))
        event("multi-day, in progress", start = now - Duration.ofDays(2), end = now + Duration.ofDays(2))
        event("ended an hour ago", start = now - hours(5), end = now - hours(1))
        event("ends right now", start = now - hours(5), end = now)
        event("other department, in progress", start = now - hours(2), department = other)
        event("public, started 2 hours ago, no end", start = now - hours(2), department = null)
    }

    @Test
    fun test_list_member_seesEventsNotOver_includingOnesInProgress() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        mockNow = now,
        databaseInitBlock = { seed() },
    ) {
        client.get("/events").apply {
            assertStatusCode(HttpStatusCode.OK)
            assertBody(Event.serializer().list()) { events ->
                assertEquals(expectedVisible, events.map { it.title }.toSet())
            }
        }
    }

    @Test
    fun test_single_member_agreesWithTheList() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        mockNow = now,
        databaseInitBlock = { seed(); EventEntity.all().associate { it.title to it.id.value } },
    ) { context ->
        val ids = context.dibResult!!
        for (title in expectedVisible) {
            client.get("/events/${ids.getValue(title)}").assertStatusCode(HttpStatusCode.OK)
        }
        for (title in expectedHidden) {
            // a denied event is answered exactly as if it didn't exist
            client.get("/events/${ids.getValue(title)}").assertStatusCode(HttpStatusCode.NotFound)
        }
    }

    @Test
    fun test_list_admin_stillSeesEveryEvent() = runApplicationTest(
        shouldLogIn = LoginType.ADMIN,
        mockNow = now,
        databaseInitBlock = { seed() },
    ) {
        client.get("/events").apply {
            assertBody(Event.serializer().list()) { events ->
                assertEquals(expectedVisible.size + expectedHidden.size, events.size)
            }
        }
    }
}
