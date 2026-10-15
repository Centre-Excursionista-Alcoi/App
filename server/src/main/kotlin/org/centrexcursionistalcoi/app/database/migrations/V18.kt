package org.centrexcursionistalcoi.app.database.migrations

import org.jetbrains.exposed.v1.jdbc.JdbcTransaction

/**
 * Migration V18: the specs, location and photos of spaces.
 * - `spaces.seats`, `spaces.beds`, `spaces.address`, `spaces.latitude`, `spaces.longitude`, `spaces.altitude`: `NULL` if unknown.
 * - `spaces."energySources"`, `spaces."climateControl"`: JSON lists, empty by default.
 * - `space_images`: the photos of each space.
 * - Idempotent, like V12 to V17.
 */
object V18 : DatabaseMigration {
    override val from: Int = 17
    override val to: Int = 18

    context(tr: JdbcTransaction)
    override fun migrate() {
        tr.exec("""ALTER TABLE spaces ADD COLUMN IF NOT EXISTS seats integer NULL""")
        tr.exec("""ALTER TABLE spaces ADD COLUMN IF NOT EXISTS beds integer NULL""")
        tr.exec("""ALTER TABLE spaces ADD COLUMN IF NOT EXISTS address text NULL""")
        tr.exec("""ALTER TABLE spaces ADD COLUMN IF NOT EXISTS latitude double precision NULL""")
        tr.exec("""ALTER TABLE spaces ADD COLUMN IF NOT EXISTS longitude double precision NULL""")
        tr.exec("""ALTER TABLE spaces ADD COLUMN IF NOT EXISTS altitude double precision NULL""")
        tr.exec("""ALTER TABLE spaces ADD COLUMN IF NOT EXISTS "energySources" jsonb NOT NULL DEFAULT '[]'::jsonb""")
        tr.exec("""ALTER TABLE spaces ADD COLUMN IF NOT EXISTS "climateControl" jsonb NOT NULL DEFAULT '[]'::jsonb""")
        tr.exec(
            """
            CREATE TABLE IF NOT EXISTS space_images (
                space uuid NOT NULL REFERENCES spaces (id) ON DELETE CASCADE,
                file uuid NOT NULL REFERENCES files (id) ON DELETE RESTRICT,
                CONSTRAINT "PK_SpaceImages_space_file" PRIMARY KEY (space, file)
            )
            """.trimIndent()
        )
    }
}
