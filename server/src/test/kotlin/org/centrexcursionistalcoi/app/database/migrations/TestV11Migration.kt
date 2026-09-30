package org.centrexcursionistalcoi.app.database.migrations

import kotlin.time.Clock
import kotlin.uuid.toKotlinUuid
import io.ktor.http.ContentType
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlin.uuid.Uuid
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.centrexcursionistalcoi.app.ResourcesUtils
import org.centrexcursionistalcoi.app.assertTrue
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.PostgresTestBase
import org.centrexcursionistalcoi.app.database.entity.ConfigEntity
import org.centrexcursionistalcoi.app.database.entity.FileEntity
import org.centrexcursionistalcoi.app.database.entity.UserInsuranceEntity
import org.centrexcursionistalcoi.app.database.table.Departments
import org.centrexcursionistalcoi.app.database.table.Events
import org.centrexcursionistalcoi.app.database.table.Files
import org.centrexcursionistalcoi.app.database.table.InventoryItemTypes
import org.centrexcursionistalcoi.app.database.table.Memories
import org.centrexcursionistalcoi.app.database.table.MemoriesFiles
import org.centrexcursionistalcoi.app.database.table.PostFiles
import org.centrexcursionistalcoi.app.database.table.Posts
import org.centrexcursionistalcoi.app.security.AES
import org.centrexcursionistalcoi.app.storage.FailingFileStorage
import org.centrexcursionistalcoi.app.storage.FileStorage
import org.centrexcursionistalcoi.app.storage.FileStorageProvider
import org.centrexcursionistalcoi.app.storage.InMemoryFileStorage
import org.centrexcursionistalcoi.app.storage.testStorage
import org.centrexcursionistalcoi.app.test.FakeUser
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.vendors.currentDialectMetadata

/**
 * Tests the migration that moves the contents of files from the database to the file storage.
 */
class TestV11Migration : PostgresTestBase() {
    private val png = ResourcesUtils.bytesFromResource("/square.png")
    private val pdf = ResourcesUtils.bytesFromResource("/document.pdf")

    /** The contents of each file in the pre-V11 database. */
    private val contents = mutableMapOf<Uuid, ByteArray>()

    /** Files referenced by something, which must be moved. */
    private val referenced = mutableListOf<Uuid>()

    /** Files referenced by nothing, which must be deleted. */
    private val orphans = mutableListOf<Uuid>()

    /** A file without a type, whose type must be detected. */
    private lateinit var untyped: Uuid

    @BeforeTest
    fun initAES() {
        AES.initForTests()
    }

    private fun newFile(bytes: ByteArray): Uuid = Database {
        FileEntity.create(bytes, "file", ContentType.Application.OctetStream).id.value
    }.also { contents[it] = bytes }

