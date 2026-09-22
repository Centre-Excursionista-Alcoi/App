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
    fun aResetPasswordLink_isOnTheAppsAddress_neverTheServersOwnHost() {
        // issue #668: a user must never see server.centrexcursionistalcoi.app
        assertEquals("https://centrexcursionistalcoi.app/reset_password?request_id=abc123", AppLinks.resetPassword("abc123"))
    }

    @Test
    fun theAddress_canBeConfigured_withOrWithoutATrailingSlash() {
        AppLinks.override("APP_LINKS_BASE_URL", "https://links.example.org/")
        assertEquals("https://links.example.org/admin/lendings/$lendingId", AppLinks.adminLending(lendingId))

        AppLinks.override("APP_LINKS_BASE_URL", "https://links.example.org")
        assertEquals("https://links.example.org/admin/lendings/$lendingId", AppLinks.adminLending(lendingId))
    }

    @Test
    fun playStoreUrl_isDerivedFromThePackageName() {
        assertEquals("https://play.google.com/store/apps/details?id=org.centrexcursionistalcoi.app", AppLinks.playStoreUrl)
    }

    @Test
    fun appStoreUrl_isTheAppStoreLink() {
        assertEquals("https://apps.apple.com/us/app/cea-app/id6754717471", AppLinks.appStoreUrl)
    }
}
