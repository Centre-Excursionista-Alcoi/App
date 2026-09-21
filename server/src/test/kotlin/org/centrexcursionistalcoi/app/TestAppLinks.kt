package org.centrexcursionistalcoi.app

import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class TestAppLinks {
    private val lendingId = UUID.fromString("1f0e5c2a-0000-4000-8000-000000000001")

    @AfterTest
    fun tearDown() {
        AppLinks.override("APP_LINKS_BASE_URL", null)
    }

    @Test
    fun aLendingLink_isAWebLink_onTheAppsAddress_byDefault() {
        assertEquals("https://centrexcursionistalcoi.app/admin/lendings/$lendingId", AppLinks.adminLending(lendingId))
    }

    @Test
    fun theAddress_canBeConfigured_withOrWithoutATrailingSlash() {
        AppLinks.override("APP_LINKS_BASE_URL", "https://links.example.org/")
        assertEquals("https://links.example.org/admin/lendings/$lendingId", AppLinks.adminLending(lendingId))

        AppLinks.override("APP_LINKS_BASE_URL", "https://links.example.org")
        assertEquals("https://links.example.org/admin/lendings/$lendingId", AppLinks.adminLending(lendingId))
    }
}
