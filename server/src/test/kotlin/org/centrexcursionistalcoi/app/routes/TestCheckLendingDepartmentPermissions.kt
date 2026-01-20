package org.centrexcursionistalcoi.app.routes

import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.centrexcursionistalcoi.app.ApplicationTestBase
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.DepartmentEntity
import org.centrexcursionistalcoi.app.database.entity.InventoryItemEntity
import org.centrexcursionistalcoi.app.database.entity.InventoryItemTypeEntity
import org.centrexcursionistalcoi.app.database.entity.LendingEntity
import org.centrexcursionistalcoi.app.database.table.LendingItems
import org.centrexcursionistalcoi.app.plugins.UserSession
import org.centrexcursionistalcoi.app.security.Permissions
import org.centrexcursionistalcoi.app.test.FakeUser
import org.centrexcursionistalcoi.app.test.LoginType
import org.centrexcursionistalcoi.app.utils.toUUID
import org.jetbrains.exposed.v1.jdbc.insert

/**
 * Tests for the checkLendingDepartmentPermissions helper function.
 */
class TestCheckLendingDepartmentPermissions : ApplicationTestBase() {

    private val departmentId1 = "11111111-1111-1111-1111-111111111111".toUUID()
    private val departmentId2 = "22222222-2222-2222-2222-222222222222".toUUID()
    private val itemTypeId1 = "33333333-3333-3333-3333-333333333333".toUUID()
    private val itemTypeId2 = "44444444-4444-4444-4444-444444444444".toUUID()
    private val itemTypeId3 = "55555555-5555-5555-5555-555555555555".toUUID() // No department
    private val itemId1 = "66666666-6666-6666-6666-666666666666".toUUID()
    private val itemId2 = "77777777-7777-7777-7777-777777777777".toUUID()
    private val itemId3 = "88888888-8888-8888-8888-888888888888".toUUID()

