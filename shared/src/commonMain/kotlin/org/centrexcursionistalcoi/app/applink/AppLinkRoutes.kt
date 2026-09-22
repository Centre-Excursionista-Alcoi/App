package org.centrexcursionistalcoi.app.applink

/**
 * The path prefixes an app link can point to, shared between the client (which resolves a link to a screen, see
 * `Destination.fromUrl`) and the server (which builds these links, see `AppLinks`). Defined once here so the two
 * can't drift apart the way they already had once.
 */
object AppLinkRoutes {
    const val ITEM_TYPE = "itemType"
    const val ADMIN_ITEMS = "admin/items"
    const val ADMIN_LENDINGS = "admin/lendings"

    /** Handled by a real, server-rendered page even without the app. */
    const val RESET_PASSWORD = "reset_password"
}
