package org.centrexcursionistalcoi.app.routes

import kotlin.uuid.toJavaUuid
import org.centrexcursionistalcoi.app.ApplicationTestBase
import org.centrexcursionistalcoi.app.ResourcesUtils
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.data.InventoryItemType
import org.centrexcursionistalcoi.app.database.entity.DepartmentEntity
import org.centrexcursionistalcoi.app.database.entity.InventoryItemTypeEntity
import org.centrexcursionistalcoi.app.routes.ProvidedRouteTests.runTestsOnRoute
import org.centrexcursionistalcoi.app.utils.toUUID
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory

class TestRoutes : ApplicationTestBase() {
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
            dataEntitySerializer = Department.serializer()
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
            dataEntitySerializer = InventoryItemType.serializer()
        ),
    ).flatten()
}