    @Test
    fun test_checkLendingDepartmentPermissions_globalGivePermission() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = {
            val department1 = DepartmentEntity.new(departmentId1) {
                displayName = "Department 1"
            }

            val itemType = InventoryItemTypeEntity.new(itemTypeId1) {
                displayName = "Item Type 1"
                department = department1
            }

            val item = InventoryItemEntity.new(itemId1) {
                type = itemType
                variation = "Variant A"
            }

            val lending = LendingEntity.new {
                userSub = FakeUser.provideEntity()
                from = java.time.LocalDate.of(2025, 10, 10)
                to = java.time.LocalDate.of(2025, 10, 15)
            }

            LendingItems.insert {
                it[LendingItems.item] = item.id.value
                it[LendingItems.lending] = lending.id
            }

            lending
        },
        userEntityPatches = { user ->
            // Give user global GIVE permission
            user.groups = listOf("user", Permissions.Lending.GIVE)
        }
    ) { context ->
        val lending = context.dibResult!!
        val session = UserSession(
            sub = FakeUser.SUB,
            fullName = FakeUser.FULL_NAME,
            email = FakeUser.EMAIL,
            groups = listOf("user", Permissions.Lending.GIVE)
        )

        val result = checkLendingDepartmentPermissions(session, lending, isGiving = true)
        assertTrue(result, "User with global GIVE permission should be able to give items from any department")
    }

    @Test
    fun test_checkLendingDepartmentPermissions_globalReceivePermission() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = {
            val department1 = DepartmentEntity.new(departmentId1) {
                displayName = "Department 1"
            }

            val itemType = InventoryItemTypeEntity.new(itemTypeId1) {
                displayName = "Item Type 1"
                department = department1
            }

            val item = InventoryItemEntity.new(itemId1) {
                type = itemType
                variation = "Variant A"
            }

            val lending = LendingEntity.new {
                userSub = FakeUser.provideEntity()
                from = java.time.LocalDate.of(2025, 10, 10)
                to = java.time.LocalDate.of(2025, 10, 15)
            }

            LendingItems.insert {
                it[LendingItems.item] = item.id.value
                it[LendingItems.lending] = lending.id
            }

            lending
        },
        userEntityPatches = { user ->
            // Give user global RECEIVE permission
            user.groups = listOf("user", Permissions.Lending.RECEIVE)
        }
    ) { context ->
        val lending = context.dibResult!!
        val session = UserSession(
            sub = FakeUser.SUB,
            fullName = FakeUser.FULL_NAME,
            email = FakeUser.EMAIL,
            groups = listOf("user", Permissions.Lending.RECEIVE)
        )

        val result = checkLendingDepartmentPermissions(session, lending, isGiving = false)
        assertTrue(result, "User with global RECEIVE permission should be able to receive items from any department")
    }

    @Test
    fun test_checkLendingDepartmentPermissions_departmentSpecificGivePermission() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = {
            val department1 = DepartmentEntity.new(departmentId1) {
                displayName = "Department 1"
            }

            val itemType = InventoryItemTypeEntity.new(itemTypeId1) {
                displayName = "Item Type 1"
                department = department1
            }

            val item = InventoryItemEntity.new(itemId1) {
                type = itemType
                variation = "Variant A"
            }

            val lending = LendingEntity.new {
                userSub = FakeUser.provideEntity()
                from = java.time.LocalDate.of(2025, 10, 10)
                to = java.time.LocalDate.of(2025, 10, 15)
            }

            LendingItems.insert {
                it[LendingItems.item] = item.id.value
                it[LendingItems.lending] = lending.id
            }

            lending
        },
        userEntityPatches = { user ->
            // Give user permission for department 1 only
            user.groups = listOf("user", Permissions.Lending.GIVE_BY_DEPARTMENT(departmentId1))
        }
    ) { context ->
        val lending = context.dibResult!!
        val session = UserSession(
            sub = FakeUser.SUB,
            fullName = FakeUser.FULL_NAME,
            email = FakeUser.EMAIL,
            groups = listOf("user", Permissions.Lending.GIVE_BY_DEPARTMENT(departmentId1))
        )

        val result = checkLendingDepartmentPermissions(session, lending, isGiving = true)
        assertTrue(result, "User with department-specific GIVE permission should be able to give items from that department")
    }

    @Test
    fun test_checkLendingDepartmentPermissions_departmentSpecificReceivePermission() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = {
            val department1 = DepartmentEntity.new(departmentId1) {
                displayName = "Department 1"
            }

            val itemType = InventoryItemTypeEntity.new(itemTypeId1) {
                displayName = "Item Type 1"
                department = department1
            }

            val item = InventoryItemEntity.new(itemId1) {
                type = itemType
                variation = "Variant A"
            }

            val lending = LendingEntity.new {
                userSub = FakeUser.provideEntity()
                from = java.time.LocalDate.of(2025, 10, 10)
                to = java.time.LocalDate.of(2025, 10, 15)
            }

            LendingItems.insert {
                it[LendingItems.item] = item.id.value
                it[LendingItems.lending] = lending.id
            }

            lending
        },
        userEntityPatches = { user ->
            // Give user permission for department 1 only
            user.groups = listOf("user", Permissions.Lending.RECEIVE_BY_DEPARTMENT(departmentId1))
        }
    ) { context ->
        val lending = context.dibResult!!
        val session = UserSession(
            sub = FakeUser.SUB,
            fullName = FakeUser.FULL_NAME,
            email = FakeUser.EMAIL,
            groups = listOf("user", Permissions.Lending.RECEIVE_BY_DEPARTMENT(departmentId1))
        )

        val result = checkLendingDepartmentPermissions(session, lending, isGiving = false)
        assertTrue(result, "User with department-specific RECEIVE permission should be able to receive items from that department")
    }

    @Test
    fun test_checkLendingDepartmentPermissions_noDepartmentPermission() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = {
            val department1 = DepartmentEntity.new(departmentId1) {
                displayName = "Department 1"
            }

            val itemType = InventoryItemTypeEntity.new(itemTypeId1) {
                displayName = "Item Type 1"
                department = department1
            }

            val item = InventoryItemEntity.new(itemId1) {
                type = itemType
                variation = "Variant A"
            }

            val lending = LendingEntity.new {
                userSub = FakeUser.provideEntity()
                from = java.time.LocalDate.of(2025, 10, 10)
                to = java.time.LocalDate.of(2025, 10, 15)
            }

            LendingItems.insert {
                it[LendingItems.item] = item.id.value
                it[LendingItems.lending] = lending.id
            }

            lending
        },
        userEntityPatches = { user ->
            // User has no lending permissions
            user.groups = listOf("user")
        }
    ) { context ->
        val lending = context.dibResult!!
        val session = UserSession(
            sub = FakeUser.SUB,
            fullName = FakeUser.FULL_NAME,
            email = FakeUser.EMAIL,
            groups = listOf("user")
        )

        val result = checkLendingDepartmentPermissions(session, lending, isGiving = true)
        assertFalse(result, "User with no lending permissions should not be able to give items")

        val resultReceive = checkLendingDepartmentPermissions(session, lending, isGiving = false)
        assertFalse(resultReceive, "User with no lending permissions should not be able to receive items")
    }

    @Test
    fun test_checkLendingDepartmentPermissions_itemsWithoutDepartment() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = {
            // Item type without department
            val itemType = InventoryItemTypeEntity.new(itemTypeId3) {
                displayName = "Item Type 3"
                department = null
            }

            val item = InventoryItemEntity.new(itemId3) {
                type = itemType
                variation = "Variant C"
            }

            val lending = LendingEntity.new {
                userSub = FakeUser.provideEntity()
                from = java.time.LocalDate.of(2025, 10, 10)
                to = java.time.LocalDate.of(2025, 10, 15)
            }

            LendingItems.insert {
                it[LendingItems.item] = item.id.value
                it[LendingItems.lending] = lending.id
            }

            lending
        },
        userEntityPatches = { user ->
            // Give user permission for items without department
            user.groups = listOf("user", Permissions.Lending.GIVE_NO_DEPARTMENT)
        }
    ) { context ->
        val lending = context.dibResult!!
        val session = UserSession(
            sub = FakeUser.SUB,
            fullName = FakeUser.FULL_NAME,
            email = FakeUser.EMAIL,
            groups = listOf("user", Permissions.Lending.GIVE_NO_DEPARTMENT)
        )

        val result = checkLendingDepartmentPermissions(session, lending, isGiving = true)
        assertTrue(result, "User with GIVE_NO_DEPARTMENT permission should be able to give items without department")
    }

    @Test
    fun test_checkLendingDepartmentPermissions_mixedDepartmentsWithFullPermissions() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = {
            val department1 = DepartmentEntity.new(departmentId1) {
                displayName = "Department 1"
            }

            val department2 = DepartmentEntity.new(departmentId2) {
                displayName = "Department 2"
            }

            val itemType1 = InventoryItemTypeEntity.new(itemTypeId1) {
                displayName = "Item Type 1"
                department = department1
            }

            val itemType2 = InventoryItemTypeEntity.new(itemTypeId2) {
                displayName = "Item Type 2"
                department = department2
            }

            val item1 = InventoryItemEntity.new(itemId1) {
                type = itemType1
                variation = "Variant A"
            }

            val item2 = InventoryItemEntity.new(itemId2) {
                type = itemType2
                variation = "Variant B"
            }

            val lending = LendingEntity.new {
                userSub = FakeUser.provideEntity()
                from = java.time.LocalDate.of(2025, 10, 10)
                to = java.time.LocalDate.of(2025, 10, 15)
            }

            LendingItems.insert {
                it[LendingItems.item] = item1.id.value
                it[LendingItems.lending] = lending.id
            }
            LendingItems.insert {
                it[LendingItems.item] = item2.id.value
                it[LendingItems.lending] = lending.id
            }

            lending
        },
        userEntityPatches = { user ->
            // Give user permission for both departments
            user.groups = listOf(
                "user",
                Permissions.Lending.GIVE_BY_DEPARTMENT(departmentId1),
                Permissions.Lending.GIVE_BY_DEPARTMENT(departmentId2)
            )
        }
    ) { context ->
        val lending = context.dibResult!!
        val session = UserSession(
            sub = FakeUser.SUB,
            fullName = FakeUser.FULL_NAME,
            email = FakeUser.EMAIL,
            groups = listOf(
                "user",
                Permissions.Lending.GIVE_BY_DEPARTMENT(departmentId1),
                Permissions.Lending.GIVE_BY_DEPARTMENT(departmentId2)
            )
        )

        val result = checkLendingDepartmentPermissions(session, lending, isGiving = true)
        assertTrue(result, "User with permissions for all departments should be able to give items from multiple departments")
    }

    @Test
    fun test_checkLendingDepartmentPermissions_mixedDepartmentsWithPartialPermissions() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = {
            val department1 = DepartmentEntity.new(departmentId1) {
                displayName = "Department 1"
            }

            val department2 = DepartmentEntity.new(departmentId2) {
                displayName = "Department 2"
            }

            val itemType1 = InventoryItemTypeEntity.new(itemTypeId1) {
                displayName = "Item Type 1"
                department = department1
            }

            val itemType2 = InventoryItemTypeEntity.new(itemTypeId2) {
                displayName = "Item Type 2"
                department = department2
            }

            val item1 = InventoryItemEntity.new(itemId1) {
                type = itemType1
                variation = "Variant A"
            }

            val item2 = InventoryItemEntity.new(itemId2) {
                type = itemType2
                variation = "Variant B"
            }

            val lending = LendingEntity.new {
                userSub = FakeUser.provideEntity()
                from = java.time.LocalDate.of(2025, 10, 10)
                to = java.time.LocalDate.of(2025, 10, 15)
            }

            LendingItems.insert {
                it[LendingItems.item] = item1.id.value
                it[LendingItems.lending] = lending.id
            }
            LendingItems.insert {
                it[LendingItems.item] = item2.id.value
                it[LendingItems.lending] = lending.id
            }

            lending
        },
        userEntityPatches = { user ->
            // Give user permission for department 1 only, not department 2
            user.groups = listOf("user", Permissions.Lending.GIVE_BY_DEPARTMENT(departmentId1))
        }
    ) { context ->
        val lending = context.dibResult!!
        val session = UserSession(
            sub = FakeUser.SUB,
            fullName = FakeUser.FULL_NAME,
            email = FakeUser.EMAIL,
            groups = listOf("user", Permissions.Lending.GIVE_BY_DEPARTMENT(departmentId1))
        )

        val result = checkLendingDepartmentPermissions(session, lending, isGiving = true)
        assertFalse(result, "User with permission for only one department should not be able to give items from multiple departments")
    }

    @Test
    fun test_checkLendingDepartmentPermissions_mixedWithAndWithoutDepartment() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = {
            val department1 = DepartmentEntity.new(departmentId1) {
                displayName = "Department 1"
            }

            val itemType1 = InventoryItemTypeEntity.new(itemTypeId1) {
                displayName = "Item Type 1"
                department = department1
            }

            val itemType3 = InventoryItemTypeEntity.new(itemTypeId3) {
                displayName = "Item Type 3"
                department = null
            }

            val item1 = InventoryItemEntity.new(itemId1) {
                type = itemType1
                variation = "Variant A"
            }

            val item3 = InventoryItemEntity.new(itemId3) {
                type = itemType3
                variation = "Variant C"
            }

            val lending = LendingEntity.new {
                userSub = FakeUser.provideEntity()
                from = java.time.LocalDate.of(2025, 10, 10)
                to = java.time.LocalDate.of(2025, 10, 15)
            }

            LendingItems.insert {
                it[LendingItems.item] = item1.id.value
                it[LendingItems.lending] = lending.id
            }
            LendingItems.insert {
                it[LendingItems.item] = item3.id.value
                it[LendingItems.lending] = lending.id
            }

            lending
        },
        userEntityPatches = { user ->
            // Give user permission for department 1 and items without department
            user.groups = listOf(
                "user",
                Permissions.Lending.RECEIVE_BY_DEPARTMENT(departmentId1),
                Permissions.Lending.RECEIVE_NO_DEPARTMENT
            )
        }
    ) { context ->
        val lending = context.dibResult!!
        val session = UserSession(
            sub = FakeUser.SUB,
            fullName = FakeUser.FULL_NAME,
            email = FakeUser.EMAIL,
            groups = listOf(
                "user",
                Permissions.Lending.RECEIVE_BY_DEPARTMENT(departmentId1),
                Permissions.Lending.RECEIVE_NO_DEPARTMENT
            )
        )

        val result = checkLendingDepartmentPermissions(session, lending, isGiving = false)
        assertTrue(result, "User with permissions for department and no-department items should be able to receive mixed items")
    }

    @Test
    fun test_checkLendingDepartmentPermissions_wrongDepartmentPermission() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = {
            val department1 = DepartmentEntity.new(departmentId1) {
                displayName = "Department 1"
            }

            val itemType = InventoryItemTypeEntity.new(itemTypeId1) {
                displayName = "Item Type 1"
                department = department1
            }

            val item = InventoryItemEntity.new(itemId1) {
                type = itemType
                variation = "Variant A"
            }

            val lending = LendingEntity.new {
                userSub = FakeUser.provideEntity()
                from = java.time.LocalDate.of(2025, 10, 10)
                to = java.time.LocalDate.of(2025, 10, 15)
            }

            LendingItems.insert {
                it[LendingItems.item] = item.id.value
                it[LendingItems.lending] = lending.id
            }

            lending
        },
        userEntityPatches = { user ->
            // Give user permission for department 2, but item is in department 1
            user.groups = listOf("user", Permissions.Lending.GIVE_BY_DEPARTMENT(departmentId2))
        }
    ) { context ->
        val lending = context.dibResult!!
        val session = UserSession(
            sub = FakeUser.SUB,
            fullName = FakeUser.FULL_NAME,
            email = FakeUser.EMAIL,
            groups = listOf("user", Permissions.Lending.GIVE_BY_DEPARTMENT(departmentId2))
        )

        val result = checkLendingDepartmentPermissions(session, lending, isGiving = true)
        assertFalse(result, "User with permission for wrong department should not be able to give items")
    }
}
