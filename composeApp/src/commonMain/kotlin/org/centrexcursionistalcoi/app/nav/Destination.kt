package org.centrexcursionistalcoi.app.nav

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItemType
import org.centrexcursionistalcoi.app.data.ReferencedLending
import org.centrexcursionistalcoi.app.typing.ShoppingList
import org.centrexcursionistalcoi.app.utils.toUuid
import kotlin.uuid.Uuid

@Serializable
sealed interface Destination : NavKey {
    companion object {
        /**
         * Builds the synthetic back stack that should be pushed for [destination] (as resolved by
         * [DeepLinks.fromUrl]), so that navigating back from a deep link behaves as if the user had navigated
         * there normally.
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
