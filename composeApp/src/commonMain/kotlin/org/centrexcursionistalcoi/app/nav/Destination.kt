package org.centrexcursionistalcoi.app.nav

import androidx.navigation3.runtime.NavKey
import io.ktor.http.Url
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.applink.AppLinkRoutes
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItemType
import org.centrexcursionistalcoi.app.data.ReferencedLending
import org.centrexcursionistalcoi.app.database.InventoryItemTypesRepository
import org.centrexcursionistalcoi.app.nav.Destination.Companion.backStackFor
import org.centrexcursionistalcoi.app.nav.Destination.Companion.fromUrl
import org.centrexcursionistalcoi.app.typing.ShoppingList
import org.centrexcursionistalcoi.app.utils.toUuid
import org.centrexcursionistalcoi.app.utils.toUuidOrNull
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import kotlin.uuid.Uuid

@Serializable
sealed interface Destination : NavKey {
    companion object : KoinComponent {
        const val ITEM_TYPE = AppLinkRoutes.ITEM_TYPE

        const val ADMIN_ITEMS = AppLinkRoutes.ADMIN_ITEMS
        const val ADMIN_LENDINGS_MANAGEMENT = AppLinkRoutes.ADMIN_LENDINGS

        const val RESET_PASSWORD = AppLinkRoutes.RESET_PASSWORD

        /**
         * Resolves the leaf destination a deep link [url] points to, or `null` if it doesn't match any destination.
         *
         * Links are plain web links, e.g. `https://centrexcursionistalcoi.app/admin/lendings/<id>`: that address is
         * the only way into the app, on every platform, with no custom URL scheme.
         *
         * This only returns the destination the link ultimately points to -- use [backStackFor] to build the full
         * back stack (with the appropriate ancestor screens) that should be pushed for it.
         *
         * Links are matched on where they point, see [route], and case-insensitively: not every app that shows a link
         * keeps the case of its host.
         */
        suspend fun fromUrl(url: Url?): Destination? {
            if (url == null) return null
            val route = url.route()
            if (route.isRoute(ITEM_TYPE)) {
                val typeId = route.idFor(ITEM_TYPE) ?: return null
                val type = get<InventoryItemTypesRepository>().get(typeId) ?: return null
                return ItemTypeDetails(type)
            }
            if (route.isRoute(ADMIN_ITEMS)) {
                val typeId = route.idFor(ADMIN_ITEMS) ?: return null
                return Main(showingAdminItemTypeId = typeId)
            }
            if (route.isRoute(ADMIN_LENDINGS_MANAGEMENT)) {
                val showingLendingId = route.idFor(ADMIN_LENDINGS_MANAGEMENT)
                return if (showingLendingId != null) {
                    Admin.LendingManagement(showingLendingId)
                } else {
                    Main(showingAdminLendingsScreen = true)
                }
            }
            if (route.equals(RESET_PASSWORD, ignoreCase = true)) {
                // Reset password request redirection from email
                val success = url.parameters["success"]?.toBoolean() ?: false
                return if (success) {
                    Login(changedPassword = true)
                } else {
                    val requestId = url.parameters["request_id"] ?: return null
                    External.ResetPassword(requestId)
                }
            }
            return null
        }

        /**
         * Builds the synthetic back stack that should be pushed for [destination] (as resolved by [fromUrl]), so
         * that navigating back from a deep link behaves as if the user had navigated there normally.
         */
        fun backStackFor(destination: Destination): List<Destination> = when (destination) {
            is ItemTypeDetails -> listOf(Main(), destination)
            is Admin.LendingManagement -> listOf(Main(showingAdminLendingsScreen = true), destination)
            else -> listOf(destination)
        }
    }

    @Serializable @SerialName("loading") data object Loading : Destination
    @Serializable @SerialName("logout") data object Logout : Destination
    @Serializable @SerialName("login") data class Login(
        val changedPassword: Boolean = false,
    ) : Destination
    @Serializable @SerialName("main") data class Main(
        val showingAdminItemTypeId: Uuid? = null,
        val showingAdminLendingsScreen: Boolean = false,
    ) : Destination
    @Serializable @SerialName("settings") data object Settings : Destination

    @Serializable @SerialName("lendingDetails") data class LendingDetails(val lendingId: Uuid) : Destination {
        constructor(lending: ReferencedLending): this(lending.id)
    }
    @Serializable @SerialName("itemTypeDetails") data class ItemTypeDetails(val typeId: Uuid, val displayName: String) : Destination {
        constructor(type: ReferencedInventoryItemType): this(type.id, type.displayName)
    }

    /**
     * Admin-related destinations.
     */
    object Admin {
        @Serializable @SerialName("lendingManagement") data class LendingManagement(val lendingId: Uuid) : Destination {
            constructor(lending: ReferencedLending): this(lending.id)
        }
    }

    @Serializable @SerialName("lendingSignUp") data object LendingSignUp : Destination
    @Serializable @SerialName("lendingCreation") data class LendingCreation(
        private val shoppingListValue: String
    ) : Destination {
        constructor(shoppingList: ShoppingList): this(
            shoppingList.map { (id, amount) -> "$id=$amount" }.joinToString("&")
        )

        val shoppingList: ShoppingList get() = shoppingListValue
            .split('&')
            .associate { it.substringBefore('=').toUuid() to it.substringAfter('=').toInt() }
    }

    @Serializable @SerialName("memoryEditor") data class MemoryEditor(
        val memoryId: Uuid? = null
    ) : Destination

    @Serializable @SerialName("lendingMemoryEditor") data class LendingMemoryEditor(
        val lendingId: Uuid? = null
    ) : Destination

    /**
     * Redirections from external links.
     */
    object External {
        @Serializable @SerialName("reset_password") data class ResetPassword(
            @SerialName("request_id") val requestId: String,
        ) : Destination
    }
}

/** Where a link points, as a path like `admin/lendings`: the segments of its URL. */
internal fun Url.route(): String = segments.filter { it.isNotEmpty() }.joinToString("/")

/** Whether this route is [base] itself, or [base] followed by an id (`admin/lendings/<id>`). */
private fun String.isRoute(base: String): Boolean =
    equals(base, ignoreCase = true) ||
        (startsWith("$base/", ignoreCase = true) && drop(base.length + 1).toUuidOrNull() != null)

/** The id a link to [base] carries, from its path (`admin/lendings/<id>`). */
private fun String.idFor(base: String): Uuid? = drop(base.length).trimStart('/').toUuidOrNull()
