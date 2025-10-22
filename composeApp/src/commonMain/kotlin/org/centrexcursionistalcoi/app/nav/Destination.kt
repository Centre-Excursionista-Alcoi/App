package org.centrexcursionistalcoi.app.nav

import kotlin.uuid.Uuid
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.typing.ShoppingList
import org.centrexcursionistalcoi.app.utils.toUuid

sealed interface Destination {
    @Serializable @SerialName("loading") data object Loading : Destination
    @Serializable @SerialName("login") data object Login : Destination
    @Serializable @SerialName("home") data object Home : Destination

    @Serializable @SerialName("lendingsManagement") data object LendingsManagement : Destination

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
}
