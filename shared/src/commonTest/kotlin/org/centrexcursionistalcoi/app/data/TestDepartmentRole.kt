package org.centrexcursionistalcoi.app.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TestDepartmentRole {
    @Test
    fun admin_impliesEveryRole() {
        for (role in DepartmentRole.entries) {
            assertTrue(DepartmentRole.ADMIN.implies(role), "ADMIN should imply $role")
        }
    }

    @Test
    fun everyRole_impliesItself() {
        for (role in DepartmentRole.entries) {
            assertTrue(role.implies(role), "$role should imply itself")
        }
    }

    @Test
    fun qualificationsManager_impliesExaminer_butNotTheOtherWayAround() {
        assertTrue(DepartmentRole.QUALIFICATIONS_MANAGER.implies(DepartmentRole.EXAMINER))
        assertFalse(DepartmentRole.EXAMINER.implies(DepartmentRole.QUALIFICATIONS_MANAGER))
    }

    @Test
    fun otherRoles_doNotImplyAnythingElse() {
        val notImplyingOthers = DepartmentRole.entries - DepartmentRole.ADMIN - DepartmentRole.QUALIFICATIONS_MANAGER
        for (role in notImplyingOthers) {
            val implied = DepartmentRole.entries.filter { role.implies(it) }
            assertEquals(listOf(role), implied, "$role should only imply itself")
        }
        assertEquals(
            setOf(DepartmentRole.QUALIFICATIONS_MANAGER, DepartmentRole.EXAMINER),
            DepartmentRole.entries.filter { DepartmentRole.QUALIFICATIONS_MANAGER.implies(it) }.toSet(),
        )
    }

    @Test
    fun storageName_roundTrips() {
        for (role in DepartmentRole.entries) {
            assertEquals(role, DepartmentRole.fromStorageName(role.storageName))
        }
    }
}
