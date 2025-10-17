package org.centrexcursionistalcoi.app.exception

import kotlin.uuid.Uuid

class CannotAllocateEnoughItemsException(
    val availableItems: List<Uuid>?,
    val triedToAllocateAmount: Int,
): IllegalStateException(
    "Not enough items available to allocate: Tried to allocate $triedToAllocateAmount items, but only ${availableItems?.size ?: 0} are available."
)
