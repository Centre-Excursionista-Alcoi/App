package org.centrexcursionistalcoi.app.database.entity

import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import io.ktor.http.ContentType
import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.centrexcursionistalcoi.app.ResourcesUtils
import org.centrexcursionistalcoi.app.data.FileWithContext
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.FileReferences
import org.centrexcursionistalcoi.app.database.table.Files
import org.centrexcursionistalcoi.app.storage.FailingFileStorage
import org.centrexcursionistalcoi.app.storage.FileStorageProvider
import org.centrexcursionistalcoi.app.storage.FilesCleanup
import org.centrexcursionistalcoi.app.storage.InMemoryFileStorage
import org.centrexcursionistalcoi.app.storage.createTestFile
import org.centrexcursionistalcoi.app.storage.testStorage
import kotlinx.coroutines.test.runTest
import org.centrexcursionistalcoi.app.utils.UPLOADED_PART
import org.centrexcursionistalcoi.app.utils.withUploadedFile
import org.jetbrains.exposed.v1.jdbc.selectAll

class TestFileEntityStorage {
    @BeforeTest
    fun setUp() {
        Database.initForTests()
    }

    @AfterTest
    fun tearDown() {
        Database.clear()
    }

    @Test
    fun test_create_storesContents() {
        val png = ResourcesUtils.bytesFromResource("/square.png")
        val file = createTestFile(png, name = "square.png")

        assertEquals(png.size.toLong(), file.size)
        assertEquals(listOf(file.objectKey), testStorage.keys())
        assertContentEquals(png, file.readBytes())
        assertContentEquals(png, file.openStream().use { it.readBytes() })
        // Detected from the contents, since none was given
        assertEquals(ContentType.Image.PNG, file.contentType)
    }

    @Test
    fun test_create_contentType() {
        val pdf = ResourcesUtils.bytesFromResource("/document.pdf")
        // Detected when generic
        assertEquals(ContentType.Application.Pdf, createTestFile(pdf, contentType = ContentType.Application.OctetStream).contentType)
        // Kept when given
        assertEquals(ContentType.Text.Plain, createTestFile(byteArrayOf(1), contentType = ContentType.Text.Plain).contentType)
        // Generic when unknown
        assertEquals(ContentType.Application.OctetStream, createTestFile(byteArrayOf(1)).contentType)
    }

    @Test
    fun test_create_rolledBack_deletesContents() {
        runCatching {
            Database {
                FileEntity.create(byteArrayOf(1, 2, 3), "file")
                error("Rolled back")
            }
        }

        assertEquals(0, Database { FileEntity.count() })
        assertEquals(emptyList(), testStorage.keys())
    }

    @Test
    fun test_create_inNestedTransaction_rolledBack_deletesContents() {
        runCatching {
            Database {
                // A nested Database {} runs in the same transaction
                Database { FileEntity.create(byteArrayOf(1, 2, 3), "file") }
                error("Rolled back")
            }
        }

        assertEquals(emptyList(), testStorage.keys())
    }

    @Test
    fun test_create_withTakenId_usesNewId() {
        val existing = createTestFile(byteArrayOf(1, 2, 3))

        val file = createTestFile(byteArrayOf(4, 5, 6), id = existing.id.value)

        assertNotEquals(existing.id, file.id)
        assertContentEquals(byteArrayOf(1, 2, 3), Database { FileEntity[existing.id] }.readBytes())
    }

    @Test
    fun test_create_withFreeId_usesIt() {
        val id = Uuid.random()
        assertEquals(id, createTestFile(byteArrayOf(1), id = id).id.value)
    }

    @Test
    fun test_delete_deletesContentsAfterCommit() {
        val file = createTestFile(byteArrayOf(1, 2, 3))

        Database {
            FileEntity[file.id].delete()
            // Not until the transaction commits: it could still roll back
            assertNotNull(testStorage.head(file.objectKey))
        }

        assertNull(testStorage.head(file.objectKey))
    }

