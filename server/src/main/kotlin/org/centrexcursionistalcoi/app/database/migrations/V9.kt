package org.centrexcursionistalcoi.app.database.migrations

import org.jetbrains.exposed.v1.jdbc.JdbcTransaction

/**
 * Migration V9:
 * - `event_members.event_id` now cascades on delete, like every other table that only links something to an event
 *   (`event_qualification_requirements`). It used to restrict, so an event with any confirmed attendee couldn't be
 *   deleted at all.
 */
object V9 : DatabaseMigration {
    override val from: Int = 8
    override val to: Int = 9

    context(tr: JdbcTransaction)
    override fun migrate() {
        tr.exec(
            """
                ALTER TABLE event_members DROP CONSTRAINT fk_event_members_event_id__id;
                ALTER TABLE event_members ADD CONSTRAINT fk_event_members_event_id__id
                    FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE ON UPDATE RESTRICT;
            """.trimIndent()
        )
    }
}
