package org.centrexcursionistalcoi.app.database

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * Real, data-preserving migrations for the version jumps we can trust the on-disk schema for. Each one is
 * written against, and should be checked against, the exact `CREATE TABLE`/`ALTER TABLE` shape recorded in the
 * matching `composeApp/schemas/.../AppDatabase/<version>.json` export (see `TestDatabaseMigrations.kt`, which
 * builds a real on-disk database from each version's schema export and runs these against it).
 */

/** v3 added the `Qualifications`/`QualificationGrants` tables and `Events.qualificationRequirements`. */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `Qualifications` (`id` TEXT NOT NULL, `departmentId` TEXT NOT NULL, `name` TEXT NOT NULL, `description` TEXT, PRIMARY KEY(`id`))"
        )
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `QualificationGrants` (`qualificationId` TEXT NOT NULL, `userSub` TEXT NOT NULL, `grantedBy` TEXT, `grantedAt` INTEGER NOT NULL, `expiresAt` INTEGER, PRIMARY KEY(`qualificationId`))"
        )
        // NOT NULL with no migration-time data to backfill from: default every existing Event to "no requirements",
        // matching what RoomConverters.uuidGroupsToString(emptyList()) itself encodes.
        connection.execSQL("ALTER TABLE `Events` ADD COLUMN `qualificationRequirements` TEXT NOT NULL DEFAULT '[]'")
    }
}

/**
 * v4 moved qualifications/grants from their own tables into two columns embedded on `Departments` (see
 * `Departments.extraColumns` server-side) -- the existing rows in the dropped tables aren't carried over as
 * data (there's no way to know, from SQL alone, which of a department's already-synced qualifications a given
 * viewer was allowed to see), but every department whose row survives this migration gets those two new columns
 * backfilled for real on the very next sync: [SyncAllDataBackgroundJob] forces a full re-fetch (bypassing
 * `If-Modified-Since`) whenever the local schema version just changed, specifically so this isn't left null
 * indefinitely just because the server saw no reason to bump that department's own `lastUpdate`.
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE `Departments` ADD COLUMN `qualifications` TEXT")
        connection.execSQL("ALTER TABLE `Departments` ADD COLUMN `qualificationGrants` TEXT")
        connection.execSQL("DROP TABLE IF EXISTS `Qualifications`")
        connection.execSQL("DROP TABLE IF EXISTS `QualificationGrants`")
    }
}
