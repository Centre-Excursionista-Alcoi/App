package org.centrexcursionistalcoi.app.nav

import io.ktor.http.Url
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.centrexcursionistalcoi.app.applink.AppLinkRoutes
import org.centrexcursionistalcoi.app.database.InventoryItemTypesRepository
import org.centrexcursionistalcoi.app.utils.toUuidOrNull
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import kotlin.uuid.Uuid

/**
 * Links (like `https://centrexcursionistalcoi.app/admin/lendings/<id>`, from the emails the server sends) that
 * reach the app by a callback rather than by starting it with the link, which is how iOS delivers them, whether
 * the app is being launched or is already running.
 *
 * The platform hands a link to [receive]; the app opens it once it can (see [canOpenLinks]) and then [consume]s
 * it. A link that arrives before the app is ready stays in [pending], so launching the app from a link works.
 */
object DeepLinks : KoinComponent {
    /** The link waiting to be opened, if any. */
    val pending: StateFlow<Url?>
        field = MutableStateFlow<Url?>(null)

    /**
     * Called by the platform with a link it was asked to open. Anything that isn't a valid URL is ignored.
     */
    fun receive(url: String) {
        val parsed = try {
            Url(url)
        } catch (_: Exception) {
            return
        }
        pending.value = parsed
    }

    /** Marks [url] as handled. Does nothing if a newer link has arrived meanwhile, which stays pending. */
    fun consume(url: Url) {
        pending.compareAndSet(url, null)
    }

    /**
     * Resolves the leaf destination a deep link [url] points to, or `null` if it doesn't match any destination.
     *
     * Links are plain web links, e.g. `https://centrexcursionistalcoi.app/admin/lendings/<id>`: that address is
     * the only way into the app, on every platform, with no custom URL scheme.
     *
     * This only returns the destination the link ultimately points to -- use [Destination.backStackFor] to build
     * the full back stack (with the appropriate ancestor screens) that should be pushed for it.
     *
     * Links are matched on where they point, see [route], and case-insensitively: not every app that shows a link
     * keeps the case of its host.
     */
    suspend fun fromUrl(url: Url?): Destination? {
        if (url == null) return null
        val route = url.route()
        if (route.isRoute(AppLinkRoutes.ITEM_TYPE)) {
            val typeId = route.idFor(AppLinkRoutes.ITEM_TYPE) ?: return null
            val type = get<InventoryItemTypesRepository>().get(typeId) ?: return null
            return Destination.ItemTypeDetails(type)
        }
        if (route.isRoute(AppLinkRoutes.ADMIN_ITEMS)) {
            val typeId = route.idFor(AppLinkRoutes.ADMIN_ITEMS) ?: return null
            return Destination.Main(showingAdminItemTypeId = typeId)
        }
        if (route.isRoute(AppLinkRoutes.ADMIN_LENDINGS)) {
            val showingLendingId = route.idFor(AppLinkRoutes.ADMIN_LENDINGS)
            return if (showingLendingId != null) {
                Destination.Admin.LendingManagement(showingLendingId)
            } else {
                Destination.Main(showingAdminLendingsScreen = true)
            }
        }
        if (route.equals(AppLinkRoutes.RESET_PASSWORD, ignoreCase = true)) {
            // Reset password request redirection from email
            val success = url.parameters["success"]?.toBoolean() ?: false
            return if (success) {
                Destination.Login(changedPassword = true)
            } else {
                val requestId = url.parameters["request_id"] ?: return null
                Destination.External.ResetPassword(requestId)
            }
        }
        return null
    }
}

/**
 * Whether a link can be opened from here right away: not while the app is still loading or the user is logging
 * in or out. Then it has to wait until the user is in, see [org.centrexcursionistalcoi.app.nav.Destination.Main].
 */
fun Destination.canOpenLinks(): Boolean = this !is Destination.Loading && this !is Destination.Login && this !is Destination.Logout

/** Where a link points, as a path like `admin/lendings`: the segments of its URL. */
private fun Url.route(): String = segments.filter { it.isNotEmpty() }.joinToString("/")

/** Whether this route is [base] itself, or [base] followed by an id (`admin/lendings/<id>`). */
private fun String.isRoute(base: String): Boolean =
    equals(base, ignoreCase = true) ||
        (startsWith("$base/", ignoreCase = true) && drop(base.length + 1).toUuidOrNull() != null)

/** The id a link to [base] carries, from its path (`admin/lendings/<id>`). */
private fun String.idFor(base: String): Uuid? = drop(base.length).trimStart('/').toUuidOrNull()
