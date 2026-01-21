package org.centrexcursionistalcoi.app.database.entity

import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.uuid.toKotlinUuid
import kotlinx.coroutines.test.runTest
import org.centrexcursionistalcoi.app.assertJsonEquals
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.data.DepartmentMemberInfo
import org.centrexcursionistalcoi.app.data.FileWithContext
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.utils.encodeEntityToString
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.request.UpdateDepartmentRequest
import org.centrexcursionistalcoi.app.test.*
import org.centrexcursionistalcoi.app.utils.toUUID
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.assertNotNull

class TestDepartment {
    @Test
    fun `test entity serializes the same as data class`() = runTest {
        Database.initForTests()

        val user = Database { FakeUser.provideEntity() }
        val user2 = Database { FakeUser2.provideEntity() }

        val imageFileId: UUID = "ffac99cf-8f56-426b-aff6-691d0e1df8dc".toUUID()
        val departmentId = "ee87b144-7b13-40ed-a465-ef00c5666ea0".toUUID()
        val departmentMember1Id = "9f91abd5-60b8-4092-99e9-ec59c4be09ab".toUUID()
        val departmentMember2Id = "29ddada8-2d21-433f-ab95-8bd5a5afc4b9".toUUID()

        val departmentEntity = Database {
            val imageFileEntity = FileEntity.new(imageFileId) {
                name = "test_image.png"
                type = "image/png"
                bytes = byteArrayOf(1, 2, 3)
            }
            DepartmentEntity.new(departmentId) {
                displayName = "Test Department"
                image = imageFileEntity
            }
        }.also { departmentEntity ->
            Database {
                DepartmentMemberEntity.new(departmentMember1Id) {
                    userReference = user
                    department = departmentEntity
                    confirmed = true
                    isManager = true
                }
                DepartmentMemberEntity.new(departmentMember2Id) {
                    userReference = user2
                    department = departmentEntity
                    confirmed = false
                    isManager = false
                }
            }
        }

        val departmentClass = Department(
            id = departmentId.toKotlinUuid(),
            displayName = "Test Department",
            image = imageFileId.toKotlinUuid(),
            members = listOf(
                DepartmentMemberInfo(
                    id = departmentMember1Id.toKotlinUuid(),
                    userSub = FakeUser.SUB,
                    departmentId = departmentId.toKotlinUuid(),
                    confirmed = true,
                    isManager = true,
                ),
                DepartmentMemberInfo(
                    id = departmentMember2Id.toKotlinUuid(),
                    userSub = FakeUser2.SUB,
                    departmentId = departmentId.toKotlinUuid(),
                    confirmed = false,
                    isManager = false,
                )
            )
        )

        assertJsonEquals(
            json.encodeEntityToString(departmentEntity),
            json.encodeToString(Department.serializer(), departmentClass)
        )
    }

    @Test
    fun `test patching`() = runTest {
        Database.initForTests()

        val departmentEntity = Database {
            DepartmentEntity.new {
                displayName = "Test Department"
                image = transaction {
                    FileEntity.new {
                        name = "test_image.png"
                        type = "image/png"
                        bytes = byteArrayOf(1, 2, 3)
                    }
                }
            }
        }

        Database {
            departmentEntity.patch(
                UpdateDepartmentRequest(
                    "Updated Department",
                    FileWithContext(byteArrayOf(4, 5, 6))
                )
            )
        }

        val updatedEntity = Database { DepartmentEntity[departmentEntity.id] }
        assertEquals("Updated Department", updatedEntity.displayName)
        val image = Database { updatedEntity.image }
        assertNotNull(image)
        assertContentEquals(byteArrayOf(4, 5, 6), image.bytes)
    }
}