    @Test
    fun test_delete_rolledBack_keepsContents() {
        val file = createTestFile(byteArrayOf(1, 2, 3))

        runCatching {
            Database {
                FileEntity[file.id].delete()
                error("Rolled back")
            }
        }

        assertNotNull(Database { FileEntity.findById(file.id) })
        assertContentEquals(byteArrayOf(1, 2, 3), testStorage.readBytes(file.objectKey))
    }

    @Test
    fun test_delete_storageFailure_doesNotFailTheTransaction() {
        val storage = FailingFileStorage(InMemoryFileStorage(), failDeletes = true)
        FileStorageProvider.useForTests(storage)
        val file = createTestFile(byteArrayOf(1, 2, 3))

        // The row is deleted, the contents stay orphaned (and are logged)
        Database { FileEntity[file.id].delete() }

        assertNull(Database { FileEntity.findById(file.id) })
        assertNotNull(storage.head(file.objectKey))
    }

    @Test
    fun test_updateOrCreate_deletesOnlyOwnedFiles() {
        val owned = createTestFile(byteArrayOf(1))
        val notOwned = createTestFile(byteArrayOf(2))

        Database {
            FileEntity.updateOrCreate(FileWithContext(id = notOwned.id.value), ownedIds = listOf(owned.id.value))
        }
        assertNotNull(Database { FileEntity.findById(notOwned.id) }, "A file not owned by the entity must not be deleted")

        var deleted: FileEntity? = null
        Database {
            FileEntity.updateOrCreate(FileWithContext(id = owned.id.value), ownedIds = listOf(owned.id.value)) {
                deleted = it
            }
        }
        assertEquals(owned.id, deleted?.id)
        assertNull(Database { FileEntity.findById(owned.id) })
        assertNull(testStorage.head(owned.objectKey))
    }

    @Test
    fun test_updateOrCreate_createsFile() {
        val bytes = withUploadedFile(byteArrayOf(1, 2)) {
            Database {
                FileEntity.updateOrCreate(FileWithContext(name = "new", part = UPLOADED_PART), ownedIds = emptyList())?.readBytes()
            }
        }
        assertContentEquals(byteArrayOf(1, 2), bytes)
    }

    @Test
    fun test_deleteIfUnreferenced() {
        val referenced = createTestFile(byteArrayOf(1))
        val unreferenced = createTestFile(byteArrayOf(2))
        Database { DepartmentEntity.new { displayName = "Department"; image = referenced } }

        Database {
            FileEntity.deleteIfUnreferenced(FileEntity[referenced.id])
            FileEntity.deleteIfUnreferenced(FileEntity[unreferenced.id])
        }

        assertNotNull(Database { FileEntity.findById(referenced.id) })
        assertNull(Database { FileEntity.findById(unreferenced.id) })
        assertEquals(listOf(referenced.objectKey), testStorage.keys())
    }

    @Test
    fun test_toData_hasNoContents() {
        val file = createTestFile(byteArrayOf(1, 2, 3), name = "a.bin")
        val data = Database { file.toData() }
        assertEquals(0, data.bytes.size)
        assertEquals("a.bin", data.name)
        assertEquals(file.id.value, data.id)
    }

    @Test
    fun test_filesCleanup_deletesOnlyOldUnreferencedFiles() = runTest {
        val old = (Clock.System.now() - (3 * 24 * 3600).seconds)
        val oldUnreferenced = createTestFile(byteArrayOf(1))
        val oldReferenced = createTestFile(byteArrayOf(2))
        val newUnreferenced = createTestFile(byteArrayOf(3))
        Database {
            FileEntity[oldUnreferenced.id].lastModified = old
            FileEntity[oldReferenced.id].lastModified = old
            DepartmentEntity.new { displayName = "Department"; image = FileEntity[oldReferenced.id] }
        }

        FilesCleanup.run()

        val remaining = Database { FileEntity.all().map { it.id } }.toSet()
        assertEquals(setOf(oldReferenced.id, newUnreferenced.id), remaining)
        assertNull(testStorage.head(oldUnreferenced.objectKey))
        assertTrue(Database { FileReferences.isReferenced(oldReferenced.id.value) })
        assertEquals(2, Database { Files.selectAll().count() }.toInt())
    }
}
