package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import kotlin.uuid.Uuid
import org.centrexcursionistalcoi.app.database.InventoryItemTypesRepository

class InventoryItemTypeDetailsScreenModel(typeId: Uuid): ViewModel() {
    val type = InventoryItemTypesRepository.getAsFlow(typeId).stateInViewModel()
}
