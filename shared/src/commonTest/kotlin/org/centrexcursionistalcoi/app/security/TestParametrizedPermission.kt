package org.centrexcursionistalcoi.app.security

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.centrexcursionistalcoi.app.security.ParametrizedPermission.Companion.parametrized

class TestParametrizedPermission {

    @Test
    fun `test parametrized permission with single parameter`() {
        val permission = ParametrizedPermission("department.*.kick")

        // Test with one parameter
        val result = permission("marketing")
        assertEquals("department.marketing.kick", result)
    }

    @Test
    fun `test parametrized permission with multiple parameters`() {
        val permission = ParametrizedPermission("resource.*.*.access")

        // Test with multiple parameters
        val result = permission("documents", "confidential")
        assertEquals("resource.documents.confidential.access", result)
    }

    @Test
    fun `test parametrized permission with no parameters`() {
        val permission = ParametrizedPermission("user.view")

        // Test with no parameters (should work with empty vararg)
        val result = permission()
        assertEquals("user.view", result)
    }

    @Test
    fun `test parametrized permission with wrong number of parameters`() {
        val permission = ParametrizedPermission("department.*.kick")

        // Test with wrong number of parameters - should throw
        assertFailsWith<IllegalArgumentException> {
            permission("marketing", "extra")
        }
    }

    @Test
    fun `test parametrized permission with too few parameters`() {
        val permission = ParametrizedPermission("resource.*.*.access")

        // Test with too few parameters - should throw
        assertFailsWith<IllegalArgumentException> {
            permission("documents")
        }
    }

    @Test
    fun `test parametrized permission extension function`() {
        // Test the extension function
        val permission = "department.*.kick".parametrized()

        val result = permission("finance")
        assertEquals("department.finance.kick", result)
    }

    @Test
    fun `test parametrized permission with different parameter types`() {
        val permission = ParametrizedPermission("item.*.view")

        // Test with different parameter types (should convert to string)
        val result1 = permission(123)
        val result2 = permission(true)
        val result3 = permission(42.5)

        assertEquals("item.123.view", result1)
        assertEquals("item.true.view", result2)
        assertEquals("item.42.5.view", result3)
    }

    @Test
    fun `test parametrized permission with special characters in parameters`() {
        val permission = ParametrizedPermission("file.*.access")

        // Test with special characters in parameters
        val result = permission("documents/confidential/file.txt")
        assertEquals("file.documents/confidential/file.txt.access", result)
    }

    @Test
    fun `test parametrized permission rejects blank parameters`() {
        val permission = ParametrizedPermission("group.*.access")

        // Test that blank parameters are rejected
        assertFailsWith<IllegalArgumentException> {
            permission("")
        }

        assertFailsWith<IllegalArgumentException> {
            permission("   ")
        }
    }

    @Test
    fun `test parametrized permission counts wildcards correctly`() {
        val permission1 = ParametrizedPermission("a.*.b")
        val permission2 = ParametrizedPermission("a.*.b.*.c")
        val permission3 = ParametrizedPermission("a.b.c")

        // These should work
        permission1("x")
        permission2("x", "y")
        permission3()

        // These should fail
        assertFailsWith<IllegalArgumentException> { permission1() }
        assertFailsWith<IllegalArgumentException> { permission1("x", "y") }
        assertFailsWith<IllegalArgumentException> { permission2("x") }
        assertFailsWith<IllegalArgumentException> { permission2("x", "y", "z") }
        assertFailsWith<IllegalArgumentException> { permission3("x") }
    }

    @Test
    fun `test parametrized permission from Permissions object`() {
        // Test that the permissions defined in the Permissions object work correctly
        val kickPermission = Permissions.Department.KICK
        val manageRequestsPermission = Permissions.Department.MANAGE_REQUESTS

        val kickResult = kickPermission("it")
        val manageResult = manageRequestsPermission("hr")

        assertEquals("department.it.kick", kickResult)
        assertEquals("department.hr.manage_requests", manageResult)
    }
}
