package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import org.centrexcursionistalcoi.app.database.InventoryItemTypesRepository
import kotlin.uuid.Uuid

class InventoryItemTypeDetailsScreenModel(
    typeId: Uuid,
    inventoryItemTypesRepository: InventoryItemTypesRepository,
): ViewModel() {
    val type = inventoryItemTypesRepository.getAsFlow(typeId).stateInViewModel()
}
