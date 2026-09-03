package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import org.centrexcursionistalcoi.app.database.InventoryItemTypesRepository
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel
import kotlin.uuid.Uuid

@KoinViewModel
class InventoryItemTypeDetailsScreenModel(
    @InjectedParam typeId: Uuid,
    inventoryItemTypesRepository: InventoryItemTypesRepository,
): ViewModel() {
    val type = inventoryItemTypesRepository.getAsFlow(typeId).stateInViewModel()
}
