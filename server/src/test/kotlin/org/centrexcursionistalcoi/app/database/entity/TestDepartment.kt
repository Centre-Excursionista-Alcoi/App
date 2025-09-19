package org.centrexcursionistalcoi.app.database.entity

import kotlinx.coroutines.test.runTest
import org.centrexcursionistalcoi.app.assertJsonEquals
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.Database.TEST_URL
import org.centrexcursionistalcoi.app.database.table.Departments
import org.centrexcursionistalcoi.app.database.table.Files
import org.centrexcursionistalcoi.app.database.utils.encodeEntityToString
import org.centrexcursionistalcoi.app.json
import java.util.UUID
import kotlin.test.Test

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
                imageFile = imageFileEntity.id
            }
        }
        val departmentClass = Department(
            id = departmentEntity.id.value,
            displayName = "Test Department",
            imageFile = imageFileId.toString()
        )

        assertJsonEquals(
            json.encodeEntityToString(departmentEntity),
            json.encodeToString(Department.serializer(), departmentClass)
        )
    }
}
