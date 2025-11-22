package org.centrexcursionistalcoi.app.data

import org.centrexcursionistalcoi.app.database.InventoryItemTypesRepository

suspend fun InventoryItem.type(): ReferencedInventoryItemType? = InventoryItemTypesRepository.get(id)
