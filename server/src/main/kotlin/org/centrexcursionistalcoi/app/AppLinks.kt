package org.centrexcursionistalcoi.app

import java.util.UUID

/**
 * The links the server puts in its emails to open something in the app.
 *
 * They're plain web links, on the address the app claims links on (`APP_LINKS_BASE_URL`): with the app installed
 * they open it directly, and without it that address is meant to show a page to get it. There is no custom URL
 * scheme; the web address is the only way into the app.
 */
object AppLinks : ConfigProvider() {
    private const val DEFAULT_BASE_URL = "https://centrexcursionistalcoi.app"

    /** The address app links are on, with no trailing slash. */
    val baseUrl: String get() = (getenv("APP_LINKS_BASE_URL") ?: DEFAULT_BASE_URL).trimEnd('/')

    /** Opens a lending in the admin panel of the app. */
    fun adminLending(lendingId: UUID): String = "$baseUrl/admin/lendings/$lendingId"
}
