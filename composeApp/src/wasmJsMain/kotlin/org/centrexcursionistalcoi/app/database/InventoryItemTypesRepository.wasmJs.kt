package org.centrexcursionistalcoi.app.database

import kotlin.uuid.Uuid
import org.centrexcursionistalcoi.app.data.InventoryItemType

actual val InventoryItemTypesRepository: Repository<InventoryItemType, Uuid> = InventoryItemTypesDatabaseRepository
