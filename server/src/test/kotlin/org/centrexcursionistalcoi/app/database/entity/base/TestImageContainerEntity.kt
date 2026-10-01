package org.centrexcursionistalcoi.app.database.entity.base

import io.ktor.http.ContentType
import kotlin.time.Instant
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.Uuid
import org.centrexcursionistalcoi.app.ResourcesUtils
import org.centrexcursionistalcoi.app.data.FileWithContext
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.DepartmentEntity
import org.centrexcursionistalcoi.app.database.entity.FileEntity
import org.centrexcursionistalcoi.app.storage.createTestFile
import org.centrexcursionistalcoi.app.storage.testStorage
import org.centrexcursionistalcoi.app.utils.UPLOADED_PART
import org.centrexcursionistalcoi.app.utils.withUploadedFile

class TestImageContainerEntity {
    private val imageBytes = ResourcesUtils.bytesFromResource("/square.png")

    @BeforeTest
    fun setUp() {
        Database.initForTests()
    }

    @AfterTest
    fun tearDown() {
        Database.clear()
    }

    private fun newDepartment(image: FileEntity? = null) = Database {
        DepartmentEntity.new {
            displayName = "Department"
            this.image = image
        }
    }

    @Test
    @Suppress("DEPRECATION")
    fun test_updateOrSetImage_bytes() {
        val oldImage = createTestFile(byteArrayOf(1, 2, 3))
        val department = newDepartment(oldImage)

        Database { department.updateOrSetImage(imageBytes, "square.png", ContentType.Image.PNG) }

        val image = Database { DepartmentEntity[department.id].image }
        assertNotNull(image)
        assertEquals("square.png", image.name)
        assertEquals(ContentType.Image.PNG, image.contentType)
        assertContentEquals(imageBytes, image.readBytes())
        // The old image is gone, with its contents
        assertNull(Database { FileEntity.findById(oldImage.id) })
        assertEquals(listOf(image.objectKey), testStorage.keys())
    }

    @Test
    fun test_updateOrSetImage_sameId_replacesContents() {
        val oldImage = createTestFile(byteArrayOf(1, 2, 3), name = "old.bin")
        Database { oldImage.lastModified = Instant.fromEpochSeconds(1_000) }
        val oldKey = Database { oldImage.objectKey }
        val department = newDepartment(oldImage)

        withUploadedFile(imageBytes) {
            Database {
                department.updateOrSetImage(
                    FileWithContext(name = "square.png", id = oldImage.id.value, part = UPLOADED_PART)
                )
            }
        }

        val image = Database { DepartmentEntity[department.id].image }
        assertNotNull(image)
        // Same file (clients keep referencing it by id), new contents at a new key
        assertEquals(oldImage.id, image.id)
        assertNotEquals(oldKey, image.objectKey)
        assertEquals("square.png", image.name)
        // The type is detected from the contents when not given
        assertEquals(ContentType.Image.PNG, image.contentType)
        assertEquals(imageBytes.size.toLong(), image.size)
        assertTrue(image.lastModified > Instant.fromEpochSeconds(1_000))
        assertContentEquals(imageBytes, image.readBytes())
        assertEquals(listOf(image.objectKey), testStorage.keys(), "The old contents must be deleted")
    }

    @Test
    fun test_updateOrSetImage_sameId_rollbackKeepsOldContents() {
        val oldImage = createTestFile(byteArrayOf(1, 2, 3))
        val department = newDepartment(oldImage)

        runCatching {
            withUploadedFile(imageBytes) {
                Database {
                    department.updateOrSetImage(FileWithContext(id = oldImage.id.value, part = UPLOADED_PART))
                    error("Rolled back")
                }
            }
        }

        val image = Database { FileEntity[oldImage.id] }
        assertEquals(oldImage.objectKey, image.objectKey)
        assertContentEquals(byteArrayOf(1, 2, 3), image.readBytes())
        assertEquals(listOf(oldImage.objectKey), testStorage.keys(), "The new contents must be deleted")
    }

    @Test
    fun test_updateOrSetImage_differentId() {
        val oldImage = createTestFile(byteArrayOf(1, 2, 3))
        val department = newDepartment(oldImage)
        val newId = Uuid.random()

        withUploadedFile(imageBytes) {
            Database { department.updateOrSetImage(FileWithContext(name = "square.png", id = newId, part = UPLOADED_PART)) }
        }

        val image = Database { DepartmentEntity[department.id].image }
        assertNotNull(image)
        assertEquals(newId, image.id.value)
        assertContentEquals(imageBytes, image.readBytes())
        assertNull(Database { FileEntity.findById(oldImage.id) })
        assertEquals(listOf(image.objectKey), testStorage.keys())
    }

    @Test
    fun test_updateOrSetImage_noImage() {
        val department = newDepartment()

        withUploadedFile(imageBytes) {
            Database { department.updateOrSetImage(FileWithContext(part = UPLOADED_PART)) }
        }

        val image = Database { DepartmentEntity[department.id].image }
        assertNotNull(image)
        assertContentEquals(imageBytes, image.readBytes())
    }

    @Test
    fun test_updateOrSetImage_idOfAnotherFile_isNotReused() {
        // A client can't take over someone else's file by sending its id
        val otherFile = createTestFile(byteArrayOf(9, 9, 9))
        val department = newDepartment()

        withUploadedFile(imageBytes) {
            Database { department.updateOrSetImage(FileWithContext(id = otherFile.id.value, part = UPLOADED_PART)) }
        }

        val image = Database { DepartmentEntity[department.id].image }
        assertNotNull(image)
        assertNotEquals(otherFile.id, image.id)
        assertContentEquals(byteArrayOf(9, 9, 9), Database { FileEntity[otherFile.id] }.readBytes())
    }
}
