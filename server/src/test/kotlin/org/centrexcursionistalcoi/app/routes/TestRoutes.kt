package org.centrexcursionistalcoi.app.routes

import java.util.Random
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid
import org.centrexcursionistalcoi.app.ApplicationTestBase
import org.centrexcursionistalcoi.app.ResourcesUtils
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.data.InventoryItem
import org.centrexcursionistalcoi.app.data.InventoryItemType
import org.centrexcursionistalcoi.app.data.Post
import org.centrexcursionistalcoi.app.database.entity.DepartmentEntity
import org.centrexcursionistalcoi.app.database.entity.InventoryItemEntity
import org.centrexcursionistalcoi.app.database.entity.InventoryItemTypeEntity
import org.centrexcursionistalcoi.app.database.entity.PostEntity
import org.centrexcursionistalcoi.app.database.table.DepartmentMembers
import org.centrexcursionistalcoi.app.routes.ProvidedRouteTests.runTestsOnRoute
import org.centrexcursionistalcoi.app.utils.FileBytesWrapper.Companion.wrapFile
import org.centrexcursionistalcoi.app.utils.Zero
import org.centrexcursionistalcoi.app.utils.toUUID
import org.jetbrains.exposed.v1.jdbc.insert
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory

class TestRoutes : ApplicationTestBase() {
    private val testDepartmentId = "2c8876a9-ff7e-4dd8-b39c-dd270631b9d2".toUUID()
    private val testItemTypeId = "3a305821-03f2-4ca8-98c8-ffe64e262cf7".toUUID()
    private val testItemId = "3c509110-2115-4fd7-b3d5-20692cb935e5".toUUID()
    private val random = Random(0)

    /** Regexp for UUIDv4 */
    private val uuidv4 = "[0-9(a-f|A-F)]{8}-[0-9(a-f|A-F)]{4}-4[0-9(a-f|A-F)]{3}-[89ab][0-9(a-f|A-F)]{3}-[0-9(a-f|A-F)]{12}"

    @TestFactory
    fun runRouteTests(): List<DynamicTest> = listOf(
        runTestsOnRoute(
            title = "Department",
            baseUrl = "/departments",
            requiredCreationValuesProvider = mapOf("displayName" to { "Test Department" }),
            optionalCreationValuesProvider = mapOf(
                "image" to { ResourcesUtils.bytesFromResource("/square.png").wrapFile() }
            ),
            locationRegex = "/departments/$uuidv4+".toRegex(),
            entityClass = DepartmentEntity,
            dataEntitySerializer = Department.serializer(),
            stubEntityProvider = {
                DepartmentEntity.new(testDepartmentId) {
                    displayName = "Test Department"
                }
            },
            invalidEntityId = Uuid.Zero.toJavaUuid(),
        ),
        runTestsOnRoute(
            title = "Posts",
            baseUrl = "/posts",
            requiredCreationValuesProvider = mapOf(
                "title" to { "Test Post" },
                "content" to { "Content for Test Post." },
            ),
            optionalCreationValuesProvider = mapOf(
                "department" to { testDepartmentId },
            ),
            userEntityPatches = { user ->
                // Make the user a member of the test department
                DepartmentMembers.insert {
                    it[departmentId] = testDepartmentId
                    it[userSub] = user.sub
                    it[confirmed] = true
                }
            },
            locationRegex = "/posts/$uuidv4+".toRegex(),
            entityClass = PostEntity,
            dataEntitySerializer = Post.serializer(),
            foreignTypesAssociations = mapOf("department" to DepartmentEntity),
            auxiliaryEntitiesProvider = {
                DepartmentEntity.new(testDepartmentId) {
                    displayName = "Test Department"
                }
            },
            stubEntityProvider = {
                PostEntity.new {
                    title = "Test Post"
                    content = "Content for Test Post."
                }
            },
            invalidEntityId = Uuid.Zero.toJavaUuid(),
        ),
        runTestsOnRoute(
            title = "Inventory Item Types",
            baseUrl = "/inventory/types",
            requiredCreationValuesProvider = mapOf("displayName" to { "Test Item Type" }),
            optionalCreationValuesProvider = mapOf(
                "description" to { "This is a test description for the item" },
                "image" to { ResourcesUtils.bytesFromResource("/square.png").wrapFile() }
            ),
            locationRegex = "/inventory/types/$uuidv4+".toRegex(),
            entityClass = InventoryItemTypeEntity,
            dataEntitySerializer = InventoryItemType.serializer(),
            stubEntityProvider = {
                InventoryItemTypeEntity.new(testItemTypeId) {
                    displayName = "Test Item Type"
                }
            },
            invalidEntityId = Uuid.Zero.toJavaUuid(),
        ),
        runTestsOnRoute(
            title = "Inventory Items",
            baseUrl = "/inventory/items",
            requiredCreationValuesProvider = mapOf("type" to { testItemTypeId }),
            optionalCreationValuesProvider = mapOf(
                "variation" to { "This is a test variation for the item" },
                "nfcId" to { ByteArray(6).apply { random.nextBytes(this) } },
            ),
            locationRegex = "/inventory/items/$uuidv4+".toRegex(),
            entityClass = InventoryItemEntity,
            dataEntitySerializer = InventoryItem.serializer(),
            foreignTypesAssociations = mapOf("type" to InventoryItemTypeEntity),
            auxiliaryEntitiesProvider = {
                InventoryItemTypeEntity.new(testItemTypeId) {
                    displayName = "Test Item Type"
                }
            },
            stubEntityProvider = {
                InventoryItemEntity.new(testItemId) {
                    type = InventoryItemTypeEntity[testItemTypeId]
                }
            },
            invalidEntityId = Uuid.Zero.toJavaUuid(),
        ),
    ).flatten()
}
