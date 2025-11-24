package org.centrexcursionistalcoi.app.nav

import io.ktor.http.Url
import kotlin.uuid.Uuid
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItemType
import org.centrexcursionistalcoi.app.data.ReferencedLending
import org.centrexcursionistalcoi.app.database.InventoryItemTypesRepository
import org.centrexcursionistalcoi.app.typing.ShoppingList
import org.centrexcursionistalcoi.app.utils.toUuid
import org.centrexcursionistalcoi.app.utils.toUuidOrNull

@Serializable
sealed interface Destination {
    companion object {
        const val ITEM_TYPE = "itemType"

        const val ADMIN_ITEMS = "admin/items"
        const val ADMIN_LENDINGS_MANAGEMENT = "admin/lendings"

        suspend fun fromUrl(url: Url?): Destination? {
            if (url == null) return null
            if (url.host == ITEM_TYPE) {
                val typeId = url.fragment.toUuidOrNull() ?: return null
                val type = InventoryItemTypesRepository.get(typeId) ?: return null
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

    @Serializable @SerialName("lendingMemoryWrite") data class LendingMemoryEditor(val lendingId: Uuid) : Destination {
        constructor(lending: ReferencedLending): this(lending.id)
    }

    /**
     * Redirections from external links.
     */
    object External {
        @Serializable @SerialName("reset_password") data class ResetPassword(
            @SerialName("request_id") val requestId: String,
        ) : Destination
    }
}
