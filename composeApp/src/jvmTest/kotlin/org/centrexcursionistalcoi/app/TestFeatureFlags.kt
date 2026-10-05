package org.centrexcursionistalcoi.app

import org.centrexcursionistalcoi.app.response.ProfileResponse
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TestFeatureFlags {
    private fun profile(vararg groups: String) = ProfileResponse(
        sub = "sub",
        fullName = "Someone",
        memberNumber = 1u,
        email = "someone@example.com",
        groups = groups.toList(),
        departments = emptyList(),
        lendingUser = null,
        insurances = emptyList(),
        femecvSyncEnabled = false,
        femecvLastSync = null,
    )

    @Test
    fun spaces_areOnlyForAdmins_whileTheyAreAWorkInProgress() {
        assertTrue(profile(ADMIN_GROUP_NAME).hasFeature(FeatureFlag.SPACES))
        assertFalse(profile().hasFeature(FeatureFlag.SPACES))
        // Managing them is not enough either
        assertFalse(profile(SPACES_MANAGER_GROUP_NAME, SPACE_LENDINGS_MANAGER_GROUP_NAME).hasFeature(FeatureFlag.SPACES))
    }
}
