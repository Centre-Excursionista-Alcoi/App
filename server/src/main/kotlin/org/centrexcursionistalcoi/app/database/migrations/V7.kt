package org.centrexcursionistalcoi.app.database.migrations

import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import java.time.ZoneId

/**
 * Migration V7:
 * - Memories now have a required `from`/`to` date range, describing when the activity they document took place,
 *   each carrying the IANA time zone it was recorded in. Stored as `fromInstant`/`fromZone` and `toInstant`/`toZone`
 *   (a plain instant plus the zone id), combined into a single `ZonedDateTime` when read back.
 * - For memories linked to a lending, the range is backfilled from that lending's own `from`/`to` (start/end of
 *   day, in the server's local time zone -- matching how new lending memories fill it in automatically).
 * - For standalone memories (no lending), there's no better source of truth, so the range is backfilled from the
 *   memory's own `createdAt`.
 * - Both legacy-data zones are backfilled with the server's own local time zone, since that's what every other
 *   date on these old rows was already implicitly recorded in.
 * - The columns are added nullable, backfilled, then set to NOT NULL, matching the `from_is_before_to` invariant
 *   already enforced (at the application level) for new memories.
 */
object V7 : DatabaseMigration {
    override val from: Int = 6
    override val to: Int = 7

    context(tr: JdbcTransaction)
    override fun migrate() {
        tr.exec(
            """
                ALTER TABLE memories ADD COLUMN "fromInstant" TIMESTAMP;
                ALTER TABLE memories ADD COLUMN "fromZone" VARCHAR(64);
                ALTER TABLE memories ADD COLUMN "toInstant" TIMESTAMP;
                ALTER TABLE memories ADD COLUMN "toZone" VARCHAR(64);
            """.trimIndent()
        )

        val zoneId = ZoneId.systemDefault().id

        // Backfill memories linked to a lending from that lending's own date range.
        tr.exec(
            """
                UPDATE memories m
                SET "fromInstant" = l."from"::timestamp,
                    "fromZone" = '$zoneId',
                    "toInstant" = l."to"::timestamp + interval '1 day' - interval '1 second',
                    "toZone" = '$zoneId'
                FROM lendings l
                WHERE m.lending = l.id;
            """.trimIndent()
        )

        // Backfill standalone memories from their own creation time.
        tr.exec(
            """
                UPDATE memories
                SET "fromInstant" = "createdAt",
                    "fromZone" = '$zoneId',
                    "toInstant" = "createdAt",
                    "toZone" = '$zoneId'
                WHERE lending IS NULL;
            """.trimIndent()
        )

        tr.exec(
            """
                ALTER TABLE memories
                ALTER COLUMN "fromInstant" SET NOT NULL,
                ALTER COLUMN "fromZone" SET NOT NULL,
                ALTER COLUMN "toInstant" SET NOT NULL,
                ALTER COLUMN "toZone" SET NOT NULL,
                ADD CONSTRAINT memories_from_is_before_to CHECK ("fromInstant" <= "toInstant");
            """.trimIndent()
        )
    }
}
