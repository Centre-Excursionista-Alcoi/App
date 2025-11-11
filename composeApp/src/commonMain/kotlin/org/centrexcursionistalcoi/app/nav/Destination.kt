package org.centrexcursionistalcoi.app.nav

import kotlin.uuid.Uuid
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.data.InventoryItemType
import org.centrexcursionistalcoi.app.data.ReferencedLending
import org.centrexcursionistalcoi.app.typing.ShoppingList
import org.centrexcursionistalcoi.app.utils.toUuid

@Serializable
sealed interface Destination {
    @Serializable @SerialName("loading") data object Loading : Destination
    @Serializable @SerialName("logout") data object Logout : Destination
    @Serializable @SerialName("login") data object Login : Destination
    @Serializable @SerialName("home") data class Home(
        val showingLendingId: Uuid? = null
    ) : Destination
    @Serializable @SerialName("settings") data object Settings : Destination

    @Serializable @SerialName("itemTypeDetails") data class ItemTypeDetails(val typeId: Uuid, val displayName: String) : Destination {
        constructor(type: InventoryItemType): this(type.id, type.displayName)
    }

    /**
     * Admin-related destinations.
     */
    object Admin {
        /**
         * Shows all the items of a given inventory type.
         */
        @Serializable @SerialName("inventoryItem") data class InventoryItems(val typeId: Uuid, val displayName: String) : Destination {
            constructor(type: InventoryItemType): this(type.id, type.displayName)
        }

        @Serializable @SerialName("lendingsManagement") data class LendingsManagement(
            val showingLendingId: Uuid? = null
        ) : Destination
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

    @Serializable @SerialName("lendingPickup") data class LendingPickup(val lendingId: Uuid) : Destination

    @Serializable @SerialName("lendingReturn") data class LendingReturn(val lendingId: Uuid) : Destination

    @Serializable @SerialName("lendingMemoryWrite") data class LendingMemoryEditor(val lendingId: Uuid) : Destination {
        constructor(lending: ReferencedLending): this(lending.id)
    }
}
