package org.centrexcursionistalcoi.app.database.entity

import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.uuid.toKotlinUuid
import kotlinx.coroutines.test.runTest
import org.centrexcursionistalcoi.app.assertJsonEquals
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.Database.TEST_URL
import org.centrexcursionistalcoi.app.database.utils.encodeEntityToString
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.request.UpdateDepartmentRequest
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.assertNotNull

class TestDepartment {
    @Test
    fun `test entity serializes the same as data class`() = runTest {
        Database.init(TEST_URL)

        var imageFileId: UUID? = null
        val departmentEntity = Database {
            val imageFileEntity = FileEntity.new {
                name = "test_image.png"
                type = "image/png"
                data = byteArrayOf(1, 2, 3)
            }
            imageFileId = imageFileEntity.id.value
            DepartmentEntity.new {
                displayName = "Test Department"
                image = imageFileEntity
            }
        }
        val departmentClass = Department(
            id = departmentEntity.id.value.toKotlinUuid(),
            displayName = "Test Department",
            image = imageFileId?.toKotlinUuid()
        )

        assertJsonEquals(
            json.encodeEntityToString(departmentEntity),
            json.encodeToString(Department.serializer(), departmentClass)
        )
    }

    @Test
    fun `test patching`() = runTest {
        Database.init(TEST_URL)

        val departmentEntity = Database {
            DepartmentEntity.new {
                displayName = "Test Department"
                image = transaction {
                    FileEntity.new {
                        name = "test_image.png"
                        type = "image/png"
                        data = byteArrayOf(1, 2, 3)
                    }
                }
            }
        }

        Database {
            departmentEntity.patch(
                UpdateDepartmentRequest(
                    "Updated Department",
                    byteArrayOf(4, 5, 6)
                )
            )
        }

        val updatedEntity = Database { DepartmentEntity[departmentEntity.id] }
        assertEquals("Updated Department", updatedEntity.displayName)
        val image = Database { updatedEntity.image }
        assertNotNull(image)
        assertContentEquals(byteArrayOf(4, 5, 6), image.data)
    }
}
