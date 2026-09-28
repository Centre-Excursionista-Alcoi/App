package org.centrexcursionistalcoi.app.database.entity

import io.ktor.http.ContentType
import kotlinx.coroutines.test.runTest
import org.centrexcursionistalcoi.app.assertJsonEquals
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.data.DepartmentMemberInfo
import org.centrexcursionistalcoi.app.data.DepartmentRole
import org.centrexcursionistalcoi.app.data.FileWithContext
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.utils.encodeEntityToString
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.request.UpdateDepartmentRequest
import org.centrexcursionistalcoi.app.security.UserSession
import org.centrexcursionistalcoi.app.test.FakeAdminUser
import org.centrexcursionistalcoi.app.test.FakeUser
import org.centrexcursionistalcoi.app.test.FakeUser2
import org.centrexcursionistalcoi.app.utils.toUUID
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.uuid.Uuid
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TestDepartment {
    @Test
    fun `test entity serializes the same as data class`() = runTest {
        Database.initForTests()

        val user = Database { FakeUser.provideEntity() }
        val user2 = Database { FakeUser2.provideEntity() }

        val imageFileId: Uuid = "ffac99cf-8f56-426b-aff6-691d0e1df8dc".toUUID()
        val departmentId = "ee87b144-7b13-40ed-a465-ef00c5666ea0".toUUID()
        val departmentMember1Id = "9f91abd5-60b8-4092-99e9-ec59c4be09ab".toUUID()
        val departmentMember2Id = "29ddada8-2d21-433f-ab95-8bd5a5afc4b9".toUUID()

        val departmentEntity = Database {
            val imageFileEntity = FileEntity.create(
                bytes = byteArrayOf(1, 2, 3),
                name = "test_image.png",
                contentType = ContentType.parse("image/png"),
                id = imageFileId,
            )
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
                    roles = listOf(DepartmentRole.ADMIN)
                }
                DepartmentMemberEntity.new(departmentMember2Id) {
                    userReference = user2
                    department = departmentEntity
                    confirmed = false
                    roles = emptyList()
                }
            }
        }

        val departmentClass = Department(
            id = departmentId,
            displayName = "Test Department",
            image = imageFileId,
            members = listOf(
                DepartmentMemberInfo(
                    id = departmentMember1Id,
                    userSub = FakeUser.SUB,
                    departmentId = departmentId,
                    confirmed = true,
                    roles = listOf(DepartmentRole.ADMIN),
                ),
                DepartmentMemberInfo(
                    id = departmentMember2Id,
                    userSub = FakeUser2.SUB,
                    departmentId = departmentId,
                    confirmed = false,
                    roles = emptyList(),
                )
            ),
            // Neither is seeded for this department, but the wire encoder (Departments.extraColumns) always
            // emits both as a real (possibly empty) list, never omits them -- unlike this hand-built Department,
            // whose declared `= null` default would otherwise get silently dropped by Json's encodeDefaults=false.
            qualifications = emptyList(),
            qualificationGrants = emptyList(),
        )

        val adminSession = UserSession(FakeAdminUser.SUB, FakeAdminUser.FULL_NAME, FakeAdminUser.EMAIL, FakeAdminUser.groups)
        assertJsonEquals(
            json.encodeEntityToString(departmentEntity, adminSession),
            json.encodeToString(Department.serializer(), departmentClass)
        )

        // The member roster is filtered per-viewer (see Departments.extraColumns): an anonymous caller sees none
        // of it, and a plain member sees only their own row -- both regardless of the roster's real content.
        val anonymous = json.encodeToString(Department.serializer(), departmentClass.copy(members = emptyList()))
        assertJsonEquals(json.encodeEntityToString(departmentEntity, session = null), anonymous)

        val user2Session = UserSession(FakeUser2.SUB, FakeUser2.FULL_NAME, FakeUser2.EMAIL, FakeUser2.groups)
        val selfOnly = json.encodeToString(
            Department.serializer(),
            departmentClass.copy(members = departmentClass.members.orEmpty().filter { it.userSub == FakeUser2.SUB }),
        )
        assertJsonEquals(json.encodeEntityToString(departmentEntity, user2Session), selfOnly)
    }

    @Test
    fun `test patching`() = runTest {
        Database.initForTests()

        val departmentEntity = Database {
            DepartmentEntity.new {
                displayName = "Test Department"
                image = transaction {
                    FileEntity.create(
                        bytes = byteArrayOf(1, 2, 3),
                        name = "test_image.png",
                        contentType = ContentType.parse("image/png"),
                    )
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
        assertContentEquals(byteArrayOf(4, 5, 6), image.readBytes())
    }
}
