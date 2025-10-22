package org.centrexcursionistalcoi.app.database

import kotlin.uuid.Uuid
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItem

actual val InventoryItemsRepository: Repository<ReferencedInventoryItem, Uuid> = InventoryItemsSettingsRepository
