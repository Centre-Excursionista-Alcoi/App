package org.centrexcursionistalcoi.app.nav

import kotlin.uuid.Uuid
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.typing.ShoppingList

sealed interface Destination {
    @Serializable @SerialName("loading") data object Loading : Destination
    @Serializable @SerialName("login") data object Login : Destination
    @Serializable @SerialName("home") data object Home : Destination

    @Serializable @SerialName("lendingsManagement") data object LendingsManagement : Destination

    @Serializable @SerialName("lendingCreation") data class LendingCreation(
        val itemsJson: String
    ) : Destination {
        constructor(items: ShoppingList): this(
            json.encodeToString(MapSerializer(Uuid.serializer(), Int.serializer()), items)
        )

        val items: ShoppingList
            get() = json.decodeFromString(
                MapSerializer(Uuid.serializer(), Int.serializer()),
                itemsJson
            )
    }
}
