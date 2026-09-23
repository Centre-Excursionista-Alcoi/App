package org.centrexcursionistalcoi.app.database

import androidx.room3.Room
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Builds real on-disk databases at each historical schema version (SQL taken verbatim from the checked-in
 * `composeApp/schemas/.../AppDatabase/<version>.json` exports) and opens them with the actual production
 * builder ([getRoomDatabase]), to verify [MIGRATION_2_3]/[MIGRATION_3_4] really do apply and preserve data --
 * and that the one deliberately uncovered jump (v1 -> v2, see [MIGRATION_2_3]'s file-level KDoc in
 * `DatabaseMigrations.kt`) still falls back to a clean destructive wipe rather than crashing.
 */
class TestDatabaseMigrations {
    private fun SQLiteConnection.tableNames(): Set<String> {
        val statement = prepare("SELECT name FROM sqlite_master WHERE type = 'table'")
        val names = mutableSetOf<String>()
        try {
            while (statement.step()) names += statement.getText(0)
        } finally {
            statement.close()
        }
        return names
    }

    private fun SQLiteConnection.columnNames(table: String): Set<String> {
        val statement = prepare("PRAGMA table_info($table)")
        val names = mutableSetOf<String>()
        try {
            while (statement.step()) names += statement.getText(1)
        } finally {
            statement.close()
        }
        return names
    }

    private fun SQLiteConnection.userVersion(): Long {
        val statement = prepare("PRAGMA user_version")
        try {
            statement.step()
            return statement.getLong(0)
        } finally {
            statement.close()
        }
    }

    private fun SQLiteConnection.textColumn(table: String, column: String, whereIdEquals: String): String? {
        val statement = prepare("SELECT `$column` FROM `$table` WHERE `id` = '$whereIdEquals'")
        try {
            if (!statement.step()) return null
            return if (statement.isNull(0)) null else statement.getText(0)
        } finally {
            statement.close()
        }
    }

