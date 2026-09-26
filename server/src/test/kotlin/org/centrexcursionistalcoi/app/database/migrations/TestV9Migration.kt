package org.centrexcursionistalcoi.app.database.migrations

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.centrexcursionistalcoi.app.assertTrue
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.PostgresTestBase
import org.centrexcursionistalcoi.app.database.entity.EventEntity
import org.centrexcursionistalcoi.app.database.table.EventMembers
import org.centrexcursionistalcoi.app.database.table.Events
import org.centrexcursionistalcoi.app.security.AES
import org.centrexcursionistalcoi.app.test.FakeUser
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/**
 * Tests the migration that makes `event_members.event_id` cascade on delete: once migrated, deleting an event with a
 * confirmed attendee removes the attendee row instead of being rejected by the foreign key, and the attendee's user
 * is kept.
 */
class TestV9Migration : PostgresTestBase() {
    @Test
    fun test() {
        AES.initForTests()

        // Create the current schema (already cascades on event_members.event_id).
        Database.init()

        Database { transaction { FakeUser.provideEntity() } }
        val event = Database {
            EventEntity.new {
                start = Instant.now().plusSeconds(3600)
                title = "Event with an attendee"
                place = "Somewhere"
            }
        }
        Database {
            EventMembers.insert {
                it[this.event] = event.id
                it[this.userReference] = FakeUser.SUB
            }
        }

        // Simulate a pre-V9 database: the event reference restricts deletes.
        Database.exec(
            """
                ALTER TABLE event_members DROP CONSTRAINT fk_event_members_event_id__id;
                ALTER TABLE event_members ADD CONSTRAINT fk_event_members_event_id__id
                    FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE RESTRICT ON UPDATE RESTRICT;
            """.trimIndent()
        ).assertTrue()

        // Migrate
        Database { V9.migrate() }

        Database.execQuery(
            """
                SELECT confdeltype, confupdtype FROM pg_constraint
                WHERE conname = 'fk_event_members_event_id__id'
            """.trimIndent()
        ).let { rs ->
            assertTrue(rs.next())
            assertEquals("c", rs.getString("confdeltype"))
            assertEquals("r", rs.getString("confupdtype"))
        }

        // Deleting the event now takes its attendee row with it.
        Database { Events.deleteWhere { Events.id eq event.id } }

        Database.execQuery("""SELECT 1 FROM event_members WHERE event_id = '${event.id.value}'""").let { rs ->
            assertFalse(rs.next())
        }
        Database.execQuery("""SELECT 1 FROM user_references WHERE sub = '${FakeUser.SUB}'""").let { rs ->
            assertTrue(rs.next())
        }
    }
}
