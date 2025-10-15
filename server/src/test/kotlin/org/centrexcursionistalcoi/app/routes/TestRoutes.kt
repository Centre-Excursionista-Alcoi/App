package org.centrexcursionistalcoi.app.routes

import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid
import org.centrexcursionistalcoi.app.ApplicationTestBase
import org.centrexcursionistalcoi.app.ResourcesUtils
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.data.InventoryItem
import org.centrexcursionistalcoi.app.data.InventoryItemType
import org.centrexcursionistalcoi.app.database.entity.DepartmentEntity
import org.centrexcursionistalcoi.app.database.entity.InventoryItemEntity
import org.centrexcursionistalcoi.app.database.entity.InventoryItemTypeEntity
import org.centrexcursionistalcoi.app.routes.ProvidedRouteTests.runTestsOnRoute
import org.centrexcursionistalcoi.app.utils.Zero
import org.centrexcursionistalcoi.app.utils.toUUID
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory

class TestRoutes : ApplicationTestBase() {
    private val testDepartmentId = 123
    private val testItemTypeId = "3a305821-03f2-4ca8-98c8-ffe64e262cf7".toUUID()
    private val testItemId = "3c509110-2115-4fd7-b3d5-20692cb935e5".toUUID()

    @TestFactory
    fun runRouteTests(): List<DynamicTest> = listOf(
        runTestsOnRoute(
            title = "Department",
            baseUrl = "/departments",
            requiredCreationValues = mapOf("displayName" to { "Test Department" }),
            optionalCreationValues = mapOf("image" to { ResourcesUtils.bytesFromResource("/square.png") }),
            locationRegex = "/departments/\\d+".toRegex(),
            entityClass = DepartmentEntity,
            idTypeConverter = { it.toInt() },
            exposedIdTypeConverter = { it },
            dataEntitySerializer = Department.serializer(),
            stubEntityProvider = {
                DepartmentEntity.new(testDepartmentId) {
                    displayName = "Test Department"
                }
            },
            invalidEntityId = 9999,
        ),
        runTestsOnRoute(
            title = "Inventory Item Types",
            baseUrl = "/inventory/types",
            requiredCreationValues = mapOf("displayName" to { "Test Item Type" }),
            optionalCreationValues = mapOf(
                "description" to { "This is a test description for the item" },
                "image" to { ResourcesUtils.bytesFromResource("/square.png") }
            ),
            locationRegex = "/inventory/types/[a-z0-9-]+".toRegex(),
            entityClass = InventoryItemTypeEntity,
            idTypeConverter = { it.toUUID() },
            exposedIdTypeConverter = { it.toJavaUuid() },
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
            requiredCreationValues = mapOf("type" to { testItemTypeId }),
            optionalCreationValues = mapOf(
                "variation" to { "This is a test variation for the item" },
            ),
            locationRegex = "/inventory/items/[a-z0-9-]+".toRegex(),
            entityClass = InventoryItemEntity,
            idTypeConverter = { it.toUUID() },
            exposedIdTypeConverter = { it.toJavaUuid() },
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