    /** Every `CREATE TABLE`/`CREATE INDEX` from `AppDatabase/1.json` and `2.json` (schema-identical). */
    private val v1v2SchemaSql = listOf(
        "CREATE TABLE IF NOT EXISTS `Departments` (`id` TEXT NOT NULL, `displayName` TEXT NOT NULL, `imageFile` TEXT, `members` TEXT, PRIMARY KEY(`id`))",
        "CREATE INDEX IF NOT EXISTS `idx_Departments_displayName` ON `Departments` (`displayName`)",
        "CREATE TABLE IF NOT EXISTS `Events` (`id` TEXT NOT NULL, `start` INTEGER NOT NULL, `end` INTEGER, `place` TEXT NOT NULL, `title` TEXT NOT NULL, `description` TEXT, `maxPeople` INTEGER, `requiresConfirmation` INTEGER NOT NULL, `requiresInsurance` INTEGER NOT NULL, `department` TEXT, `image` TEXT, PRIMARY KEY(`id`))",
        "CREATE TABLE IF NOT EXISTS `EventUsers` (`eventId` TEXT NOT NULL, `userSub` TEXT NOT NULL, PRIMARY KEY(`eventId`, `userSub`), FOREIGN KEY(`eventId`) REFERENCES `Events`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        "CREATE INDEX IF NOT EXISTS `index_EventUsers_userSub` ON `EventUsers` (`userSub`)",
        "CREATE TABLE IF NOT EXISTS `InventoryItemTypes` (`id` TEXT NOT NULL, `displayName` TEXT NOT NULL, `description` TEXT, `categories` TEXT, `department` TEXT, `image` TEXT, PRIMARY KEY(`id`), FOREIGN KEY(`department`) REFERENCES `Departments`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION )",
        "CREATE INDEX IF NOT EXISTS `index_InventoryItemTypes_department` ON `InventoryItemTypes` (`department`)",
        "CREATE TABLE IF NOT EXISTS `InventoryItems` (`id` TEXT NOT NULL, `variation` TEXT, `type` TEXT NOT NULL, `nfcId` BLOB, `manufacturerTraceabilityCode` TEXT, PRIMARY KEY(`id`), FOREIGN KEY(`type`) REFERENCES `InventoryItemTypes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        "CREATE INDEX IF NOT EXISTS `index_InventoryItems_type` ON `InventoryItems` (`type`)",
        "CREATE TABLE IF NOT EXISTS `Lendings` (`id` TEXT NOT NULL, `userSub` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `fromDate` TEXT NOT NULL, `toDate` TEXT NOT NULL, `confirmed` INTEGER NOT NULL, `taken` INTEGER NOT NULL, `givenBy` TEXT, `givenAt` INTEGER, `returned` INTEGER NOT NULL, `memorySubmitted` INTEGER NOT NULL, `memorySubmittedAt` INTEGER, `memoryReviewed` INTEGER NOT NULL, `notes` TEXT, PRIMARY KEY(`id`))",
        "CREATE TABLE IF NOT EXISTS `LendingItems` (`lendingId` TEXT NOT NULL, `itemId` TEXT NOT NULL, PRIMARY KEY(`lendingId`, `itemId`), FOREIGN KEY(`lendingId`) REFERENCES `Lendings`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`itemId`) REFERENCES `InventoryItems`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        "CREATE INDEX IF NOT EXISTS `index_LendingItems_itemId` ON `LendingItems` (`itemId`)",
        "CREATE TABLE IF NOT EXISTS `Members` (`memberNumber` INTEGER NOT NULL, `status` TEXT, `fullName` TEXT NOT NULL, `nif` TEXT, `email` TEXT, PRIMARY KEY(`memberNumber`))",
        "CREATE TABLE IF NOT EXISTS `Memories` (`id` TEXT NOT NULL, `place` TEXT, `externalUsers` TEXT, `text` TEXT NOT NULL, `sport` TEXT, `department` TEXT, `attachments` TEXT, `submittedBy` TEXT NOT NULL, `fromDate` TEXT NOT NULL, `toDate` TEXT NOT NULL, `pdf` TEXT, `lending` TEXT, PRIMARY KEY(`id`), FOREIGN KEY(`department`) REFERENCES `Departments`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION , FOREIGN KEY(`lending`) REFERENCES `Lendings`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION )",
        "CREATE INDEX IF NOT EXISTS `index_Memories_department` ON `Memories` (`department`)",
        "CREATE INDEX IF NOT EXISTS `index_Memories_lending` ON `Memories` (`lending`)",
        "CREATE TABLE IF NOT EXISTS `MemoryMembers` (`memoryId` TEXT NOT NULL, `memberNumber` INTEGER NOT NULL, PRIMARY KEY(`memoryId`, `memberNumber`), FOREIGN KEY(`memoryId`) REFERENCES `Memories`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        "CREATE INDEX IF NOT EXISTS `index_MemoryMembers_memberNumber` ON `MemoryMembers` (`memberNumber`)",
        "CREATE TABLE IF NOT EXISTS `Posts` (`id` TEXT NOT NULL, `date` INTEGER NOT NULL, `title` TEXT NOT NULL, `content` TEXT NOT NULL, `department` TEXT, `link` TEXT, `files` TEXT, PRIMARY KEY(`id`), FOREIGN KEY(`department`) REFERENCES `Departments`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION )",
        "CREATE INDEX IF NOT EXISTS `index_Posts_department` ON `Posts` (`department`)",
        "CREATE TABLE IF NOT EXISTS `ReceivedItems` (`id` TEXT NOT NULL, `lending` TEXT NOT NULL, `item` TEXT NOT NULL, `notes` TEXT, `receivedBy` TEXT NOT NULL, `receivedAt` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`lending`) REFERENCES `Lendings`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION , FOREIGN KEY(`item`) REFERENCES `InventoryItems`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION , FOREIGN KEY(`receivedBy`) REFERENCES `Users`(`sub`) ON UPDATE NO ACTION ON DELETE NO ACTION )",
        "CREATE INDEX IF NOT EXISTS `index_ReceivedItems_lending` ON `ReceivedItems` (`lending`)",
        "CREATE INDEX IF NOT EXISTS `index_ReceivedItems_item` ON `ReceivedItems` (`item`)",
        "CREATE INDEX IF NOT EXISTS `index_ReceivedItems_receivedBy` ON `ReceivedItems` (`receivedBy`)",
        "CREATE TABLE IF NOT EXISTS `Users` (`sub` TEXT NOT NULL, `memberNumber` INTEGER NOT NULL, `fullName` TEXT NOT NULL, `email` TEXT NOT NULL, `groups` TEXT NOT NULL, `departments` TEXT NOT NULL, `lendingUser` TEXT, `insurances` TEXT NOT NULL, `isDisabled` INTEGER NOT NULL, PRIMARY KEY(`sub`))",
        "CREATE INDEX IF NOT EXISTS `idx_users_isDisabled` ON `Users` (`isDisabled`)",
    )

    /** Same as [v1v2SchemaSql], but with v3's additions: `Qualifications`/`QualificationGrants` and `Events.qualificationRequirements`. */
    private val v3SchemaSql = v1v2SchemaSql.map { sql ->
        if (sql.startsWith("CREATE TABLE IF NOT EXISTS `Events`")) {
            sql.replace("`image` TEXT, PRIMARY KEY", "`image` TEXT, `qualificationRequirements` TEXT NOT NULL, PRIMARY KEY")
        } else {
            sql
        }
    } + listOf(
        "CREATE TABLE IF NOT EXISTS `Qualifications` (`id` TEXT NOT NULL, `departmentId` TEXT NOT NULL, `name` TEXT NOT NULL, `description` TEXT, PRIMARY KEY(`id`))",
        "CREATE TABLE IF NOT EXISTS `QualificationGrants` (`qualificationId` TEXT NOT NULL, `userSub` TEXT NOT NULL, `grantedBy` TEXT, `grantedAt` INTEGER NOT NULL, `expiresAt` INTEGER, PRIMARY KEY(`qualificationId`))",
    )

