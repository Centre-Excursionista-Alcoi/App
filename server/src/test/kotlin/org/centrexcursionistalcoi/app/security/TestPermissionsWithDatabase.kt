package org.centrexcursionistalcoi.app.security

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.exception.UserNotFoundException
import org.centrexcursionistalcoi.app.test.*

class TestPermissionsWithDatabase {

    @Test
    fun `hasPermission throws for invalid permission format`() = runTest {
        Database.clear()
        Database.initForTests()

        Database { FakeUser.provideEntity() }

        assertFailsWith<IllegalArgumentException> {
            Database { Permissions.hasPermission(FakeUser.SUB, "Invalid.Format") }
        }
    }

    @Test
    fun `hasPermission throws when user not found`() {
        Database.clear()
        Database.initForTests()

        assertFailsWith<UserNotFoundException> {
            Database { Permissions.hasPermission("non-existent-sub", "user.view") }
        }
    }

    @Test
    fun `disabled user has no permissions`() = runTest {
        Database.clear()
        Database.initForTests()

        // create a disabled user entity
        Database {
            val u = FakeUser.provideEntity()
            // mark disabled
            u.isDisabled = true
        }

        Database { assertFalse(Permissions.hasPermission(FakeUser.SUB, "user")) }
    }

    @Test
    fun `admin user has all permissions`() = runTest {
        Database.clear()
        Database.initForTests()

        Database { FakeAdminUser.provideEntity() }

        Database {
            assertTrue(Permissions.hasPermission(FakeAdminUser.SUB, "anything.foo"))
            assertTrue(Permissions.hasPermission(FakeAdminUser.SUB, "user.view"))
        }
    }

    @Test
    fun `direct group match grants permission`() = runTest {
        Database.clear()
        Database.initForTests()

        Database {
            val u = FakeUser.provideEntity()
            // set an explicit group that equals the permission
            u.groups = listOf("user.view")
        }

        Database {
            assertTrue(Permissions.hasPermission(FakeUser.SUB, "user.view"))
            assertFalse(Permissions.hasPermission(FakeUser.SUB, "user.other"))
        }
    }

    @Test
    fun `wildcard group matches permission`() = runTest {
        Database.clear()
        Database.initForTests()

        Database {
            val u = FakeUser.provideEntity()
            u.groups = listOf("user.*")
        }

        Database {
            assertTrue(Permissions.hasPermission(FakeUser.SUB, "user.view"))
            assertTrue(Permissions.hasPermission(FakeUser.SUB, "user.edit"))
            assertFalse(Permissions.hasPermission(FakeUser.SUB, "other.view"))
        }
    }
}
