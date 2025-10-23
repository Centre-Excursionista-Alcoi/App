package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import kotlin.uuid.Uuid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItem
import org.centrexcursionistalcoi.app.database.InventoryItemTypesRepository
import org.centrexcursionistalcoi.app.database.InventoryItemsRepository
import org.centrexcursionistalcoi.app.defaultAsyncDispatcher
import org.centrexcursionistalcoi.app.exception.CannotAllocateEnoughItemsException
import org.centrexcursionistalcoi.app.network.LendingsRemoteRepository
import org.centrexcursionistalcoi.app.typing.ShoppingList

class LendingCreationViewModel(
    private val originalShoppingList: ShoppingList
) : ViewModel() {
    val inventoryItemTypes = InventoryItemTypesRepository.selectAllAsFlow().stateInViewModel()

    val inventoryItems = InventoryItemsRepository.selectAllAsFlow().stateInViewModel()

    private val _from = MutableStateFlow<LocalDate?>(null)
    val from = _from.asStateFlow()

    private val _to = MutableStateFlow<LocalDate?>(null)
    val to = _to.asStateFlow()

    private val _shoppingList = MutableStateFlow(originalShoppingList)
    val shoppingList = _shoppingList.asStateFlow()

    private val _allocatedItems = MutableStateFlow<List<ReferencedInventoryItem>?>(null)
    val allocatedItems = _allocatedItems.asStateFlow()

    private val _error = MutableStateFlow<Throwable?>(null)
    val error = _error.asStateFlow()

    fun setFrom(date: LocalDate) {
        _from.value = date
        allocateItems()
    }

    fun setTo(date: LocalDate) {
        _to.value = date
        allocateItems()
    }

    fun resetShoppingList() {
        _shoppingList.value = originalShoppingList
        allocateItems()
    }

    fun addItemToShoppingList(typeId: Uuid) {
        val maxItemAmount = inventoryItems.value?.count { it.type.id == typeId } ?: 0
        val shoppingList = _shoppingList.value.toMutableMap()
        val currentAmount = shoppingList[typeId] ?: 0
        if (currentAmount < maxItemAmount) {
            shoppingList[typeId] = currentAmount + 1
            _shoppingList.value = shoppingList
            allocateItems()
        } else {
            Napier.w { "Cannot add more items of type $typeId to shopping list. Reached max available amount: $maxItemAmount" }
        }
    }

    fun removeItemFromShoppingList(typeId: Uuid) {
        val shoppingList = _shoppingList.value.toMutableMap()
        val currentAmount = shoppingList[typeId] ?: 0
        if (currentAmount > 0) {
            if (currentAmount == 1) {
                shoppingList.remove(typeId)
            } else {
                shoppingList[typeId] = currentAmount - 1
            }
            _shoppingList.value = shoppingList
            allocateItems()
        } else {
            Napier.w { "Cannot remove items of type $typeId from shopping list. Amount is already zero." }
        }
    }

    fun removeItemTypeFromShoppingList(typeId: Uuid) {
        val shoppingList = _shoppingList.value.toMutableMap()
        if (shoppingList.containsKey(typeId)) {
            shoppingList.remove(typeId)
            _shoppingList.value = shoppingList
            allocateItems()
        } else {
            Napier.w { "Cannot remove items of type $typeId from shopping list. Type not present." }
        }
    }

    private fun allocateItems() = viewModelScope.launch(defaultAsyncDispatcher) {
        val from = from.value ?: return@launch
        val to = to.value ?: return@launch

        _error.emit(null)
        _allocatedItems.emit(null)

        val allocatedItemsIds = mutableListOf<Uuid>()
        for ((typeId, amount) in shoppingList.value) {
            try {
                Napier.i { "Trying to allocate x$amount of $typeId from $from to $to..." }
                val items = LendingsRemoteRepository.allocate(typeId, from, to, amount)
                Napier.d { "Items allocated. IDs: $items" }
                allocatedItemsIds.addAll(items)
            } catch (e: CannotAllocateEnoughItemsException) {
                // Not enough items available
                Napier.e(e) { "Not enough items available for the given date range." }
                _error.emit(e)
                _allocatedItems.emit(emptyList())
                return@launch
            } catch (e: IllegalArgumentException) {
                // Some other error
                Napier.e(e) { "Failed to allocate $typeId" }
                _error.emit(e)
                _allocatedItems.emit(emptyList())
                return@launch
            }
        }
        val inventoryItems = inventoryItems.value.orEmpty()

        val allocatedItems = allocatedItemsIds.map { uuid ->
            inventoryItems.first { it.id == uuid }
        }
        Napier.i { "Allocated ${allocatedItems.size} items successfully." }
        _allocatedItems.emit(allocatedItems)
    }

    fun createLending(onSuccess: () -> Unit) {
        val from = from.value ?: return Napier.w { "From date not set" }
        val to = to.value ?: return Napier.w { "To date not set" }
        val items = allocatedItems.value ?: return Napier.w { "Items allocation not ready" }

        viewModelScope.launch(defaultAsyncDispatcher) {
            val itemIds = items.map { it.id }

            try {
                LendingsRemoteRepository.create(from, to, itemIds, null)
                Napier.i { "Lending created" }
                withContext(Dispatchers.Main) { onSuccess() }
            } catch (_: IllegalArgumentException) {
                Napier.e { "Failed to create lending. Conflict with another lending." }
            }
        }
    }
}
