package org.centrexcursionistalcoi.app.security

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TestPermissions {
    @Test
    fun `validatePermissionFormat accepts valid and rejects invalid`() {
        // valid
        assertTrue(Permissions.validatePermissionFormat("user.view"))
        assertTrue(Permissions.validatePermissionFormat("user.*"))
        assertTrue(Permissions.validatePermissionFormat("user.*.edit"))
        assertTrue(Permissions.validatePermissionFormat("user_view"))
        assertTrue(Permissions.validatePermissionFormat("abc123.something"))

        // invalid
        assertFalse(Permissions.validatePermissionFormat("User.View"))
        assertFalse(Permissions.validatePermissionFormat("user..view"))
        assertFalse(Permissions.validatePermissionFormat("user,\$pecial"))
        assertFalse(Permissions.validatePermissionFormat(""))
    }
}