    /**
     * Creates a database as it was before V11: one file referenced by each of the columns referencing files, one
     * without type, and two referenced by nothing.
     */
    private fun createPreV11Database() {
        Database.init()

        val user = Database { transaction { FakeUser.provideEntity() } }

        val departmentImage = newFile(png)
        val department = Database {
            Departments.insertAndGetId {
                it[displayName] = "Department"
                it[image] = departmentImage
            }
        }
        val typeImage = newFile(png)
        Database {
            InventoryItemTypes.insert {
                it[displayName] = "Type"
                it[InventoryItemTypes.department] = department
                it[image] = typeImage
            }
        }
        val eventImage = newFile(png)
        Database {
            Events.insert {
                it[start] = Clock.System.now()
                it[place] = "Place"
                it[title] = "Event"
                it[image] = eventImage
            }
        }
        val memoryPdf = newFile(pdf)
        val memoryAttachment = newFile(png)
        Database {
            val memory = Memories.insertAndGetId {
                it[text] = "Memory"
                it[submittedBy] = user.id
                it[fromInstant] = Clock.System.now()
                it[fromZone] = "Europe/Madrid"
                it[toInstant] = Clock.System.now()
                it[toZone] = "Europe/Madrid"
                it[Memories.pdf] = memoryPdf
            }
            MemoriesFiles.insert {
                it[MemoriesFiles.memory] = memory
                it[file] = memoryAttachment
            }
        }
        val postFile = newFile(pdf)
        Database {
            val post = Posts.insertAndGetId {
                it[title] = "Post"
                it[content] = "Content"
            }
            PostFiles.insert {
                it[PostFiles.post] = post
                it[file] = postFile
            }
        }
        val insuranceDocument = newFile(pdf)
        untyped = newFile(png)
        Database {
            UserInsuranceEntity.new {
                userSub = user
                insuranceCompany = "Company"
                policyNumber = "1234"
                validFrom = LocalDate(2025, 1, 1)
                validTo = LocalDate(2025, 12, 31)
            }.addDocuments(listOf(FileEntity[insuranceDocument], FileEntity[untyped]))
        }
        referenced += listOf(departmentImage, typeImage, eventImage, memoryPdf, memoryAttachment, postFile, insuranceDocument, untyped)
        orphans += listOf(newFile(byteArrayOf(1, 2, 3)), newFile(byteArrayOf(4, 5, 6)))

        // Back to the pre-V11 shape of files: contents in "bytes", no "objectKey" nor "size"
        Database.exec("""ALTER TABLE files ADD COLUMN bytes bytea NOT NULL DEFAULT ''::bytea""").assertTrue()
        for ((id, bytes) in contents) {
            Database.exec("UPDATE files SET bytes = ? WHERE id = ?", arrayOf(bytes, id))
        }
        Database.exec("UPDATE files SET type = NULL WHERE id = ?", arrayOf(untyped))
        Database.exec("""ALTER TABLE files DROP COLUMN "objectKey", DROP COLUMN "size"""").assertTrue()
        Database.exec("ALTER TABLE files ALTER COLUMN bytes DROP DEFAULT").assertTrue()
        ConfigEntity.DatabaseVersion.set(10)

        testStorage.objects.clear()
    }

    private fun columns(): Map<String, Boolean> = Database.execQuery(
        "SELECT column_name, is_nullable FROM information_schema.columns WHERE table_name = 'files'"
    ).let { rs -> buildMap { while (rs.next()) put(rs.getString(1), rs.getString(2) == "YES") } }

    private fun fileIds(): Set<Uuid> = Database.execQuery("SELECT id FROM files").let { rs ->
        buildSet { while (rs.next()) add(rs.getObject(1, java.util.UUID::class.java).toKotlinUuid()) }
    }

    private fun migrate(storage: FileStorage) = Database { V11.migrate(storage) }

    private fun assertMigrated(storage: FileStorage) {
        assertEquals(referenced.toSet(), fileIds(), "Orphans must be deleted, and referenced files kept")

        val columns = columns()
        assertFalse("bytes" in columns, "The contents column must be dropped")
        assertEquals(false, columns["objectKey"], "objectKey must be NOT NULL")
        assertEquals(false, columns["size"], "size must be NOT NULL")

        Database {
            for (id in referenced) {
                val file = FileEntity[id]
                assertEquals("files/$id", file.objectKey)
                assertEquals(contents.getValue(id).size.toLong(), file.size)
                assertContentEquals(contents.getValue(id), storage.readBytes(file.objectKey))
            }
            assertEquals(ContentType.Image.PNG, FileEntity[untyped].contentType, "The type must be detected")
        }
        assertEquals(referenced.map { "files/$it" }.sorted(), storage.keys().sorted())

        // objectKey is unique
        Database.execQuery(
            """SELECT 1 FROM pg_indexes WHERE tablename = 'files' AND indexdef ILIKE '%UNIQUE%"objectKey"%'"""
        ).let { assertTrue(it.next(), "objectKey must have a unique index") }
    }

    @Test
    fun test_movesContents() {
        createPreV11Database()

        migrate(testStorage)

        assertMigrated(testStorage)
        // Files work normally afterwards
        Database { assertContentEquals(png, FileEntity[referenced.first()].readBytes()) }
    }

    @Test
    fun test_failedUpload_changesNothing_andCanBeRetried() {
        createPreV11Database()
        val storage = InMemoryFileStorage()

        assertFails { migrate(FailingFileStorage(storage, failPutOnCall = 3)) }

        // Everything rolled back: contents still in the database, orphans not deleted
        assertTrue("bytes" in columns())
        assertFalse("objectKey" in columns())
        assertEquals((referenced + orphans).toSet(), fileIds())

        // Running it again (like on the next start) works, overwriting what was already stored
        migrate(storage)
        assertMigrated(storage)
    }

    @Test
    fun test_wrongStoredSize_changesNothing() {
        createPreV11Database()

        assertFails { migrate(FailingFileStorage(InMemoryFileStorage(), wrongHeadSize = true)) }

        assertTrue("bytes" in columns())
        assertEquals((referenced + orphans).toSet(), fileIds())
    }

    @Test
    fun test_unavailableStorage_changesNothing() {
        createPreV11Database()

        assertFails { migrate(FailingFileStorage(InMemoryFileStorage(), unavailable = true)) }

        assertTrue("bytes" in columns())
        assertEquals((referenced + orphans).toSet(), fileIds())
    }

    @Test
    fun test_unavailableStorage_withoutFiles_migrates() {
        // Nothing to move: the storage isn't needed
        Database.init()
        Database.exec("""ALTER TABLE files ADD COLUMN bytes bytea NOT NULL, DROP COLUMN "objectKey", DROP COLUMN "size"""").assertTrue()

        migrate(FailingFileStorage(InMemoryFileStorage(), unavailable = true))

        val columns = columns()
        assertFalse("bytes" in columns)
        assertEquals(false, columns["objectKey"])
    }

    @Test
    fun test_newDatabase_nothingToMove() {
        // A new database is created with the current schema, and set to the current version directly
        Database.init()
        assertEquals(DatabaseMigration.VERSION, ConfigEntity.DatabaseVersion.get())

        migrate(FailingFileStorage(InMemoryFileStorage(), unavailable = true))

        assertFalse("bytes" in columns())
    }

    @Test
    fun test_runsOnStartup() {
        createPreV11Database()

        // Like a restart: forget the tables Exposed has seen in this connection
        Database { currentDialectMetadata.resetCaches() }

        // Database.init() runs pending migrations, with the configured storage
        val result = Database.init()

        assertEquals(Database.INIT_RESULT_MIGRATION_EXECUTED, result and Database.INIT_RESULT_MIGRATION_EXECUTED)
        assertEquals(DatabaseMigration.VERSION, ConfigEntity.DatabaseVersion.get())
        assertMigrated(FileStorageProvider.current)
    }
}
