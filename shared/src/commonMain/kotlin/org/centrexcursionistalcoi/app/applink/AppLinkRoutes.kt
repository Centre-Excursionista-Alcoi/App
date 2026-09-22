package org.centrexcursionistalcoi.app.applink

/**
 * The path prefixes an app link can point to, shared between the client (which resolves a link to a screen, see
 * `Destination.fromUrl`) and the server (which builds these links, see `AppLinks`, and serves a "get the app"
 * landing page at the [APP_ONLY] ones, for whoever doesn't have the app to intercept the link -- see
 * `appLinkLandingRoutes`). Defined once here so those three places can't drift apart.
 */
object AppLinkRoutes {
    const val ITEM_TYPE = "itemType"
    const val ADMIN_ITEMS = "admin/items"
    const val ADMIN_LENDINGS = "admin/lendings"

    /** Handled by a real, server-rendered page even without the app; not one of [APP_ONLY]. */
    const val RESET_PASSWORD = "reset_password"

    /** Paths that only mean something inside the app: nothing else can render them, so they need [APP_ONLY]'s landing page. */
    val APP_ONLY = listOf(ITEM_TYPE, ADMIN_ITEMS, ADMIN_LENDINGS)
}