    /** Builds a real on-disk database matching [schemaSql] at [version], seeded by [seed], and returns its path. */
    private fun buildDatabaseFile(version: Int, schemaSql: List<String>, seed: SQLiteConnection.() -> Unit): String {
        val dbFile = File.createTempFile("cea_app_migration_test_v$version", ".db")
        dbFile.delete()
        dbFile.deleteOnExit()

        BundledSQLiteDriver().open(dbFile.absolutePath).use { connection ->
            connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
            connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'test')")
            for (sql in schemaSql) connection.execSQL(sql)
            connection.seed()
            connection.execSQL("PRAGMA user_version = $version")
        }
        return dbFile.absolutePath
    }

    @Test
    fun migratingFromV1_hasNoExplicitPath_fallsBackToDestructiveWipe() = runTest {
        val path = buildDatabaseFile(version = 1, schemaSql = v1v2SchemaSql) {
            execSQL("INSERT INTO Departments (id, displayName, imageFile, members) VALUES ('11111111-1111-1111-1111-111111111111', 'Old Department', NULL, NULL)")
        }

        val database = getRoomDatabase(Room.databaseBuilder<AppDatabase>(name = path), Dispatchers.IO)
        try {
            // Forces the real open (and migration) path.
            assertEquals(emptyList(), database.departmentDao().selectAll())
        } finally {
            database.close()
        }

        BundledSQLiteDriver().open(path).use { connection ->
            assertEquals(4L, connection.userVersion())
            // The destructive fallback really did wipe the pre-existing row -- proves this went through the
            // fallback path, not some accidental no-op.
            assertNull(connection.textColumn("Departments", "displayName", "11111111-1111-1111-1111-111111111111"))
        }
    }

    @Test
    fun migratingFromV2_preservesExistingRows_andAddsV3AndV4Columns() = runTest {
        val path = buildDatabaseFile(version = 2, schemaSql = v1v2SchemaSql) {
            execSQL("INSERT INTO Departments (id, displayName, imageFile, members) VALUES ('11111111-1111-1111-1111-111111111111', 'Old Department', NULL, NULL)")
            execSQL(
                "INSERT INTO Events (id, start, end, place, title, description, maxPeople, requiresConfirmation, requiresInsurance, department, image) VALUES " +
                    "('22222222-2222-2222-2222-222222222222', 0, NULL, 'Somewhere', 'An event', NULL, NULL, 0, 0, NULL, NULL)"
            )
        }

        val database = getRoomDatabase(Room.databaseBuilder<AppDatabase>(name = path), Dispatchers.IO)
        try {
            assertEquals(1, database.departmentDao().selectAll().size)
        } finally {
            database.close()
        }

        BundledSQLiteDriver().open(path).use { connection ->
            assertEquals(4L, connection.userVersion())

            // MIGRATION_2_3 + MIGRATION_3_4 ran in sequence: no destructive wipe, the original rows survived.
            assertEquals("Old Department", connection.textColumn("Departments", "displayName", "11111111-1111-1111-1111-111111111111"))
            assertEquals("An event", connection.textColumn("Events", "title", "22222222-2222-2222-2222-222222222222"))
            // Backfilled default for the NOT NULL column MIGRATION_2_3 added.
            assertEquals("[]", connection.textColumn("Events", "qualificationRequirements", "22222222-2222-2222-2222-222222222222"))

            val tables = connection.tableNames()
            assertFalse("Qualifications" in tables)
            assertFalse("QualificationGrants" in tables)

            val departmentColumns = connection.columnNames("Departments")
            assertTrue("qualifications" in departmentColumns)
            assertTrue("qualificationGrants" in departmentColumns)
            // Not backfilled by the migration itself -- SyncAllDataBackgroundJob's forced post-upgrade resync
            // is what's expected to populate this from the server.
            assertNull(connection.textColumn("Departments", "qualifications", "11111111-1111-1111-1111-111111111111"))
        }
    }

    @Test
    fun migratingFromV3_preservesDepartmentsAndEvents_dropsQualificationTables() = runTest {
        val path = buildDatabaseFile(version = 3, schemaSql = v3SchemaSql) {
            execSQL("INSERT INTO Departments (id, displayName, imageFile, members) VALUES ('11111111-1111-1111-1111-111111111111', 'Old Department', NULL, NULL)")
            execSQL(
                "INSERT INTO Qualifications (id, departmentId, name, description) VALUES " +
                    "('33333333-3333-3333-3333-333333333333', '11111111-1111-1111-1111-111111111111', 'Lead climbing', NULL)"
            )
        }

        val database = getRoomDatabase(Room.databaseBuilder<AppDatabase>(name = path), Dispatchers.IO)
        try {
            assertEquals(1, database.departmentDao().selectAll().size)
        } finally {
            database.close()
        }

        BundledSQLiteDriver().open(path).use { connection ->
            assertEquals(4L, connection.userVersion())
            assertEquals("Old Department", connection.textColumn("Departments", "displayName", "11111111-1111-1111-1111-111111111111"))
            assertFalse("Qualifications" in connection.tableNames())
        }
    }
}
