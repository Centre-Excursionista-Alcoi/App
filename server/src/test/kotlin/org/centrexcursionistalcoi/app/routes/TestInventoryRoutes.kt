package org.centrexcursionistalcoi.app.routes

import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import org.centrexcursionistalcoi.app.ApplicationTestBase
import org.centrexcursionistalcoi.app.assertStatusCode
import org.centrexcursionistalcoi.app.database.entity.DepartmentEntity
import org.centrexcursionistalcoi.app.database.entity.InventoryItemEntity
import org.centrexcursionistalcoi.app.database.entity.InventoryItemTypeEntity
import org.centrexcursionistalcoi.app.database.table.DepartmentMembers
import org.centrexcursionistalcoi.app.test.FakeUser
import org.centrexcursionistalcoi.app.test.LoginType
import org.jetbrains.exposed.v1.jdbc.insert

/**
 * Regression coverage for GET /inventory/types/{id} and GET /inventory/items/{id}: they previously fetched the
 * entity directly with no check at all, even though GET /inventory/types and GET /inventory/items (their
 * listProviders) return an *empty list* for an anonymous or non-member caller -- so a caller who knew or guessed
 * an id could always read a department's private equipment catalog (including manufacturerTraceabilityCode/nfcId)
 * regardless of that restriction.
 */
class TestInventoryRoutes : ApplicationTestBase() {
    @Test
    fun test_get_itemType_byId_notLoggedIn_notVisible() = runApplicationTest(
        databaseInitBlock = {
            InventoryItemTypeEntity.new { displayName = "Private Item Type" }
        },
    ) { context ->
        val type = context.dibResult!!

        client.get("/inventory/types/${type.id.value}").assertStatusCode(HttpStatusCode.NotFound)
    }

    @Test
    fun test_get_itemType_byId_loggedIn_outsideDepartment_notVisible() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = {
            FakeUser.provideEntity()
            val otherDepartment = DepartmentEntity.new { displayName = "Other Department" }
            InventoryItemTypeEntity.new {
                displayName = "Private Item Type"
                department = otherDepartment
            }
        },
    ) { context ->
        val type = context.dibResult!!

        client.get("/inventory/types/${type.id.value}").assertStatusCode(HttpStatusCode.NotFound)
    }

    @Test
    fun test_get_itemType_byId_departmentMember_visible() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = {
            FakeUser.provideEntity()
            val department = DepartmentEntity.new { displayName = "Managed Department" }
            DepartmentMembers.insert {
                it[userSub] = FakeUser.SUB
                it[departmentId] = department.id
                it[confirmed] = true
                it[roles] = emptyList()
            }
            InventoryItemTypeEntity.new {
                displayName = "Department Item Type"
                this.department = department
            }
        },
    ) { context ->
        val type = context.dibResult!!

        client.get("/inventory/types/${type.id.value}").assertStatusCode(HttpStatusCode.OK)
    }

    @Test
    fun test_get_item_byId_notLoggedIn_notVisible() = runApplicationTest(
        databaseInitBlock = {
            val type = InventoryItemTypeEntity.new { displayName = "Private Item Type" }
            InventoryItemEntity.new { this.type = type }
        },
    ) { context ->
        val item = context.dibResult!!

        client.get("/inventory/items/${item.id.value}").assertStatusCode(HttpStatusCode.NotFound)
    }

    @Test
    fun test_get_item_byId_departmentMember_visible() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = {
            FakeUser.provideEntity()
            val department = DepartmentEntity.new { displayName = "Managed Department" }
            DepartmentMembers.insert {
                it[userSub] = FakeUser.SUB
                it[departmentId] = department.id
                it[confirmed] = true
                it[roles] = emptyList()
            }
            val type = InventoryItemTypeEntity.new {
                displayName = "Department Item Type"
                this.department = department
            }
            InventoryItemEntity.new { this.type = type }
        },
    ) { context ->
        val item = context.dibResult!!

        client.get("/inventory/items/${item.id.value}").assertStatusCode(HttpStatusCode.OK)
    }
}
