package org.centrexcursionistalcoi.app.nav

import androidx.navigation3.runtime.NavKey
import io.ktor.http.Url
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
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
        const val ITEM_TYPE = "itemType"

        const val ADMIN_ITEMS = "admin/items"
        const val ADMIN_LENDINGS_MANAGEMENT = "admin/lendings"

        /**
         * Resolves the leaf destination a deep link [url] points to, or `null` if it doesn't match any destination.
         *
         * This only returns the destination the link ultimately points to -- use [backStackFor] to build the full
         * back stack (with the appropriate ancestor screens) that should be pushed for it.
         */
        suspend fun fromUrl(url: Url?): Destination? {
            if (url == null) return null
            if (url.host == ITEM_TYPE) {
                val typeId = url.fragment.toUuidOrNull() ?: return null
                val type = get<InventoryItemTypesRepository>().get(typeId) ?: return null
                return ItemTypeDetails(type)
            }
            if (url.host == ADMIN_ITEMS) {
                val typeId = url.fragment.toUuidOrNull() ?: return null
                return Main(showingAdminItemTypeId = typeId)
            }
            if (url.host == ADMIN_LENDINGS_MANAGEMENT) {
                val showingLendingId = url.fragment.toUuidOrNull()
                return if (showingLendingId != null) {
                    Admin.LendingManagement(showingLendingId)
                } else {
                    Main(showingAdminLendingsScreen = true)
                }
            }
            if (url.segments[0] == "reset_password") {
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
