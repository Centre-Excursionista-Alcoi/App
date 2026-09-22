package org.centrexcursionistalcoi.app

import org.centrexcursionistalcoi.app.applink.AppLinkRoutes
import java.util.UUID

/**
 * The links the server puts in its emails to open something in the app.
 *
 * They're plain web links, on the address the app claims links on (`APP_LINKS_BASE_URL`): with the app installed
 * they open it directly, and without it that address is meant to show a page to get it (see
 * `appLinkFallbackRoutes`). There is no custom URL scheme; the web address is the only way into the app.
 */
object AppLinks : ConfigProvider() {
    private const val DEFAULT_BASE_URL = "https://centrexcursionistalcoi.app"

    /** The Android package id, shared by every build flavor (see `applicationId` in `android/build.gradle.kts`). */
    const val ANDROID_PACKAGE_NAME = "org.centrexcursionistalcoi.app"

    /** The address app links are on, with no trailing slash. */
    val baseUrl: String get() = (getenv("APP_LINKS_BASE_URL") ?: DEFAULT_BASE_URL).trimEnd('/')

    /** Where an Android visitor without the app ends up (see `respondAppLinkFallback`). */
    val playStoreUrl: String get() = "https://play.google.com/store/apps/details?id=$ANDROID_PACKAGE_NAME"

    /** Where an iOS visitor without the app ends up. */
    const val appStoreUrl: String = "https://apps.apple.com/us/app/cea-app/id6754717471"

    /** Opens a lending in the admin panel of the app. */
    fun adminLending(lendingId: UUID): String = "$baseUrl/${AppLinkRoutes.ADMIN_LENDINGS}/$lendingId"

    /**
     * The link the "lost password" email sends. On this same host so it never exposes the server's own domain to a
     * user (issue #668) -- it's excluded from the app's own link-claiming (see `WEB_ONLY_PATHS` in
     * `WellKnownRoutes.kt`/`MainActivity`) since it's a real, server-rendered page with no native screen to route to.
     */
    fun resetPassword(requestId: String): String = "$baseUrl/${AppLinkRoutes.RESET_PASSWORD}?request_id=$requestId"
}
