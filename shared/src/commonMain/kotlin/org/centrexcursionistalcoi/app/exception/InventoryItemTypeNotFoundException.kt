package org.centrexcursionistalcoi.app.exception

import kotlin.uuid.Uuid

class InventoryItemTypeNotFoundException(id: Uuid): NoSuchElementException("Inventory item type with id $id not found")
