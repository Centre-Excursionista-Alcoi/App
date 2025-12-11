package org.centrexcursionistalcoi.app.database.entity

import kotlinx.coroutines.test.runTest
import org.centrexcursionistalcoi.app.assertJsonEquals
import org.centrexcursionistalcoi.app.data.InventoryItemType
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.utils.encodeEntityToString
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.utils.toUUID
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.test.Test
import kotlin.uuid.toKotlinUuid

class TestInventoryItemType {
    @Test
    fun `test entity serializes the same as data class`() = runTest {
        Database.initForTests()

        val id = "81653ead-5950-4b8a-92f9-88d6982fe769".toUUID()
        val departmentId = "651abec5-7e83-4f8c-88e2-eff25f098d96".toUUID()
        val imageFileId = "3c3346f8-0ad7-4338-a844-33e4ae7c7152".toUUID()

        val departmentEntity = Database {
            DepartmentEntity.new(departmentId) {
                displayName = "Test Department"
            }
        }
        val entity = Database {
            InventoryItemTypeEntity.new(id) {
                displayName = "Test Type"
                description = "Test description"
                categories = listOf("Category1", "Category2")
                department = departmentEntity
                image = transaction {
                    FileEntity.new(imageFileId) {
                        name = "test_image.png"
                        type = "image/png"
                        bytes = byteArrayOf(1, 2, 3)
                    }
                }
            }
        }
        val typeClass = InventoryItemType(
            id = id.toKotlinUuid(),
            displayName = "Test Type",
            description = "Test description",
            categories = listOf("Category1", "Category2"),
            department = departmentId.toKotlinUuid(),
            image = imageFileId?.toKotlinUuid()
        )

        assertJsonEquals(
            json.encodeEntityToString(entity),
            json.encodeToString(InventoryItemType.serializer(), typeClass),
            ignoreKeys = setOf("files")
        )
    }
}
