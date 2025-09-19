package org.centrexcursionistalcoi.app.database.entity

import kotlinx.coroutines.test.runTest
import org.centrexcursionistalcoi.app.assertJsonEquals
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.Database.TEST_URL
import org.centrexcursionistalcoi.app.database.table.Departments
import org.centrexcursionistalcoi.app.database.table.Files
import org.centrexcursionistalcoi.app.database.utils.encodeEntityToString
import org.centrexcursionistalcoi.app.database.utils.insert
import org.centrexcursionistalcoi.app.json
import java.util.UUID
import kotlin.test.Test

class TestDepartment {
    @Test
    fun `test entity serializes the same as data class`() = runTest {
        Database.init(TEST_URL)

        var imageFileId: UUID? = null
        val departmentEntity = Database {
            val imageFile = FileEntity.insert {
                it[Files.name] = "test_image.png"
                it[Files.type] = "image/png"
                it[Files.data] = byteArrayOf(1, 2, 3)
            }
            imageFileId = imageFile.id.value
            DepartmentEntity.insert {
                it[Departments.displayName] = "Test Department"
                it[Departments.imageFile] = imageFile.id
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
