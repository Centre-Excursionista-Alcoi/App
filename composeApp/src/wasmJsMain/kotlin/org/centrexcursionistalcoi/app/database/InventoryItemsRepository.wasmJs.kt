package org.centrexcursionistalcoi.app.database

import kotlin.uuid.Uuid
import org.centrexcursionistalcoi.app.data.InventoryItem

actual val InventoryItemsRepository: Repository<InventoryItem, Uuid> = InventoryItemsSettingsRepository
