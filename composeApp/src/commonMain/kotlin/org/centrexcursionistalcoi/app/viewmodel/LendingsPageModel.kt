package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItemType
import org.centrexcursionistalcoi.app.database.InventoryItemsRepository
import org.centrexcursionistalcoi.app.database.LendingsRepository
import org.centrexcursionistalcoi.app.database.ProfileRepository
import org.koin.core.annotation.KoinViewModel
import kotlin.uuid.Uuid

@KoinViewModel
class LendingsPageModel(
    inventoryItemsRepository: InventoryItemsRepository,
    lendingsRepository: LendingsRepository,
) : ViewModel() {
    val profile = ProfileRepository.profile.stateInViewModel()
    val inventoryItems = inventoryItemsRepository.selectAllAsFlow().stateInViewModel()
    val lendings = lendingsRepository.selectAllAsFlow().stateInViewModel()

    private val _shoppingList = MutableStateFlow(emptyMap<Uuid, Int>())
    val shoppingList = _shoppingList.asStateFlow()

    fun addItemToShoppingList(type: ReferencedInventoryItemType) {
        val currentList = _shoppingList.value.toMutableMap()
        currentList[type.id] = (currentList[type.id] ?: 0) + 1
        _shoppingList.value = currentList
    }

    fun removeItemFromShoppingList(type: ReferencedInventoryItemType) {
        val currentList = _shoppingList.value.toMutableMap()
        val newAmount = (currentList[type.id] ?: 0) - 1
        when {
            newAmount > 0 -> currentList[type.id] = newAmount
            else -> currentList.remove(type.id)
        }
        _shoppingList.value = currentList
    }
}
