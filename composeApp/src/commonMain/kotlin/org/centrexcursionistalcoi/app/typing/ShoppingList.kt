package org.centrexcursionistalcoi.app.typing

import kotlin.uuid.Uuid
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import org.centrexcursionistalcoi.app.nav.SerializableNavType

/**
 * A map of InventoryItemType ID to amount in the shopping list.
 */
typealias ShoppingList = Map<Uuid, Int>

class ShoppingListNavType() : SerializableNavType<ShoppingList>(MapSerializer(Uuid.serializer(), Int.serializer()))
